/*******************************************************************************
 * Copyright (c) 2010 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Manuel Doninger <manuel.doninger@googlemail.com> - Branch checkout/creation
 *     Steffen Pingel <steffen.pingel@tasktop.com>
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.egit.core.ProjectReference;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.internal.mylyn.ui.Messages;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.mylyn.commons.core.storage.ICommonStorable;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.internal.context.tasks.ui.TaskContextStore;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.resources.ui.ResourcesUi;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskActivationListener;
import org.eclipse.mylyn.versions.core.ProjectSetConverter;
import org.eclipse.team.core.RepositoryProviderType;

/**
 * Manages the branch state of repositories based on the context of the active
 * task.
 */
public class TaskActivationListener implements ITaskActivationListener {

	private static final String PROJECTS_PSF = "projects.psf"; //$NON-NLS-1$

	/**
	 * Repositories referenced in the task context.
	 */
	private Set<Repository> managedRepositories = new HashSet<Repository>();

	/**
	 * Mapping of repository to branch when no task was active. This will be the
	 * default branch when the current task will be deactivated
	 */
	private Map<Repository, String> defaultRepositoryToBranch = new HashMap<Repository, String>();

	/**
	 * Saves the default branching state of all repositories an the map.
	 */
	public void preTaskActivated(ITask task) {
		defaultRepositoryToBranch.clear();
		RepositoryCache repoCache = org.eclipse.egit.core.Activator
				.getDefault().getRepositoryCache();
		for (Repository repo : repoCache.getAllRepositories()) {
			try {
				defaultRepositoryToBranch.put(repo, repo.getBranch());
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
	}

	/**
	 * Persists the branch state for repositories that are referenced in the
	 * task context.
	 */
	public void preTaskDeactivated(ITask task) {
		List<IProject> projectsInActiveContext = new ArrayList<IProject>(
				getProjectsInActiveContext());
		for (IProject project : projectsInActiveContext) {
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(project);
			if (repositoryMapping != null)
				managedRepositories.add(repositoryMapping.getRepository());
		}

		ICommonStorable storable = ((TaskContextStore) TasksUiPlugin
				.getContextStore()).getStorable(task);
		try {
			OutputStream out = storable.write(PROJECTS_PSF, null);
			try {
				ByteArrayOutputStream data = ProjectSetConverter
						.exportProjectSet(projectsInActiveContext);
				out.write(data.toByteArray());
			} finally {
				out.close();
			}
		} catch (Exception e) {
			Activator.logError(e.getMessage(), e);
		} finally {
			storable.release();
		}
	}

	private Set<IProject> getProjectsInActiveContext() {
		return ResourcesUi.getProjects(ContextCore.getContextManager()
				.getActiveContext());
	}

	/**
	 * Checks out branches stored in the task context.
	 */
	@SuppressWarnings("restriction")
	public void taskActivated(ITask task) {
		managedRepositories.clear();
		ICommonStorable storable = ((TaskContextStore) TasksUiPlugin
				.getContextStore()).getStorable(task);
		try {
			InputStream in = storable.read(PROJECTS_PSF, null);
			try {
				RepositoryProviderType gitProvider = RepositoryProviderType
						.getProviderType("org.eclipse.egit.core.GitProvider"); //$NON-NLS-1$
				List<String> projectReferencesString = ProjectSetConverter
						.readProjectReferences(in, gitProvider);
				Set<Repository> repositories = checkoutProjectReferences(projectReferencesString);
				managedRepositories.addAll(repositories);
			} finally {
				in.close();
			}
		} catch (Exception e) {
			Activator.logError(e.getMessage(), e);
			Activator
					.showError(
					Messages.TaskActivationListener_MylynCoreException,
							e);
		} finally {
			storable.release();
		}
	}

	/**
	 * Restores the default branching state of all repositories.
	 */
	public void taskDeactivated(ITask task) {
		// only change managed repositories
		for (Iterator<Repository> it = defaultRepositoryToBranch.keySet()
				.iterator(); it.hasNext();)
			if (!managedRepositories.contains(it.next()))
				it.remove();

		performBranchCheckout(defaultRepositoryToBranch);

		managedRepositories.clear();
		defaultRepositoryToBranch.clear();
	}

	private Set<Repository> checkoutProjectReferences(
			List<String> projectReferencesString) {
		List<ProjectReference> references = new ArrayList<ProjectReference>();
		for (String ref : projectReferencesString) {
			try {
				references.add(new ProjectReference(ref));
			} catch (Exception e) {
				Activator.logError(e.getMessage(), e);
				Activator.showError(
						Messages.TaskActivationListener_ReadingMalformedPSF, e);
			}
		}

		// find branch information for references
		Map<Repository, String> repositoryToBranch = new HashMap<Repository, String>();
		Map<String, Repository> remoteUrlToRepository = getRemoteUrlToRepository();
		for (ProjectReference ref : references) {
			String remoteUrl = ref.getRepository().toString();
			Repository repository = remoteUrlToRepository.get(remoteUrl);
			if (repository != null)
				repositoryToBranch.put(repository, ref.getBranch());
		}

		performBranchCheckout(repositoryToBranch);

		return repositoryToBranch.keySet();
	}

	private Map<String, Repository> getRemoteUrlToRepository() {
		Map<String, Repository> remoteUrlToRepository = new HashMap<String, Repository>();
		RepositoryCache repoCache = org.eclipse.egit.core.Activator
				.getDefault().getRepositoryCache();
		for (Repository repo : repoCache.getAllRepositories()) {
			StoredConfig config = repo.getConfig();
			Set<String> subsections = config.getSubsections("remote"); //$NON-NLS-1$
			for (String subsec : subsections) {
				String url = config.getString("remote", subsec, "url"); //$NON-NLS-1$//$NON-NLS-2$
				remoteUrlToRepository.put(url, repo);
			}
		}
		return remoteUrlToRepository;
	}

	private void performBranchCheckout(
			Map<Repository, String> repositoryToBranch) {
		for (Repository repository : repositoryToBranch.keySet()) {
			try {
				BranchOperation operation = new BranchOperation(repository,
						repositoryToBranch.get(repository));
				operation.execute(null);
			} catch (Exception e) {
				Activator.logError(e.getMessage(), e);
				Activator.showError(
								Messages.TaskActivationListener_BranchCheckoutFailed,
								e);
			}
		}
	}


}
