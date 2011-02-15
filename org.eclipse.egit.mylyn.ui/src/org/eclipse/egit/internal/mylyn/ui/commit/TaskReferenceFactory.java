/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Muskalla <bmuskalla@tasktop.com> - initial API and implementation
 *     Ilya Ivanov <ilya.ivanov@intland.com> - task repository url resolving
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.internal.mylyn.ui.EGitMylynUI;
import org.eclipse.egit.mylyn.ui.ITaskRepositoryResolver;
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

	private static final String BUGTRACK_SECTION = "bugtracker"; //$NON-NLS-1$
	private static final String BUGTRACK_URL = "url"; //$NON-NLS-1$

	// ID of extension point
	private static final String RESOLVER_ID = "org.eclipse.egit.mylyn.ui.extensionpoint.definition.repositoryResolver"; //$NON-NLS-1$

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
			RevWalk revWalk = new RevWalk(r);

			String message = null;
			String commitName = null;
			// try to resolve commit and extract info
			try {
				RevCommit revCommit = revWalk.parseCommit(commit);
				if (revCommit != null) {
					message = revCommit.getFullMessage();
					commitName = revCommit.getName();
				}
			} catch (Exception e) {
				// commit not fount in the repo
				continue;
			}

			String bugtrackerUrl = getConfiguredUrl(r);
			if (bugtrackerUrl == null) {
				// try to use extensions
				AbstractTaskReference ref = runResolverExtension(getRepoRoot(r), message, commitName);
				if (ref != null)
					return ref;
			}

			// if nothing worked, try to use url of origin
			if (bugtrackerUrl == null)
				bugtrackerUrl = getOriginUrl(r);

			if (message == null || message.trim().length() == 0)
				continue;

			String taskRepositoryUrl = null;
			if (bugtrackerUrl != null) {
				TaskRepository repository = getTaskRepositoryFromURL(bugtrackerUrl);
				if (repository != null)
					taskRepositoryUrl = repository.getRepositoryUrl();
			}

			return new LinkedTaskInfo(taskRepositoryUrl, null, null, message);
		}

		return null;
	}

	private IPath getRepoRoot(Repository r) {
		if (r.isBare())
			return null;
		return new Path(r.getDirectory().getParent());
	}

	private static RevCommit getCommitForElement(Object element) {
		RevCommit commit = null;
		if (element instanceof RevCommit)
			commit = (RevCommit) element;
		else if (element instanceof GitModelCommit) {
			GitModelCommit modelCommit = (GitModelCommit) element;
			commit = modelCommit.getBaseCommit();
		}
		return commit;
	}

	/**
	 * Finds {@link TaskRepository} by provided Git repository Url
	 * @param repoUrl Git repository url
	 * @return {@link TaskRepository} associated with this Git repo or <code>null</code> if nothing found
	 */
	private TaskRepository getTaskRepositoryFromURL(final String repoUrl) {
		try {
			// replacing protocol name to avoid MalformedURIException
			URI uri = repoUrl == null ? null : new URI(repoUrl.replaceFirst("\\w+://", "http://")); //$NON-NLS-1$ //$NON-NLS-2$
			if (uri != null) {
				String gitHost = uri.toURL().getHost();
				return getTaskRepositoryByHost(gitHost);
			}
		} catch (Exception ex) {
			EGitMylynUI.getDefault().getLog().log(
					new Status(IStatus.ERROR, EGitMylynUI.PLUGIN_ID, "failed to get repo url", ex)); //$NON-NLS-1$
		}
		return null;
	}

	private static String getConfiguredUrl(Repository repo) {
		return repo.getConfig().getString(BUGTRACK_SECTION, null, BUGTRACK_URL);
	}

	private static String getOriginUrl(Repository repo) {
		return repo.getConfig().getString("remote", "origin", "url");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		try {
			String ipAddress1 = InetAddress.getByName(hostname1).getHostAddress();
			String ipAddress2 = InetAddress.getByName(hostname2).getHostAddress();

			return ipAddress1.equals(ipAddress2);
		} catch (UnknownHostException ex) {
			// Ignore it.
		}

		return false;
	}

	private AbstractTaskReference runResolverExtension(final IPath repoRoot,
			final String message, final String commitSHA) {

		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(RESOLVER_ID);
		final AbstractTaskReference[] result = new AbstractTaskReference[1];

		try {
			for (IConfigurationElement e : config) {
				final Object o = e.createExecutableExtension("class"); //$NON-NLS-1$
				if (o instanceof ITaskRepositoryResolver)
					SafeRunner.run(new ISafeRunnable() {
						public void handleException(Throwable exception) {
							// ignore
						}

						public void run() throws Exception {
							result[0] = ((ITaskRepositoryResolver) o).createTaskRerference(
									repoRoot, message, commitSHA);
						}
					});

				if (result[0] != null)
					break;
			}
		} catch (CoreException ex) {
			EGitMylynUI.getDefault().getLog().log(
					new Status(IStatus.ERROR, EGitMylynUI.PLUGIN_ID, "Failed extension call", ex)); //$NON-NLS-1$
		}
		return result[0];
	}
}
