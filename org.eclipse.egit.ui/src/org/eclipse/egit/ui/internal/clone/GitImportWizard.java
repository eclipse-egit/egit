/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.ConfigurationChecker;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;

/**
 * The import wizard including options to clone/add repositories
 */
public class GitImportWizard extends Wizard implements IImportWizard {
	private GitSelectRepositoryPage selectRepoPage = new GitSelectRepositoryPage();

	private GitSelectWizardPage importWithDirectoriesPage = new GitSelectWizardPage();

	private GitProjectsImportPage projectsImportPage = new GitProjectsImportPage();

	private GitCreateGeneralProjectPage createGeneralProjectPage = new GitCreateGeneralProjectPage();

	/**
	 * Default constructor
	 */
	public GitImportWizard() {
		setWindowTitle(UIText.GitImportWizard_WizardTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		selectRepoPage.setWizard(this);
		setNeedsProgressMonitor(true);
		ConfigurationChecker.checkConfiguration();
	}

	@Override
	public void addPages() {
		addPage(selectRepoPage);
		addPage(importWithDirectoriesPage);
		addPage(projectsImportPage);
		addPage(createGeneralProjectPage);
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					importProjects(monitor);
				}
			});
		} catch (InvocationTargetException e) {
			Activator
					.handleError(e.getCause().getMessage(), e.getCause(), true);
			return false;
		} catch (InterruptedException e) {
			Activator.handleError(
					UIText.GitCreateProjectViaWizardWizard_AbortedMessage, e,
					true);
			return false;
		}
		return true;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == selectRepoPage) {
			importWithDirectoriesPage.setRepository(selectRepoPage
					.getRepository());
			return importWithDirectoriesPage;
		} else if (page == importWithDirectoriesPage) {

			switch (importWithDirectoriesPage.getWizardSelection()) {
			case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD:
				projectsImportPage.setProjectsList(importWithDirectoriesPage
						.getPath());
				return projectsImportPage;
			case GitSelectWizardPage.NEW_WIZARD:
				return null;
			case GitSelectWizardPage.GENERAL_WIZARD:
				createGeneralProjectPage.setPath(importWithDirectoriesPage
						.getPath());
				return createGeneralProjectPage;

			}

		} else if (page == createGeneralProjectPage
				|| page == projectsImportPage) {
			return null;
		}
		return super.getNextPage(page);
	}

	@Override
	public boolean canFinish() {
		switch (importWithDirectoriesPage.getWizardSelection()) {
		case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD:
			return projectsImportPage.isPageComplete();
		case GitSelectWizardPage.NEW_WIZARD:
			return true;
		case GitSelectWizardPage.GENERAL_WIZARD:
			return createGeneralProjectPage.isPageComplete();
		}
		return super.canFinish();

	}

	private void importProjects(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		switch (importWithDirectoriesPage.getWizardSelection()) {
		case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD: {
			final Set<ProjectRecord> projectsToCreate = new HashSet<ProjectRecord>();
			final List<IWorkingSet> workingSets = new ArrayList<IWorkingSet>();
			final Repository[] repository = new Repository[1];
			// get the data from the pages in the UI thread
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					projectsToCreate.addAll(projectsImportPage
							.getCheckedProjects());
					IWorkingSet[] workingSetArray = projectsImportPage
							.getSelectedWorkingSets();
					workingSets.addAll(Arrays.asList(workingSetArray));
					repository[0] = selectRepoPage.getRepository();
				}
			});
			ProjectUtils.createProjects(projectsToCreate, repository[0],
					workingSets.toArray(new IWorkingSet[workingSets.size()]),
					monitor);
			break;
		}
		case GitSelectWizardPage.NEW_WIZARD: {
			final File[] repoDir = new File[1];
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					repoDir[0] = selectRepoPage.getRepository().getDirectory();
				}
			});
			final List<IProject> previousProjects = Arrays
					.asList(ResourcesPlugin.getWorkspace().getRoot()
							.getProjects());
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					new NewProjectAction(PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow()).run();
				}
			});
			IWorkspaceRunnable wsr = new IWorkspaceRunnable() {
				public void run(IProgressMonitor actMonitor)
						throws CoreException {
					IProject[] currentProjects = ResourcesPlugin.getWorkspace()
							.getRoot().getProjects();
					for (IProject current : currentProjects) {
						if (!previousProjects.contains(current)) {
							ConnectProviderOperation cpo = new ConnectProviderOperation(
									current, repoDir[0]);
							cpo.execute(actMonitor);
						}
					}
				}
			};
			try {
				ResourcesPlugin.getWorkspace().run(wsr, monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
			break;
		}
		case GitSelectWizardPage.GENERAL_WIZARD: {
			final String[] projectName = new String[1];
			final boolean[] defaultLocation = new boolean[1];
			final String[] path = new String[1];
			final File[] repoDir = new File[1];
			// get the data from the page in the UI thread
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					projectName[0] = createGeneralProjectPage.getProjectName();
					defaultLocation[0] = createGeneralProjectPage
							.isDefaultLocation();
					path[0] = importWithDirectoriesPage.getPath();
					repoDir[0] = selectRepoPage.getRepository().getDirectory();
				}
			});
			try {
				IWorkspaceRunnable wsr = new IWorkspaceRunnable() {
					public void run(IProgressMonitor actMonitor)
							throws CoreException {
						final IProjectDescription desc = ResourcesPlugin
								.getWorkspace().newProjectDescription(
										projectName[0]);
						desc.setLocation(new Path(path[0]));

						IProject prj = ResourcesPlugin.getWorkspace().getRoot()
								.getProject(desc.getName());
						prj.create(desc, actMonitor);
						prj.open(actMonitor);
						ConnectProviderOperation cpo = new ConnectProviderOperation(
								prj, repoDir[0]);
						cpo.execute(new NullProgressMonitor());

						ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
								IResource.DEPTH_ONE, actMonitor);
					}
				};
				ResourcesPlugin.getWorkspace().run(wsr, monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
			break;
		}
		}
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to do
	}
}
