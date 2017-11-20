/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * Copyright (C) 2007, Martin Oberhuber (martin.oberhuber@windriver.com)
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2015, Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;

/**
 * This class contains utility methods related to projects
 * TODO: rename to RefreshUtil or ResourceUtil?
 */
public class ProjectUtil {

	/**
	 * The name of the folder containing metadata information for the workspace.
	 */
	public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

	/**
	 * The method returns all valid open projects contained in the given Git
	 * repository. A project is considered as valid if the .project file exists.
	 * @see ProjectUtil#refreshValidProjects(IProject[], IProgressMonitor)
	 * @param repository
	 * @return valid open projects
	 * @throws CoreException
	 */
	public static IProject[] getValidOpenProjects(Repository repository)
			throws CoreException {
		if (repository == null || repository.isBare()) {
			return new IProject[0];
		}
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		List<IProject> result = new ArrayList<>();
		final Path repositoryPath = new Path(
				repository.getWorkTree().getAbsolutePath());
		for (IProject p : projects) {
			IPath projectLocation = p.getLocation();
			if (!p.isOpen() || projectLocation == null
					|| !repositoryPath.isPrefixOf(projectLocation))
				continue;
			IPath projectFilePath = projectLocation
					.append(IProjectDescription.DESCRIPTION_FILE_NAME);
			if (projectFilePath.toFile().exists()) {
				result.add(p);
			}
		}
		return result.toArray(new IProject[result.size()]);
	}

	/**
	 * The method refreshes the given projects. Projects with missing .project
	 * file are deleted. The method should be called in the following flow:<br>
	 * <ol>
	 * <li>Call {@link ProjectUtil#getValidOpenProjects(Repository)}
	 * <li>Perform a workdir checkout (e.g. branch, reset)
	 * <li>Call
	 * {@link ProjectUtil#refreshValidProjects(IProject[], IProgressMonitor)}
	 * </ol>
	 *
	 * @param projects
	 *            list of valid projects before workdir checkout.
	 * @param monitor
	 * @throws CoreException
	 */
	public static void refreshValidProjects(IProject[] projects,
			IProgressMonitor monitor) throws CoreException {
		refreshValidProjects(projects, true, monitor);
	}

	/**
	 * The method refreshes the given projects. Projects with missing .project
	 * file are deleted. The method should be called in the following flow:<br>
	 * <ol>
	 * <li>Call {@link ProjectUtil#getValidOpenProjects(Repository)}
	 * <li>Perform a workdir checkout (e.g. branch, reset)
	 * <li>Call
	 * {@link ProjectUtil#refreshValidProjects(IProject[], IProgressMonitor)}
	 * </ol>
	 *
	 * @param projects
	 *            list of valid projects before workdir checkout.
	 * @param delete
	 *            true to delete projects, false to close them
	 * @param monitor
	 *
	 * @throws CoreException
	 */
	public static void refreshValidProjects(IProject[] projects, boolean delete,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor,
				CoreText.ProjectUtil_refreshingProjects, projects.length);
		for (IProject p : projects) {
			if (progress.isCanceled())
				break;
			IPath projectLocation = p.getLocation();
			if (projectLocation == null) {
				progress.worked(1);
				continue;
			}
			String projectFilePath = projectLocation
					.append(IProjectDescription.DESCRIPTION_FILE_NAME)
					.toOSString();
			File projectFile = new File(projectFilePath);
			if (projectFile.exists())
				p.refreshLocal(IResource.DEPTH_INFINITE, progress.newChild(1));
			else if (delete)
				p.delete(false, true, progress.newChild(1));
			else
				closeMissingProject(p, projectFile, progress.newChild(1));
		}
	}

	/**
	 * Close a project that has already been deleted on disk. This will fall
	 * back to deleting the project if it cannot be successfully closed.
	 * <p>
	 * Closing a missing project involves creating a temporary '.project' file
	 * since only existing projects can be closed
	 *
	 * @param p
	 * @param projectFile
	 * @param monitor
	 * @throws CoreException
	 */
	static void closeMissingProject(IProject p, File projectFile,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		// Don't close/delete if already closed
		if (p.exists() && !p.isOpen())
			return;

		// Create temporary .project file so it can be closed
		boolean closeFailed = false;
		File projectRoot = projectFile.getParentFile();
		if (!projectRoot.isFile()) {
			boolean hasRoot = projectRoot.exists();
			try {
				if (!hasRoot)
					FileUtils.mkdirs(projectRoot, true);
				if (projectFile.createNewFile())
					p.close(progress.newChild(1));
				else
					closeFailed = true;
			} catch (IOException e) {
				closeFailed = true;
			} finally {
				// Clean up created .project file
				try {
					FileUtils.delete(projectFile, FileUtils.RETRY
							| FileUtils.SKIP_MISSING);
				} catch (IOException e) {
					closeFailed = true;
				}
				// Clean up created folder
				if (!hasRoot)
					try {
						FileUtils.delete(projectRoot, FileUtils.RETRY
								| FileUtils.SKIP_MISSING | FileUtils.RECURSIVE);
					} catch (IOException e) {
						closeFailed = true;
					}
			}
		} else
			closeFailed = true;
		// Delete projects that can't be closed
		if (closeFailed)
			p.delete(false, true, progress.newChild(1));
	}

	/**
	 * The method refreshes resources
	 *
	 * @param resources
	 *            resources to refresh
	 * @param monitor
	 * @throws CoreException
	 */
	public static void refreshResources(IResource[] resources,
			IProgressMonitor monitor) throws CoreException {
		try {
			SubMonitor progress = SubMonitor.convert(monitor,
					CoreText.ProjectUtil_refreshing, resources.length);
			for (IResource resource : resources) {
				if (progress.isCanceled())
					break;
				resource.refreshLocal(IResource.DEPTH_INFINITE,
						progress.newChild(1));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Refresh the resources that are within the passed repository paths.
	 *
	 * @param repository
	 * @param relativePaths
	 *            repository-relative paths to refresh
	 * @param monitor
	 * @throws CoreException
	 */
	public static void refreshRepositoryResources(Repository repository,
			Collection<String> relativePaths, IProgressMonitor monitor)
			throws CoreException {
		if (repository == null || repository.isBare()) {
			return;
		}
		if (relativePaths.isEmpty() || relativePaths.contains("")) { //$NON-NLS-1$
			refreshResources(getProjects(repository), monitor);
			return;
		}

		IPath repositoryPath = new Path(repository.getWorkTree().getAbsolutePath());
		IProject[] projects = null;
		Set<IResource> resources = new LinkedHashSet<>();
		for (String relativePath : relativePaths) {
			IPath location = repositoryPath.append(relativePath);
			IResource resource = ResourceUtil
					.getResourceForLocation(location, false);
			if (resource != null) {
				// Resource exists for path, refresh it
				resources.add(resource);
			} else {
				// Resource doesn't exist. Check if there are any projects
				// contained in the path, we need to refresh them.
				if (projects == null)
					projects = getProjects(repository);
				for (IProject project : projects) {
					IPath projectLocation = project.getLocation();
					if (projectLocation != null
							&& location.isPrefixOf(projectLocation))
						resources.add(project);
				}
			}
		}
		refreshResources(resources.toArray(new IResource[0]), monitor);
	}

	/**
	 * The method retrieves all accessible projects related to the given
	 * repository.
	 *
	 * @param repository
	 *            to get the projects of
	 * @return list of projects, with nested projects first.
	 */
	public static IProject[] getProjects(Repository repository) {
		if (repository == null || repository.isBare()) {
			return new IProject[0];
		}
		List<IProject> result = new ArrayList<>();
		for (IProject project : getProjectsUnderPath(
				new Path(repository.getWorkTree().getAbsolutePath()))) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null) {
				result.add(project);
			}
		}
		return result.toArray(new IProject[result.size()]);
	}

	/**
	 * The method returns all projects containing at least one of the given
	 * paths.
	 *
	 * @param repository
	 *            the repository who's working tree is used as base for lookup
	 * @param fileList
	 *            the list of files/directories to lookup
	 * @return valid projects containing one of the paths
	 * @throws CoreException
	 */
	public static IProject[] getProjectsContaining(Repository repository,
			Collection<String> fileList) throws CoreException {
		if (repository == null || repository.isBare()) {
			return new IProject[0];
		}
		Set<IProject> result = new LinkedHashSet<>();
		File workTree = repository.getWorkTree();

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = getProjectsForContainerMatch(root);

		for (String member : fileList) {
			File file = new File(workTree, member);

			for (IProject prj : projects) {
				if (checkContainerMatch(prj, file.getAbsolutePath())) {
					result.add(prj);
					break;
				}
			}
		}

		return result.toArray(new IProject[result.size()]);
	}

	/**
	 * Looks up the IProject containing the given file, if available. This is
	 * done by path comparison, which is very cheap compared to
	 * IWorkspaceRoot.findContainersForLocationURI(). If no project is found the
	 * code returns the {@link IWorkspaceRoot} or the file is inside the
	 * workspace.
	 *
	 * @param file
	 *            the path to lookup a container for
	 * @return the IProject or IWorkspaceRoot or <code>null</code> if not found.
	 */
	public static IContainer findProjectOrWorkspaceRoot(File file) {
		String absFile = file.getAbsolutePath();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] allProjects = getProjectsForContainerMatch(root);

		for (IProject prj : allProjects)
			if (checkContainerMatch(prj, absFile))
				return prj;

		if (checkContainerMatch(root, absFile))
			return root;

		return null;
	}

	private static IProject[] getProjectsForContainerMatch(IWorkspaceRoot root) {
		IProject[] allProjects = root.getProjects();

		// Sorting makes us look into nested projects first
		Arrays.sort(allProjects, new Comparator<IProject>() {
			@Override
			public int compare(IProject o1, IProject o2) {
				IPath l1 = o1.getLocation();
				IPath l2 = o2.getLocation();
				if (l1 != null && l2 != null)
					return -l1.toFile().compareTo(l2.toFile());
				else if (l1 != null)
					return -1;
				else if (l2 != null)
					return 1;
				else
					return 0;
			}

		});
		return allProjects;
	}

	private static boolean checkContainerMatch(IContainer container,
			String absFile) {
		IPath location = container.getLocation();
		if (location != null) {
			String absPrj = location.toFile().getAbsolutePath();
			if (absPrj.equals(absFile))
				return true;
			if (absPrj.length() < absFile.length()) {
				char sepChar = absFile.charAt(absPrj.length());
				if (sepChar == File.separatorChar && absFile.startsWith(absPrj))
					return true;
			}
		}
		return false;
	}

	/**
	 * Find projects located under the given path.
	 *
	 * @param path
	 *            absolute path under which to look for projects
	 * @return projects located under the given path
	 */
	public static IProject[] getProjectsUnderPath(@NonNull final IPath path) {
		IProject[] allProjects = getProjectsForContainerMatch(ResourcesPlugin
				.getWorkspace().getRoot());
		Set<IProject> projects = new HashSet<>();
		for (IProject p : allProjects) {
			IPath loc = p.getLocation();
			if (loc != null && path.isPrefixOf(loc)) {
				projects.add(p);
			}
		}
		return projects.toArray(new IProject[projects.size()]);
	}

	/**
	 * Find directories containing .project files recursively starting at given
	 * directory
	 *
	 * @param files
	 *            the collection to add the found projects to
	 * @param directory
	 *            where to search for project files
	 * @param searchNested
	 *            whether to search for nested projects or not
	 * @param monitor
	 * @return true if projects files found, false otherwise
	 */
	public static boolean findProjectFiles(final Collection<File> files,
			final File directory, boolean searchNested,
			final IProgressMonitor monitor) {
		return findProjectFiles(files, directory, searchNested, null, monitor);
	}

	private static boolean findProjectFiles(final Collection<File> files,
			final File directory, final boolean searchNested,
			final Set<String> visistedDirs, final IProgressMonitor monitor) {
		if (directory == null)
			return false;

		if (directory.getName().equals(Constants.DOT_GIT)
				&& FileKey.isGitRepository(directory, FS.DETECTED))
			return false;

		IProgressMonitor pm = monitor;
		if (pm == null)
			pm = new NullProgressMonitor();
		else if (pm.isCanceled())
			return false;

		pm.subTask(NLS.bind(CoreText.ProjectUtil_taskCheckingDirectory,
				directory.getPath()));

		final File[] contents = directory.listFiles();
		if (contents == null || contents.length == 0)
			return false;

		Set<String> directoriesVisited;
		// Initialize recursion guard for recursive symbolic links
		if (visistedDirs == null) {
			directoriesVisited = new HashSet<>();
			directoriesVisited.add(directory.getAbsolutePath());
		} else
			directoriesVisited = visistedDirs;

		// first look for project description files
		boolean foundProject = false;
		final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
		for (int i = 0; i < contents.length; i++) {
			File file = contents[i];
			if (file.isFile() && file.getName().equals(dotProject)) {
				files.add(file);
				foundProject = true;
			}
		}
		if (foundProject && !searchNested)
			return true;
		// recurse into sub-directories (even when project was found above, for nested projects)
		for (int i = 0; i < contents.length; i++) {
			// Skip non-directories
			if (!contents[i].isDirectory())
				continue;
			// Skip .metadata folders
			if (contents[i].getName().equals(METADATA_FOLDER))
				continue;
			String path = contents[i].getAbsolutePath();
			if (!directoriesVisited.add(path))
				// already been here --> do not recurse
				continue;
			findProjectFiles(files, contents[i], searchNested,
					directoriesVisited, pm);
		}
		return true;
	}
}
