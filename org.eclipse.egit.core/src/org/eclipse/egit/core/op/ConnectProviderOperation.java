/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 * Copyright (C) 2013, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2016, Stefan Dirix <sdirix@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryFinder;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Connects Eclipse to an existing Git repository
 */
public class ConnectProviderOperation implements IEGitOperation {
	private final Map<IProject, File> projects = new LinkedHashMap<>();

	private boolean refreshResources = true;

	/**
	 * Create a new connection operation to execute within the workspace.
	 * <p>
	 * Uses <code>.git</code> as a default relative path to repository.
	 * @see #ConnectProviderOperation(IProject, File)
	 *
	 * @param proj
	 *            the project to connect to the Git team provider.
	 */
	public ConnectProviderOperation(final IProject proj) {
		this(proj, proj.getLocation().append(Constants.DOT_GIT).toFile());
	}

	/**
	 * Create a new connection operation to execute within the workspace.
	 *
	 * @param proj
	 *            the project to connect to the Git team provider.
	 * @param pathToRepo
	 *            absolute path to the repository
	 */
	public ConnectProviderOperation(final IProject proj, File pathToRepo) {
		this.projects.put(proj, pathToRepo);
	}

	/**
	 * Create a new connection operation to execute within the workspace.
	 *
	 * @param projects
	 *            the projects to connect to the Git team provider.
	 */
	public ConnectProviderOperation(final Map<IProject, File> projects) {
		this.projects.putAll(projects);
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		SubMonitor progress = SubMonitor.convert(m,
				CoreText.ConnectProviderOperation_connecting, projects.size());
		MultiStatus ms = new MultiStatus(Activator.getPluginId(), 0,
				CoreText.ConnectProviderOperation_ConnectErrors, null);
		for (Entry<IProject, File> entry : projects.entrySet()) {
			connectProject(entry, ms, progress.newChild(1));
		}
		if (!ms.isOK()) {
			throw new CoreException(ms);
		}
	}

	private void connectProject(Entry<IProject, File> entry, MultiStatus ms,
			IProgressMonitor monitor) throws CoreException {
		IProject project = entry.getKey();

		String taskName = NLS.bind(
				CoreText.ConnectProviderOperation_ConnectingProject,
				project.getName());
		SubMonitor subMon = SubMonitor.convert(monitor, taskName, 100);

		if (GitTraceLocation.CORE.isActive()) {
			GitTraceLocation.getTrace()
					.trace(GitTraceLocation.CORE.getLocation(), taskName);
		}

		RepositoryFinder finder = new RepositoryFinder(project);
		finder.setFindInChildren(false);
		Collection<RepositoryMapping> repos = finder.find(subMon.newChild(50));
		if (repos.isEmpty()) {
			ms.add(Activator.error(NLS.bind(
					CoreText.ConnectProviderOperation_NoRepositoriesError,
					project.getName()), null));
			return;
		}
		RepositoryMapping actualMapping = findActualRepository(repos,
				entry.getValue());
		if (actualMapping == null) {
			ms.add(Activator.error(NLS.bind(
					CoreText.ConnectProviderOperation_UnexpectedRepositoryError,
					new Object[] { project.getName(),
							entry.getValue().toString(), repos.toString() }),
					null));
			return;
		}
		GitProjectData projectData = new GitProjectData(project);
		try {
			projectData.setRepositoryMappings(Arrays.asList(actualMapping));
			projectData.store();
			GitProjectData.add(project, projectData);
		} catch (CoreException ce) {
			ms.add(ce.getStatus());
			deleteGitProvider(ms, project);
			return;
		} catch (RuntimeException ce) {
			ms.add(Activator.error(ce.getMessage(), ce));
			deleteGitProvider(ms, project);
			return;
		}
		RepositoryProvider.map(project, GitProvider.ID);

		IPath gitPath = actualMapping.getGitDirAbsolutePath();
		if (refreshResources) {
			touchGitResources(project, subMon.newChild(10));
			project.refreshLocal(IResource.DEPTH_INFINITE, subMon.newChild(30));
			if (gitPath != null) {
				try {
					Repository repository = org.eclipse.egit.core.Activator
							.getDefault().getRepositoryCache()
							.lookupRepository(gitPath.toFile());
					IndexDiffCacheEntry cacheEntry = org.eclipse.egit.core.Activator
							.getDefault().getIndexDiffCache()
							.getIndexDiffCacheEntry(repository);
					if (cacheEntry != null) {
						cacheEntry.refresh();
					}
				} catch (IOException e) {
					Activator.logError(e.getMessage(), e);
				}
			}
		} else {
			subMon.worked(40);
		}

		autoIgnoreDerivedResources(project, subMon.newChild(10));
		if (gitPath != null) {
			autoIgnoreWorkspaceMetaData(gitPath.toFile());
		}
	}

	/**
	 * Touches all descendants named ".git" so that they'll be included in a
	 * subsequent resource delta.
	 *
	 * @param project
	 *            to process
	 * @param monitor
	 *            for progress reporting and cancellation, may be {@code null}
	 *            if neither is desired
	 */
	private void touchGitResources(IProject project, IProgressMonitor monitor) {
		final SubMonitor progress = SubMonitor.convert(monitor, 1);
		try {
			project.accept(new IResourceProxyVisitor() {
				@Override
				public boolean visit(IResourceProxy resource)
						throws CoreException {
					int type = resource.getType();
					if ((type == IResource.FILE || type == IResource.FOLDER)
							&& Constants.DOT_GIT.equals(resource.getName())) {
						progress.setWorkRemaining(2);
						resource.requestResource().touch(progress.newChild(1));
						return false;
					}
					return true;
				}
			}, IResource.NONE);
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
		}
	}

	private void deleteGitProvider(MultiStatus ms, IProject project) {
		try {
			GitProjectData.delete(project);
		} catch (IOException e) {
			ms.add(Activator.error(e.getMessage(), e));
		}
	}

	private void autoIgnoreDerivedResources(IProject project,
			IProgressMonitor monitor) throws CoreException {
		if (!Activator.autoIgnoreDerived()) {
			return;
		}
		List<IPath> paths = findDerivedResources(project);
		if (paths.size() > 0) {
			IgnoreOperation ignoreOp = new IgnoreOperation(paths);
			ignoreOp.execute(monitor);
		}
	}

	/**
	 * Auto-ignore the .metadata and .recommenders folders located in the
	 * workspace root if the workspace is in the working tree of a git
	 * repository (which is not recommended)
	 *
	 * @param gitDir
	 *            the .git directory containing the repository metadata
	 */
	private static void autoIgnoreWorkspaceMetaData(File gitDir) {
		java.nio.file.Path workspaceRoot = ResourcesPlugin.getWorkspace()
				.getRoot().getLocation().toFile().toPath();
		try (Repository r = FileRepositoryBuilder.create(gitDir)) {
			if (!r.isBare()
					&& workspaceRoot.startsWith(r.getWorkTree().toPath())) {
				Collection<IPath> ignoredPaths = buildIgnoredPathsList(
						workspaceRoot, ".metadata", //$NON-NLS-1$
						".recommenders"); //$NON-NLS-1$
				JobUtil.scheduleUserJob(new IgnoreOperation(ignoredPaths),
						CoreText.ConnectProviderOperation_autoIgnoreMetaData,
						JobFamilies.AUTO_IGNORE);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}
	}

	private static Collection<IPath> buildIgnoredPathsList(
			java.nio.file.Path workspaceRoot,
			String... metaDataDirectoryNames) {
		Collection<IPath> ignoredPaths = new HashSet<>();
		for (String m : metaDataDirectoryNames) {
			Path metaData = new Path(
					workspaceRoot.resolve(m).toAbsolutePath().toString());
			try {
				if (RepositoryUtil.canBeAutoIgnored(metaData)) {
					ignoredPaths.add(metaData);
				}
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return ignoredPaths;
	}

	private List<IPath> findDerivedResources(IContainer c)
			throws CoreException {
		List<IPath> derived = new ArrayList<>();
		IResource[] members = c.members(IContainer.INCLUDE_HIDDEN);
		for (IResource r : members) {
			if (r.isDerived())
				derived.add(r.getLocation());
			else if (r instanceof IContainer)
				derived.addAll(findDerivedResources((IContainer) r));
		}
		return derived;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		Set<IProject> projectSet = projects.keySet();
		return new MultiRule(projectSet.toArray(new IProject[projectSet.size()]));
	}

	/**
	 * @param repos
	 *         available repositories
	 * @param suggestedRepo
	 *         relative path to git repository
	 * @return a repository mapping which corresponds to a suggested repository
	 *         location, <code>null</code> otherwise
	 */
	@Nullable
	private RepositoryMapping findActualRepository(
			Collection<RepositoryMapping> repos, File suggestedRepo) {
		File path = Path.fromOSString(suggestedRepo.getPath()).toFile();
		for (RepositoryMapping rm : repos) {
			IPath other = rm.getGitDirAbsolutePath();
			if (other == null) {
				continue;
			}
			if (path.equals(other.toFile())) {
				return rm;
			}
		}
		return null;
	}

	/**
	 * @param refresh
	 *            true to refresh resources after connect operation (default)
	 */
	public void setRefreshResources(boolean refresh) {
		this.refreshResources = refresh;
	}
}
