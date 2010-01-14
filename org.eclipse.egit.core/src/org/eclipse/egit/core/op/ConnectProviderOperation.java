/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryFinder;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Connects Eclipse to an existing Git repository
 */
public class ConnectProviderOperation implements IWorkspaceRunnable {
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
		this(proj, new File(Constants.DOT_GIT));
	}

	/**
	 * Create a new connection operation to execute within the workspace.
	 *
	 * @param proj
	 *            the project to connect to the Git team provider.
	 * @param pathToRepo
	 *            relative path to the repository
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

	public void run(IProgressMonitor m) throws CoreException {
		if (m == null) {
			m = new NullProgressMonitor();
		}

		m.beginTask(CoreText.ConnectProviderOperation_connecting,
				100 * projects.size());
		try {

			for (Iterator iterator = projects.keySet().iterator(); iterator.hasNext();) {
				IProject project = (IProject) iterator.next();
				m.setTaskName(NLS.bind(
						CoreText.ConnectProviderOperation_ConnectingProject,
						project.getName()));
				Activator.trace("Locating repository for " + project); //$NON-NLS-1$
				Collection<RepositoryMapping> repos = new RepositoryFinder(
						project).find(new SubProgressMonitor(m, 40));
				File suggestedRepo = projects.get(project);
				RepositoryMapping actualMapping= findActualRepository(repos, suggestedRepo);
				if (actualMapping != null) {
					GitProjectData projectData = new GitProjectData(project);
					try {
						projectData.setRepositoryMappings(Arrays.asList(actualMapping));
						projectData.store();
					} catch (CoreException ce) {
						GitProjectData.delete(project);
						throw ce;
					} catch (RuntimeException ce) {
						GitProjectData.delete(project);
						throw ce;
					}
					RepositoryProvider
							.map(project, GitProvider.class.getName());
					projectData = GitProjectData.get(project);
					project.refreshLocal(IResource.DEPTH_INFINITE,
							new SubProgressMonitor(m, 50));
					m.worked(10);
				} else {
					Activator
							.trace("Attempted to share project without repository ignored :" //$NON-NLS-1$
									+ project);
					m.worked(60);
				}
			}
		} finally {
			m.done();
		}
	}

	/**
	 * @param repos
	 *            available repositories
	 * @param suggestedRepo
	 *            relative path to git repository
	 * @return a repository mapping which corresponds to a suggested repository
	 *         location, <code>null</code> otherwise
	 */
	private RepositoryMapping findActualRepository(
			Collection<RepositoryMapping> repos, File suggestedRepo) {
		for (RepositoryMapping rm : repos) {
			if (Path.fromOSString(rm.getGitDir()).equals(Path.fromOSString(suggestedRepo.getPath())))
				return rm;
		}
		return null;
	}
}
