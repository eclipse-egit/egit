/*******************************************************************************
 * Copyright (c) 2010 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Manuel Doninger <manuel.doninger@googlemail.com> - Branch checkout/creation
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.tasks;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.ProjectReference;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskActivationListener;
import org.eclipse.mylyn.versions.core.ProjectSetConverter;
import org.eclipse.team.core.RepositoryProviderType;

/**
 *
 */
public class TaskActivationListener implements ITaskActivationListener {

	Set<Repository> usedRepositories = new HashSet<Repository>();

	public void preTaskDeactivated(ITask task) {

		List<IProject> projectsInActiveContext = ProjectSetConverter
				.getProjectsInActiveContext();

		for (IProject project : projectsInActiveContext) {
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(project);
			Repository repo = repositoryMapping.getRepository();
			usedRepositories.add(repo);

		}

		//TODO: Move to a task activation listener in plugin org.eclipse.mylyn.versions.core
		ProjectSetConverter.exportProjectSet(task);
	}

	@SuppressWarnings("restriction")
	public void taskActivated(ITask task) {
		usedRepositories = new HashSet<Repository>();

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects(IContainer.INCLUDE_HIDDEN);

		Set<Repository> allRepositories = new HashSet<Repository>();

		for (IProject project : projects) {
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(project);
			if (repositoryMapping != null)
				allRepositories.add(repositoryMapping.getRepository());
		}

		Map<String, Repository> remoteMapping = new HashMap<String, Repository>();

		for (Repository repo : allRepositories) {
			StoredConfig config = repo.getConfig();
			Set<String> subsections = config.getSubsections("remote"); //$NON-NLS-1$
			for (String subsec : subsections) {
				String url = config.getString("remote", subsec, "url"); //$NON-NLS-1$//$NON-NLS-2$
				remoteMapping.put(url, repo);
			}
		}

		RepositoryProviderType providerType = RepositoryProviderType
				.getProviderType("org.eclipse.egit.core.GitProvider"); //$NON-NLS-1$
		ArrayList<String> projectReferencesString = ProjectSetConverter
				.getProjectReferences(task, providerType);
		ArrayList<ProjectReference> references = new ArrayList<ProjectReference>();

		for (String ref : projectReferencesString) {
			try {
				references.add(new ProjectReference(ref));
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Map<Repository, String> repositoriesInPSF = new HashMap<Repository, String>();

		for (ProjectReference ref : references) {
			if (remoteMapping.containsKey(ref.getRepository().toString())) {
				repositoriesInPSF.put(
						remoteMapping.get(ref.getRepository().toString()),
						ref.getBranch());
			}
		}

		// String branch = task.getTaskKey() != null ? task.getTaskKey() : task
		// .getTaskId();
		// String branchFullName = Constants.R_HEADS + branch;

		if (repositoriesInPSF.isEmpty()) {
			// RepositoryAndBranchSelectionDialog dialog = new
			// RepositoryAndBranchSelectionDialog(
			// PlatformUI.getWorkbench().getActiveWorkbenchWindow()
			// .getShell(), branchFullName);
			// if (dialog.open() == Window.OK) {
			// Set<Repository> repos = dialog.getSelectedRepositories();
			// performBranchCheckout(branch, repos);
			// performBranchCheckout(dialog.getBranch(), repos);
			// }
		} else
			performBranchCheckout(repositoriesInPSF);
	}

	private void performBranchCheckout(
			Map<Repository, String> reposBranchMapping) {
		usedRepositories.addAll(reposBranchMapping.keySet());
		try {
			for (Repository repo : reposBranchMapping.keySet()) {
				// Create new branch, if branch with proposed name doesn't
				// exist, otherwise checkout
				if (repo.getRefDatabase().getRef(reposBranchMapping.get(repo)) == null) {
					CreateLocalBranchOperation createOperation = new CreateLocalBranchOperation(
							repo, reposBranchMapping.get(repo),
							repo.getRef(Constants.R_REMOTES
									+ Constants.DEFAULT_REMOTE_NAME
									+ "/" + Constants.MASTER), //$NON-NLS-1$
							null);
					createOperation.execute(null);
				}

				// BranchOperation operation = new BranchOperation(repo,
				// Constants.R_HEADS + defaultBranch);
				BranchOperation operation = new BranchOperation(repo,
						reposBranchMapping.get(repo));
				operation.execute(null);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void taskDeactivated(ITask task) {
		// TODO Auto-generated method stub
	}

	public void preTaskActivated(ITask task) {
		// TODO Auto-generated method stub

	}
}
