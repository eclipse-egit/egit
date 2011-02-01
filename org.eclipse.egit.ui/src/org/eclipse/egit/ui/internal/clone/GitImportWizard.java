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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.ConfigurationChecker;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewWizardAction;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * The import wizard including options to clone/add repositories
 */
public class GitImportWizard extends Wizard implements ProjectCreator,
		IImportWizard {

	private final IProject[] previousProjects;

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
		previousProjects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
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
					importProjects();
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

	public void importProjects() {
		// TODO progress monitoring and cancellation
		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				switch (importWithDirectoriesPage.getWizardSelection()) {
				case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD:
					projectsImportPage.createProjects();
					break;
				case GitSelectWizardPage.NEW_WIZARD:
					new NewWizardAction(PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow()).run();
					break;
				case GitSelectWizardPage.GENERAL_WIZARD:
					try {

						final String projectName = createGeneralProjectPage
								.getProjectName();
						final String path = importWithDirectoriesPage.getPath();
						getContainer().run(true, false,
								new WorkspaceModifyOperation() {

									@Override
									protected void execute(
											IProgressMonitor monitor)
											throws CoreException,
											InvocationTargetException,
											InterruptedException {

										final IProjectDescription desc = ResourcesPlugin
												.getWorkspace()
												.newProjectDescription(
														projectName);
										desc.setLocation(new Path(path));

										IProject prj = ResourcesPlugin
												.getWorkspace().getRoot()
												.getProject(desc.getName());
										prj.create(desc, monitor);
										prj.open(monitor);

										ResourcesPlugin.getWorkspace()
												.getRoot().refreshLocal(
														IResource.DEPTH_ONE,
														monitor);

									}
								});
					} catch (InvocationTargetException e1) {
						Activator.handleError(e1.getMessage(), e1
								.getTargetException(), true);
					} catch (InterruptedException e1) {
						Activator.handleError(e1.getMessage(), e1, true);
					}
					break;

				}
			}
		});

	}

	public IProject[] getAddedProjects() {

		IProject[] currentProjects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();

		List<IProject> newProjects = new ArrayList<IProject>();

		for (IProject current : currentProjects) {
			boolean found = false;
			for (IProject previous : previousProjects) {
				if (previous.equals(current)) {
					found = true;
					break;
				}
			}
			if (!found) {
				newProjects.add(current);
			}
		}

		return newProjects.toArray(new IProject[0]);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to do
	}

}
