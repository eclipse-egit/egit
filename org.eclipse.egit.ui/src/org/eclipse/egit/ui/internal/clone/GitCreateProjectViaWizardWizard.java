/*******************************************************************************
 * Copyright (c) 2010, 2013, 2017 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Wim Jongman (wim.jongman@remainsoftware.com) - Bug 358152
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;

/**
 * A wizard used to import existing projects from a {@link Repository}
 */
public class GitCreateProjectViaWizardWizard extends Wizard {
	private final Repository myRepository;

	private final String myGitDir;

	private GitSelectWizardPage mySelectionPage;

	private GitCreateGeneralProjectPage myCreateGeneralProjectPage;

	private GitProjectsImportPage myProjectsImportPage;

	private List<String> myFilter;

	/**
	 * @param repository
	 * @param path
	 */
	public GitCreateProjectViaWizardWizard(Repository repository, String path) {
		super();
		myRepository = repository;
		myGitDir = path;
		setNeedsProgressMonitor(true);
		setWindowTitle(NLS.bind(
				UIText.GitCreateProjectViaWizardWizard_WizardTitle,
				myRepository.getDirectory().getPath()));
		setDialogSettings(GitImportWizard.getImportWizardDialogSettings());
	}

	@Override
	public void addPages() {
		mySelectionPage = new GitSelectWizardPage(myRepository, myGitDir);
		addPage(mySelectionPage);
		myCreateGeneralProjectPage = new GitCreateGeneralProjectPage(myGitDir) {
			@Override
			public void setVisible(boolean visible) {
				setPath(mySelectionPage.getPath());
				super.setVisible(visible);
			}
		};
		addPage(myCreateGeneralProjectPage);
		myProjectsImportPage = new GitProjectsImportPage() {
			@Override
			public void setVisible(boolean visible) {
				setProjectsList(mySelectionPage.getPath());
				super.setVisible(visible);
			}
		};
		addPage(myProjectsImportPage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == mySelectionPage) {
			switch (mySelectionPage.getWizardSelection()) {
			case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD:
				return myProjectsImportPage;
			case GitSelectWizardPage.NEW_WIZARD:
				return null;
			case GitSelectWizardPage.GENERAL_WIZARD:
				return myCreateGeneralProjectPage;
			}
			return super.getNextPage(page);
		} else if (page == myCreateGeneralProjectPage
				|| page == myProjectsImportPage) {
			return null;
		}
		return super.getNextPage(page);
	}

	@Override
	public boolean canFinish() {
		switch (mySelectionPage.getWizardSelection()) {
		case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD:
			return myProjectsImportPage.isPageComplete();
		case GitSelectWizardPage.NEW_WIZARD:
			return true;
		case GitSelectWizardPage.GENERAL_WIZARD:
			return myCreateGeneralProjectPage.isPageComplete();
		}
		return super.canFinish();
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
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

	private void importProjects(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		switch (mySelectionPage.getWizardSelection()) {
		case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD: {
			final Set<ProjectRecord> projectsToCreate = new HashSet<>();
			final List<IWorkingSet> workingSets = new ArrayList<>();
			// get the data from the page in the UI thread
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					projectsToCreate.addAll(myProjectsImportPage
							.getCheckedProjects());
					IWorkingSet[] workingSetArray = myProjectsImportPage
							.getSelectedWorkingSets();
					workingSets.addAll(Arrays.asList(workingSetArray));
					myProjectsImportPage.saveWidgetValues();
				}
			});
			ProjectUtils.createProjects(projectsToCreate,
					workingSets.toArray(new IWorkingSet[workingSets.size()]),
					monitor);
			break;
		}
		case GitSelectWizardPage.NEW_WIZARD: {
			final List<IProject> previousProjects = Arrays
					.asList(ResourcesPlugin.getWorkspace().getRoot()
							.getProjects());
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					new NewProjectAction(PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow()).run();
				}
			});
			IWorkspaceRunnable wsr = new IWorkspaceRunnable() {
				@Override
				public void run(IProgressMonitor actMonitor)
						throws CoreException {
					IProject[] currentProjects = ResourcesPlugin.getWorkspace()
							.getRoot().getProjects();
					for (IProject current : currentProjects) {
						if (!previousProjects.contains(current)) {
							ConnectProviderOperation cpo = new ConnectProviderOperation(
									current, myRepository.getDirectory());
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
			// get the data from the page in the UI thread
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					projectName[0] = myCreateGeneralProjectPage
							.getProjectName();
					defaultLocation[0] = myCreateGeneralProjectPage
							.isDefaultLocation();
				}
			});

			try {
				IWorkspaceRunnable wsr = new IWorkspaceRunnable() {
					@Override
					public void run(IProgressMonitor actMonitor)
							throws CoreException {
						final IProjectDescription desc = ResourcesPlugin
								.getWorkspace().newProjectDescription(
										projectName[0]);
						if (!defaultLocation[0])
							desc.setLocation(new Path(myGitDir));

						IProject prj = ResourcesPlugin.getWorkspace().getRoot()
								.getProject(desc.getName());
						prj.create(desc, actMonitor);
						prj.open(actMonitor);

						ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
								IResource.DEPTH_ONE, actMonitor);

						File repoDir = myRepository.getDirectory();
						ConnectProviderOperation cpo = new ConnectProviderOperation(
								prj, repoDir);
						cpo.execute(new NullProgressMonitor());
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

	/**
	 * Add a list of paths that may match projects that this wizard can show. If
	 * no filter is set or if the filter is empty then all projects will show.
	 * If a non empty filter is set and no projects match then the wizard will
	 * not show any projects.
	 *
	 * @param filter
	 *            a list of paths
	 */
	public void setFilter(List<String> filter) {
		myFilter = filter;
	}

	/**
	 * Gets the list of projects that will filter in a subset of all eligible
	 * projects.
	 *
	 * @return an unmodifiable list of projects which could be empty but never
	 *         null.
	 * @see #setFilter(List)
	 */
	public List<String> getFilter() {
		if (myFilter == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(myFilter);
	}
}
