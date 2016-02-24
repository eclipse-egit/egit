/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 * Copyright (C) 2013, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2016, Andre Bossert <anb0s@anbos.de>
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.internal.CoreText;
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
	private final Map<IProject, File> projects = new LinkedHashMap<IProject, File>();

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
		finder.setFindInChildren(true);
		Collection<RepositoryMapping> repos = finder.find(subMon.newChild(50));
		if (repos.isEmpty()) {
			ms.add(Activator.error(NLS.bind(
					CoreText.ConnectProviderOperation_NoRepositoriesError,
					project.getName()), null));
			return;
		}
		GitProjectData projectData = new GitProjectData(project);
		try {
			projectData.setRepositoryMappings(repos);
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

		if (refreshResources) {
			project.refreshLocal(IResource.DEPTH_INFINITE, subMon.newChild(40));
		} else {
			subMon.worked(40);
		}

		autoIgnoreDerivedResources(project, subMon.newChild(10));
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

	@Override
	public ISchedulingRule getSchedulingRule() {
		Set<IProject> projectSet = projects.keySet();
		return new MultiRule(projectSet.toArray(new IProject[projectSet.size()]));
	}

	/**
	 * @param refresh
	 *            true to refresh resources after connect operation (default)
	 */
	public void setRefreshResources(boolean refresh) {
		this.refreshResources = refresh;
	}
}
