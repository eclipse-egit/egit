/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * Copyright (C) 2007, Martin Oberhuber (martin.oberhuber@windriver.com)
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
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
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
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
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		List<IProject> result = new ArrayList<IProject>();
		final File parentFile = repository.getWorkTree();
		for (IProject p : projects) {
			IPath projectLocation = p.getLocation();
			if (!p.isOpen() || projectLocation == null)
				continue;
			String projectFilePath = projectLocation.append(
					IProjectDescription.DESCRIPTION_FILE_NAME).toOSString();
			File projectFile = new File(projectFilePath);
			if (projectFile.exists()) {
				final File file = p.getLocation().toFile();
				if (file.getAbsolutePath().startsWith(
						parentFile.getAbsolutePath()))
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
	public static void refreshValidProjects(IProject[] projects,
			boolean delete, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(CoreText.ProjectUtil_refreshingProjects,
					projects.length);
			for (IProject p : projects) {
				if (monitor.isCanceled())
					break;
				IPath projectLocation = p.getLocation();
				if (projectLocation == null)
					continue;
				String projectFilePath = projectLocation.append(
						IProjectDescription.DESCRIPTION_FILE_NAME).toOSString();
				File projectFile = new File(projectFilePath);
				if (projectFile.exists())
					p.refreshLocal(IResource.DEPTH_INFINITE,
							new SubProgressMonitor(monitor, 1));
				else if (delete)
					p.delete(false, true, new SubProgressMonitor(monitor, 1));
				else
					closeMissingProject(p, projectFile, monitor);
				monitor.worked(1);
			}
		} finally {
			monitor.done();
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
					p.close(new SubProgressMonitor(monitor, 1));
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
			p.delete(false, true, new SubProgressMonitor(monitor, 1));
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
			monitor.beginTask(CoreText.ProjectUtil_refreshing,
					resources.length);
			for (IResource resource : resources) {
				if (monitor.isCanceled())
					break;
				resource.refreshLocal(IResource.DEPTH_INFINITE,
						new SubProgressMonitor(monitor, 1));
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}

	}

	/**
	 * The method retrieves all accessible projects related to the given
	 * repository
	 *
	 * @param repository
	 * @return list of projects
	 */
	public static IProject[] getProjects(Repository repository) {
		List<IProject> result = new ArrayList<IProject>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects)
			if (project.isAccessible()) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(project);
				if (mapping != null && mapping.getRepository() == repository)
					result.add(project);
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
		Set<IProject> result = new LinkedHashSet<IProject>();
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
			public int compare(IProject o1, IProject o2) {
				return -o1.getLocation().toFile()
						.compareTo(o2.getLocation().toFile());
			}

		});
		return allProjects;
	}

	private static boolean checkContainerMatch(IContainer container,
			String absFile) {
		String absPrj = container.getLocation().toFile().getAbsolutePath();
		if (absPrj.equals(absFile))
			return true;
		if (absPrj.length() < absFile.length()) {
			char sepChar = absFile.charAt(absPrj.length());
			if (sepChar == File.separatorChar && absFile.startsWith(absPrj))
				return true;
		}
		return false;
	}

	/**
	 * Find directories containing .project files recursively starting at given
	 * directory
	 *
	 * @param files the collection to add the found projects to
	 * @param directory where to search for project files
	 * @param searchNested whether to search for nested projects or not
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
			directoriesVisited = new HashSet<String>();
			try {
				directoriesVisited.add(directory.getCanonicalPath());
			} catch (IOException exception) {
				Activator.logError(exception.getLocalizedMessage(), exception);
			}
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
			try {
				String canonicalPath = contents[i].getCanonicalPath();
				if (!directoriesVisited.add(canonicalPath))
					// already been here --> do not recurse
					continue;
			} catch (IOException exception) {
				Activator.logError(exception.getLocalizedMessage(), exception);

			}
			findProjectFiles(files, contents[i], searchNested,
					directoriesVisited, pm);
		}
		return true;
	}
}
