/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bugs 477281, 478877
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * The "Create New Repository" wizard
 */
public class NewRepositoryWizard extends Wizard implements INewWizard {
	private final CreateRepositoryPage myCreatePage;

	private Repository repository;

	/**
	 * Default constructor. Needed for File->New->Other->Git->Git Repository
	 */
	public NewRepositoryWizard(){
		this(false);
	}

	/**
	 * @param hideBareOption
	 *            if <code>true</code>, no "bare" repository can be created
	 */
	public NewRepositoryWizard(boolean hideBareOption) {
		myCreatePage = new CreateRepositoryPage(hideBareOption);
	}

	@Override
	public void addPages() {
		setWindowTitle(UIText.NewRepositoryWizard_WizardTitle);
		setHelpAvailable(false);
		addPage(myCreatePage);
	}

	@Override
	public boolean performFinish() {
		try {
			boolean isBare = myCreatePage.getBare();
			File gitDir = Git.init()
					.setDirectory(FileUtils.canonicalize(
							new File(myCreatePage.getDirectory())))
					.setBare(isBare)
					.call().getRepository().getDirectory();
			this.repository = Activator.getDefault().getRepositoryCache()
					.lookupRepository(gitDir);
			Activator.getDefault().getRepositoryUtil()
					.addConfiguredRepository(gitDir);

			if (!isBare && doAutoShare()) {
				IPath workTree = new Path(repository.getWorkTree()
						.getAbsolutePath());
				IProject[] projects = ProjectUtil
						.getProjectsUnderPath(workTree);
				if (projects.length == 0)
					return true;
				autoShareProjects(repository, projects);
			}
		} catch (GitAPIException e) {
			org.eclipse.egit.ui.Activator.handleError(e.getMessage(), e, false);
		} catch (IOException e) {
			org.eclipse.egit.ui.Activator.handleError(e.getMessage(), e, false);
		}
		return true;
	}

	private boolean doAutoShare() {
		IEclipsePreferences d = DefaultScope.INSTANCE.getNode(Activator
				.getPluginId());
		IEclipsePreferences p = InstanceScope.INSTANCE.getNode(Activator
				.getPluginId());
		return p.getBoolean(GitCorePreferences.core_autoShareProjects,
				d.getBoolean(GitCorePreferences.core_autoShareProjects, true));
	}

	/**
	 * auto-share projects which are located inside newly created repository
	 *
	 * @param repoToCreate
	 * @param projects
	 */
	private void autoShareProjects(Repository repoToCreate, IProject[] projects) {
		final Map<IProject, File> projectsMap = new HashMap<>();
		for (IProject project : projects)
			projectsMap.put(project, repoToCreate.getDirectory());
		ConnectProviderOperation op = new ConnectProviderOperation(projectsMap);
		JobUtil.scheduleUserJob(op, CoreText.Activator_AutoShareJobName,
				JobFamilies.AUTO_SHARE);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to initialize
	}

	/**
	 * @return the newly created Repository in case of successful completion,
	 *         otherwise <code>null</code
	 */
	public Repository getCreatedRepository() {
		return repository;
	}
}
