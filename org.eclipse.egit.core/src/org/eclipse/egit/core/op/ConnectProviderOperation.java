/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 * Copyright (C) 2013, Matthias Sohn <matthias.sohn@sap.com>
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryFinder;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Connects Eclipse to an existing Git repository
 */
public class ConnectProviderOperation implements IEGitOperation {
	private final Map<IProject, File> projects = new HashMap<IProject, File>();

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

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null) {
			monitor = new NullProgressMonitor();
		} else {
			monitor = m;
		}

		monitor.beginTask(CoreText.ConnectProviderOperation_connecting,
				100 * projects.size());
		try {

			for (Iterator iterator = projects.keySet().iterator(); iterator.hasNext();) {
				IProject project = (IProject) iterator.next();
				monitor.setTaskName(NLS.bind(
						CoreText.ConnectProviderOperation_ConnectingProject,
						project.getName()));
				// TODO is this the right location?
				if (GitTraceLocation.CORE.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.CORE.getLocation(),
							"Locating repository for " + project); //$NON-NLS-1$

				Collection<RepositoryMapping> repos = new RepositoryFinder(
						project).find(new SubProgressMonitor(monitor, 40));
				File suggestedRepo = projects.get(project);
				RepositoryMapping actualMapping= findActualRepository(repos, suggestedRepo);
				if (actualMapping != null) {
					GitProjectData projectData = new GitProjectData(project);
					try {
						projectData.setRepositoryMappings(Arrays.asList(actualMapping));
						projectData.store();
						GitProjectData.add(project, projectData);
					} catch (CoreException ce) {
						try {
							GitProjectData.delete(project);
						} catch (IOException e) {
							MultiStatus status = new MultiStatus(
									Activator.getPluginId(), IStatus.ERROR,
									e.getMessage(), e);
							status.add(new Status(IStatus.ERROR, Activator
									.getPluginId(), ce.getMessage(), ce));
							throw new CoreException(status);
						}
						throw ce;
					} catch (RuntimeException ce) {
						try {
							GitProjectData.delete(project);
						} catch (IOException e) {
							MultiStatus status = new MultiStatus(
									Activator.getPluginId(), IStatus.ERROR,
									e.getMessage(), e);
							status.add(new Status(IStatus.ERROR, Activator
									.getPluginId(), ce.getMessage(), ce));
							throw new CoreException(status);
					}
						throw ce;
					}
					RepositoryProvider
							.map(project, GitProvider.class.getName());
					autoIgnoreDerivedResources(project, monitor);
					project.refreshLocal(IResource.DEPTH_INFINITE,
							new SubProgressMonitor(monitor, 50));
					monitor.worked(10);
				} else {
					// TODO is this the right location?
					if (GitTraceLocation.CORE.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.CORE.getLocation(),
								"Attempted to share project without repository ignored :" //$NON-NLS-1$
										+ project);
					monitor.worked(60);
				}
			}
		} finally {
			monitor.done();
		}
	}

	private void autoIgnoreDerivedResources(IProject project,
			IProgressMonitor monitor) throws CoreException {
		List<IPath> paths = findDerivedResources(project);
		if (paths.size() > 0) {
			IgnoreOperation ignoreOp = new IgnoreOperation(paths);
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
			ignoreOp.execute(subMonitor);
		}
	}

	private List<IPath> findDerivedResources(IContainer c)
			throws CoreException {
		List<IPath> derived = new ArrayList<IPath>();
		IResource[] members = c.members(IContainer.INCLUDE_HIDDEN);
		for (IResource r : members) {
			if (r.isDerived())
				derived.add(r.getLocation());
			else if (r instanceof IContainer)
				derived.addAll(findDerivedResources((IContainer) r));
		}
		return derived;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#getSchedulingRule()
	 */
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
	private RepositoryMapping findActualRepository(
			Collection<RepositoryMapping> repos, File suggestedRepo) {
		for (RepositoryMapping rm : repos) {
			if (rm.getGitDirAbsolutePath().equals(Path.fromOSString(suggestedRepo.getPath())))
				return rm;
		}
		return null;
	}
}
