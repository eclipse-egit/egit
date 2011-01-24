/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Muskalla <benjamin.muskalla@tasktop.com> - initial API and implementation
 *     Ilya Ivanov <ilya.ivanov@intland.com> - implemented task repository url resolving
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.team.ui.LinkedTaskInfo;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.team.ui.AbstractTaskReference;

/**
 * Adapter factory to bridge between Mylyn and EGit domain models.
 */
@SuppressWarnings("restriction")
public class TaskReferenceFactory implements IAdapterFactory {
	private static final Class<?>[] ADAPTER_TYPES = new Class[] { AbstractTaskReference.class };

	@SuppressWarnings({ "rawtypes" })
	public Class[] getAdapterList() {
		return ADAPTER_TYPES;
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (!AbstractTaskReference.class.equals(adapterType))
			return null;

		return adaptFromObject(adaptableObject);
	}

	private AbstractTaskReference adaptFromObject(Object element) {
		RevCommit commit = getCommitForElement(element);
		if (commit != null) {
			return adaptFromRevCommit(commit);
		}

		return null;
	}

	/**
	 * Finds {@link TaskRepository} for provided {@link RevCommit} object and returns new {@link LinkedTaskInfo} object
	 * or <code>null</code> if nothing found.
	 * @param commit a {@link RevCommit} object to look for
	 * @return {@link LinkedTaskInfo} object, or <code>null</code> if repository not found
	 */
	private AbstractTaskReference adaptFromRevCommit(RevCommit commit) {
		Repository[] repositories = Activator.getDefault().getRepositoryCache().getAllRepositories();
		for (Repository r : repositories) {
			RevWalk revWalk = new RevWalk(r);

			String repoUrl = null;
			String message = null;

			// try to get repository url and commit message
			try {
				RevCommit revCommit = revWalk.parseCommit(commit);
				if (revCommit != null) {
					repoUrl = getRepoUrl(r);
					message = revCommit.getFullMessage();
				}
			} catch (Exception e) {
				continue;
			}

			if (message == null || message.trim().length() == 0) {
				continue;
			}

			String taskRepositoryUrl = null;
			if (repoUrl != null) {
				TaskRepository repository = getTaskRepositoryByGitRepoURL(repoUrl);
				if (repository != null) {
					taskRepositoryUrl = repository.getRepositoryUrl();
				}
			}

			return new LinkedTaskInfo(taskRepositoryUrl, null, null, message);
		}

		return null;
	}

	private static RevCommit getCommitForElement(Object element) {
		RevCommit commit = null;
		if (element instanceof RevCommit) {
			commit = (RevCommit) element;
		} else if (element instanceof GitModelCommit) {
			GitModelCommit modelCommit = (GitModelCommit) element;
			commit= modelCommit.getBaseCommit();
		}
		return commit;
	}

	/**
	 * Finds {@link TaskRepository} by provided Git repository Url
	 * @param repoUrl Git repository url
	 * @return {@link TaskRepository} associated with this Git repo or <code>null</code> if nothing found
	 */
	private TaskRepository getTaskRepositoryByGitRepoURL(final String repoUrl) {
		try {
			// replacing protocol name to aviod MalformedURIException
			URI uri = repoUrl == null ? null : new URI(repoUrl.replaceFirst("\\w+://", "http://")); //$NON-NLS-1$ //$NON-NLS-2$
			if (uri != null) {
				String gitHost = uri.toURL().getHost();
				return getTaskRepositoryByHost(gitHost);
			}
		} catch (Exception ex) {
			// It should be logged.
		}
		return null;
	}

	// TODO search in all configurations, not only 'origin' ?
	private static String getRepoUrl(Repository repo) {
		return repo.getConfig().getString("remote", "origin", "url"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private TaskRepository getTaskRepositoryByHost(String host) {
		List<TaskRepository> repositories = TasksUiPlugin.getRepositoryManager().getAllRepositories();
		if (repositories == null || repositories.isEmpty()) {
			return null;
		}

		if (repositories.size() == 1) {
			return repositories.iterator().next();
		}

		for (TaskRepository repository : repositories) {
			if (!repository.isOffline()) {
				try {
					URL url = new URL(repository.getRepositoryUrl());

					if (isSameHosts(host, url.getHost())) {
						return repository;
					}
				} catch (MalformedURLException e) {
					// We cannot do anything.
				}
			}
		}
		return null;
	}

	private boolean isSameHosts(String name1, String name2) {
		name1 = name1 == null ? "" : name1.trim(); //$NON-NLS-1$
		name2 = name2 == null ? "" : name2.trim(); //$NON-NLS-1$

		if (name1.equals(name2)) {
			return true;
		}

		String localHost = "localhost"; //$NON-NLS-1$
		String localHostName = localHost;
		try {
			localHostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException ex) {
			// Ignore it.
		}

		if (name1.length() == 0) {
			name1 = localHostName;
		}

		if (name2.length() == 0) {
			name2 = localHostName;
		}

		if (name1.equals(name2)) {
			return true;
		}

		if ((name1.equals(localHost) && name2.equals(localHostName)) || (name1.equals(localHostName) && name2.equals(localHost))) {
			return true;
		}

		try {
			String ipAddress1 = InetAddress.getByName(name1).getHostAddress();
			String ipAddress2 = InetAddress.getByName(name2).getHostAddress();

			return ipAddress1.equals(ipAddress2);
		} catch (UnknownHostException ex) {
			// Ignore it.
		}

		return false;
	}

}
