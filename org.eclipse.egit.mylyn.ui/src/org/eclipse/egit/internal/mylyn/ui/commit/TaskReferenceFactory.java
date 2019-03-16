/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Benjamin Muskalla <bmuskalla@tasktop.com> - initial API and implementation
 *     Ilya Ivanov <ilya.ivanov@intland.com> - task repository url resolving
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.internal.mylyn.ui.EGitMylynUI;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
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

	private static final String BUGTRACK_SECTION = "bugtracker"; //$NON-NLS-1$
	private static final String BUGTRACK_URL = "url"; //$NON-NLS-1$

	@Override
	public Class<?>[] getAdapterList() {
		final Class<?>[] c = new Class[ADAPTER_TYPES.length];
		System.arraycopy(ADAPTER_TYPES, 0, c, 0, c.length);
		return c;
	}

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (!AbstractTaskReference.class.equals(adapterType)) {
			return null;
		}
		return adapterType.cast(adaptFromObject(adaptableObject));
	}

	private AbstractTaskReference adaptFromObject(Object element) {
		RevCommit commit = getCommitForElement(element);
		if (commit != null)
			return adaptFromRevCommit(commit);

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

			String repoUrl = null;
			String message = null;
			long timestamp = 0;

			// try to get repository url and commit message
			try (RevWalk revWalk = new RevWalk(r)) {
				RevCommit revCommit = revWalk.parseCommit(commit);
				repoUrl = getRepoUrl(r);
				message = revCommit.getFullMessage();
				timestamp = (long) revCommit.getCommitTime() * 1000;
			} catch (IOException e) {
				continue;
			} catch (RuntimeException e) {
				continue;
			}

			if (message == null || message.trim().length() == 0)
				continue;

			String taskRepositoryUrl = null;
			if (repoUrl != null) {
				TaskRepository repository = getTaskRepositoryByGitRepoURL(repoUrl);
				if (repository != null)
					taskRepositoryUrl = repository.getRepositoryUrl();
			}

			return new LinkedTaskInfo(taskRepositoryUrl, null, null, message, timestamp);
		}

		return null;
	}

	private static RevCommit getCommitForElement(Object element) {
		RevCommit commit = null;
		if (element instanceof RevCommit)
			commit = (RevCommit) element;
		else if (element instanceof GitModelCommit) {
			GitModelCommit modelCommit = (GitModelCommit) element;
			if (!(modelCommit.getParent() instanceof GitModelRepository))
				return null; // should never happen

			GitModelRepository parent = (GitModelRepository) modelCommit.getParent();
			Repository repo = parent.getRepository();
			AbbreviatedObjectId id = modelCommit.getCachedCommitObj().getId();
			try (RevWalk rw = new RevWalk(repo)) {
				commit = rw.lookupCommit(id.toObjectId());
			}
		}
		return commit;
	}

	/**
	 * Finds {@link TaskRepository} by provided Git repository Url
	 * @param repoUrl Git repository url
	 * @return {@link TaskRepository} associated with this Git repo or <code>null</code> if nothing found
	 */
	private TaskRepository getTaskRepositoryByGitRepoURL(final String repoUrl) {
		if (repoUrl == null)
			return null;

		try {
			return getTaskRepositoryByHost(new URIish(repoUrl).getHost());
		} catch (Exception ex) {
			EGitMylynUI.getDefault().getLog().log(
					new Status(IStatus.ERROR, EGitMylynUI.PLUGIN_ID, "failed to get repo url", ex)); //$NON-NLS-1$
		}
		return null;
	}

	private static String getRepoUrl(Repository repo) {
		String configuredUrl = repo.getConfig().getString(BUGTRACK_SECTION, null, BUGTRACK_URL);
		String originUrl = repo.getConfig().getString(ConfigConstants.CONFIG_REMOTE_SECTION,
				Constants.DEFAULT_REMOTE_NAME, ConfigConstants.CONFIG_KEY_URL);
		return configuredUrl != null ? configuredUrl : originUrl;
	}

	private TaskRepository getTaskRepositoryByHost(String host) {
		List<TaskRepository> repositories = TasksUiPlugin.getRepositoryManager().getAllRepositories();
		if (repositories == null || repositories.isEmpty())
			return null;

		if (repositories.size() == 1)
			return repositories.iterator().next();

		for (TaskRepository repository : repositories) {
			if (!repository.isOffline()) {
				try {
					URL url = new URL(repository.getRepositoryUrl());

					if (isSameHosts(host, url.getHost()))
						return repository;
				} catch (MalformedURLException e) {
					// We cannot do anything.
				}
			}
		}
		return null;
	}

	private boolean isSameHosts(final String name1, final String name2) {
		String hostname1 = name1 == null ? "" : name1.trim(); //$NON-NLS-1$
		String hostname2 = name2 == null ? "" : name2.trim(); //$NON-NLS-1$

		if (hostname1.equals(hostname2))
			return true;

		String localHost = "localhost"; //$NON-NLS-1$
		String resolvedHostName;
		try {
			resolvedHostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException ex) {
			resolvedHostName = localHost;
		}

		if (hostname1.length() == 0)
			hostname1 = resolvedHostName;

		if (hostname2.length() == 0)
			hostname2 = resolvedHostName;

		if (hostname1.equals(hostname2))
			return true;

		if ((hostname1.equals(localHost) && hostname2.equals(resolvedHostName))
				|| (hostname1.equals(resolvedHostName) && hostname2.equals(localHost)))
			return true;

		return false;
	}

}
