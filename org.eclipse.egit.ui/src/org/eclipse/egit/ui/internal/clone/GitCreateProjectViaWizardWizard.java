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
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewWizardAction;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * A wizard used to import existing projects from a {@link Repository}
 * <p>
 * TODO String externalization
 */
public class GitCreateProjectViaWizardWizard extends Wizard {

	private final Repository myRepository;

	private final String myGitDir;

	private final IProject[] previousProjects;

	private GitSelectWizardPage mySelectionPage;

	private GitCreateGeneralProjectPage myCreateGeneralProjectPage;

	private GitProjectsImportPage myProjectsImportPage;

	private GitShareProjectsPage mySharePage;

	/**
	 * @param repository
	 * @param path
	 */
	public GitCreateProjectViaWizardWizard(Repository repository, String path) {
		super();
		previousProjects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		myRepository = repository;
		myGitDir = path;
		setWindowTitle(NLS.bind("Import projects from Git Repository {0}",
				myRepository.getDirectory().getPath()));

		// TODO the Import wizard could be started like this,
		// but throws an Exception if started within a wizard
		// context (no active workbench window found) and the
		// list of available wizards is empty
		// -> investigate if we can include that wizard
		//
		// IHandlerService handlerService = (IHandlerService)
		// PlatformUI.getWorkbench().getService(IHandlerService.class);
		//
		// handlerService.executeCommand("org.eclipse.ui.file.import",

	}

	@Override
	public void addPages() {

		mySelectionPage = new GitSelectWizardPage();
		addPage(mySelectionPage);
		myCreateGeneralProjectPage = new GitCreateGeneralProjectPage(myGitDir);
		addPage(myCreateGeneralProjectPage);
		// for "Import Existing Projects"
		// TODO new constructor with repository and directory
		myProjectsImportPage = new GitProjectsImportPage(false) {

			@Override
			public void setVisible(boolean visible) {
				setGitDir(myRepository.getDirectory());
				setProjectsList(myGitDir);
				super.setVisible(visible);
			}

		};
		addPage(myProjectsImportPage);
		mySharePage = new GitShareProjectsPage();
		addPage(mySharePage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {

		if (page == mySelectionPage) {

			switch (mySelectionPage.getWizardSelection()) {
			case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD:
				return myProjectsImportPage;
			case GitSelectWizardPage.NEW_WIZARD:
				if (mySelectionPage.getActionSelection() != GitSelectWizardPage.ACTION_DIALOG_SHARE)
					return null;
				else
					return mySharePage;

			case GitSelectWizardPage.GENERAL_WIZARD:
				return myCreateGeneralProjectPage;

			}

			return super.getNextPage(page);

		} else if (page == myCreateGeneralProjectPage
				|| page == myProjectsImportPage) {

			if (mySelectionPage.getActionSelection() != GitSelectWizardPage.ACTION_DIALOG_SHARE)
				return null;
			else
				return mySharePage;
		}
		return super.getNextPage(page);
	}

	@Override
	public boolean canFinish() {

		boolean showSharePage = mySelectionPage.getActionSelection() == GitSelectWizardPage.ACTION_DIALOG_SHARE;
		boolean showShareComplete = !showSharePage
				|| mySharePage.isPageComplete();

		switch (mySelectionPage.getWizardSelection()) {
		case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD:
			return myProjectsImportPage.isPageComplete() && showShareComplete;
		case GitSelectWizardPage.NEW_WIZARD:
			return showShareComplete;
		case GitSelectWizardPage.GENERAL_WIZARD:
			return myCreateGeneralProjectPage.isPageComplete()
					&& showShareComplete;
		}
		return super.canFinish();

	}

	@Override
	public boolean performFinish() {

		try {

			final int actionSelection = mySelectionPage.getActionSelection();

			final IProject[] projectsToShare;
			if (actionSelection != GitSelectWizardPage.ACTION_DIALOG_SHARE) {
				projectsToShare = getAddedProjects();
			} else {
				projectsToShare = mySharePage.getSelectedProjects();
			}

			getContainer().run(true, true, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {

					if (actionSelection != GitSelectWizardPage.ACTION_DIALOG_SHARE) {
						// in case of the share page, the import is done by the
						// share page
						// itself
						// TODO this currently must be run in the UI Thread due
						// to access to
						// SWT widgets
						importProjects();
					}

					if (actionSelection != GitSelectWizardPage.ACTION_NO_SHARE) {

						// TODO scheduling rule?
						for (IProject prj : projectsToShare) {
							if (monitor.isCanceled())
								throw new InterruptedException();
							//
							ConnectProviderOperation connectProviderOperation = new ConnectProviderOperation(
									prj, myRepository.getDirectory());
							try {
								connectProviderOperation.run(monitor);
							} catch (CoreException e) {
								throw new InvocationTargetException(e);
							}
						}

					}

				}
			});
		} catch (InvocationTargetException e) {
			Activator
					.handleError(e.getCause().getMessage(), e.getCause(), true);
			return false;
		} catch (InterruptedException e) {
			Activator.handleError("Action was aborted", e, true);
			return false;
		}
		return true;

	}

	/**
	 *
	 */
	public void importProjects() {

		// TODO progress monitoring and cancellation
		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				switch (mySelectionPage.getWizardSelection()) {
				case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD:
					myProjectsImportPage.createProjects();
					break;
				case GitSelectWizardPage.NEW_WIZARD:
					new NewWizardAction(PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow()).run();
					break;
				case GitSelectWizardPage.GENERAL_WIZARD:
					try {
						final String projectName = myCreateGeneralProjectPage
								.getProjectName();
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
										desc.setLocation(new Path(myGitDir));

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

	/**
	 * @return the projects added to the workspace since the start of this
	 *         wizard
	 */
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

}
