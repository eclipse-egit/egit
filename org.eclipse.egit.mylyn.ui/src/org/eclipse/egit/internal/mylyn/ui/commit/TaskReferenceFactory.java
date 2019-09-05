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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.egit.internal.mylyn.ui.EGitMylynUI;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Config;
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

	private static final String LOCALHOST = "localhost"; //$NON-NLS-1$

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
		IRepositoryCommit commit = getCommitForElement(element);
		if (commit != null) {
			return adaptFromCommit(commit);
		}
		return null;
	}

	/**
	 * Finds the {@link TaskRepository} for the provided
	 * {@link IRepositoryCommit} object and returns new {@link LinkedTaskInfo}
	 * object or <code>null</code> if nothing found.
	 *
	 * @param commit
	 *            an {@link IRepositoryCommit} object to find the task info for
	 * @return {@link LinkedTaskInfo} object, or <code>null</code> if repository
	 *         not found
	 */
	private AbstractTaskReference adaptFromCommit(IRepositoryCommit commit) {
		Repository r = commit.getRepository();
		TaskRepository repository = getTaskRepository(r);
		if (repository == null) {
			return null;
		}
		String taskRepositoryUrl = repository.getRepositoryUrl();

		String message = null;
		long timestamp = 0;
		try (RevWalk revWalk = new RevWalk(r)) {
			RevCommit revCommit = revWalk.parseCommit(commit.getRevCommit());
			message = revCommit.getFullMessage();
			timestamp = (long) revCommit.getCommitTime() * 1000;
		} catch (IOException | RuntimeException e) {
			return null;
		}
		if (message == null || message.trim().isEmpty()) {
			return null;
		}
		return new LinkedTaskInfo(taskRepositoryUrl, null, null, message,
				timestamp);
	}

	private static IRepositoryCommit getCommitForElement(Object element) {
		if (element instanceof IRepositoryCommit) {
			// plugin.xml references SWTCommit, but that's internal
			return (IRepositoryCommit) element;
		} else if (element instanceof GitModelCommit) {
			GitModelCommit modelCommit = (GitModelCommit) element;
			if (!(modelCommit.getParent() instanceof GitModelRepository))
				return null; // should never happen

			GitModelRepository parent = (GitModelRepository) modelCommit.getParent();
			Repository repo = parent.getRepository();
			AbbreviatedObjectId id = modelCommit.getCachedCommitObj().getId();
			try (RevWalk rw = new RevWalk(repo)) {
				RevCommit commit = rw.lookupCommit(id.toObjectId());
				return new IRepositoryCommit() {

					@Override
					public Repository getRepository() {
						return repo;
					}

					@Override
					public RevCommit getRevCommit() {
						return commit;
					}
				};
			}
		}
		return null;
	}

	/**
	 * Finds a {@link TaskRepository} for the given {@link Repository}.
	 *
	 * @param repository
	 *            git repository to find a task repository for
	 * @return {@link TaskRepository} associated with this git repository or
	 *         {@code null} if none found
	 */
	private TaskRepository getTaskRepository(Repository repository) {
		Config config = repository.getConfig();
		String url = config.getString(BUGTRACK_SECTION, null, BUGTRACK_URL);
		if (url != null) {
			return TasksUiPlugin.getRepositoryManager().getRepository(url);
		}
		// Try to find any that uses the same host as the configured origin URL
		url = config.getString(ConfigConstants.CONFIG_REMOTE_SECTION,
				Constants.DEFAULT_REMOTE_NAME, ConfigConstants.CONFIG_KEY_URL);
		if (url == null) {
			return null;
		}
		try {
			return getTaskRepositoryByHost(new URIish(url).getHost());
		} catch (Exception ex) {
			EGitMylynUI.getDefault().getLog().log(
					new Status(IStatus.ERROR, EGitMylynUI.PLUGIN_ID, "failed to get repo url", ex)); //$NON-NLS-1$
		}
		return null;
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

	private boolean isSameHosts(String name1, String name2) {
		String hostname1 = name1 == null ? LOCALHOST : name1.trim();
		String hostname2 = name2 == null ? LOCALHOST : name2.trim();
		if (hostname1.isEmpty()) {
			hostname1 = LOCALHOST;
		}
		if (hostname2.isEmpty()) {
			hostname2 = LOCALHOST;
		}

		return hostname1.equals(hostname2);
	}

}
