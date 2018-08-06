/*******************************************************************************
 * Copyright (c) 2010-2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Stefan Lay (SAP AG) - improvements
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositorySearchResult;
import org.eclipse.egit.ui.internal.provisional.wizards.NoRepositoryInfoException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;

/**
 * A wizard which allows to optionally clone a repository and to import projects from a repository.
 */
public class GitImportWizard extends AbstractGitCloneWizard implements IImportWizard {
	private static final String GIT_IMPORT_SECTION = "GitImportWizard"; //$NON-NLS-1$

	private GitSelectRepositoryPage selectRepoPage = new GitSelectRepositoryPage(
			false);

	private GitSelectWizardPage importWithDirectoriesPage = new GitSelectWizardPage(){
		@Override
		public void setVisible(boolean visible) {
			if (existingRepo == null && visible && (cloneDestination.cloneSettingsChanged())) {
				setCallerRunsCloneOperation(true);
				try {
					final GitRepositoryInfo repositoryInfo = currentSearchResult.getGitRepositoryInfo();
					performClone(repositoryInfo);
					importWithDirectoriesPage.getControl().getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							runCloneOperation(getContainer(), repositoryInfo);
							cloneDestination.saveSettingsForClonedRepo();
						}});
				} catch (URISyntaxException e) {
					Activator.error(UIText.GitImportWizard_errorParsingURI, e);
				} catch (NoRepositoryInfoException e) {
					Activator.error(UIText.GitImportWizard_noRepositoryInfo, e);
				} catch (Exception e) {
					Activator.error(e.getMessage(), e);
				}
			}
			super.setVisible(visible);
		}
	};

	private GitProjectsImportPage projectsImportPage = new GitProjectsImportPage() {
		@Override
		public void setVisible(boolean visible) {
			if (visible)
				setProjectsList(importWithDirectoriesPage.getPath());
			super.setVisible(visible);
		}
	};

	private GitCreateGeneralProjectPage createGeneralProjectPage = new GitCreateGeneralProjectPage() {
		@Override
		public void setVisible(boolean visible) {
			if (visible)
				setPath(importWithDirectoriesPage.getPath());
			super.setVisible(visible);
		}
	};

	private Repository existingRepo;

	/**
	 * The default constructor
	 */
	public GitImportWizard() {
		this(null);
	}

	/**
	 * Construct the import wizard based on given repository search result. The
	 * wizard skips the repository location page in this case.
	 *
	 * @param searchResult
	 *            the search result to initialize the import wizard with.
	 */
	public GitImportWizard(IRepositorySearchResult searchResult) {
		super(searchResult);
		setWindowTitle(UIText.GitImportWizard_WizardTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		setDialogSettings(getImportWizardDialogSettings());
	}

	@Override
	protected void addPreClonePages() {
		if (!hasSearchResult())
			addPage(selectRepoPage);
	}

	@Override
	protected void addPostClonePages() {
		addPage(importWithDirectoriesPage);
		addPage(projectsImportPage);
		addPage(createGeneralProjectPage);
	}

	@Override
	protected List<CloneSourceProvider> getCloneSourceProviders() {
		List<CloneSourceProvider> cloneSourceProvider = super.getCloneSourceProviders();
		cloneSourceProvider.add(0, CloneSourceProvider.LOCAL);
		return cloneSourceProvider;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to do
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == selectRepoPage) {
			existingRepo = selectRepoPage.getRepository();
			importWithDirectoriesPage.setRepository(selectRepoPage
					.getRepository());
			return importWithDirectoriesPage;
		} else if (page == cloneDestination) {
			existingRepo = null;
			importWithDirectoriesPage.setRepository(getTargetRepository());
			return importWithDirectoriesPage;
		} else if (page == importWithDirectoriesPage)
			switch (importWithDirectoriesPage.getWizardSelection()) {
			case GitSelectWizardPage.EXISTING_PROJECTS_WIZARD:
				return projectsImportPage;
			case GitSelectWizardPage.NEW_WIZARD:
				return null;
			case GitSelectWizardPage.GENERAL_WIZARD:
				return createGeneralProjectPage;
			}
		else if (page == createGeneralProjectPage
				|| page == projectsImportPage)
			return null;
		return super.getNextPage(page);
	}

	private Repository getTargetRepository() {
		if (existingRepo != null)
			return existingRepo;
		else
			try {
				return org.eclipse.egit.core.Activator
						.getDefault()
						.getRepositoryCache()
						.lookupRepository(
								new File(cloneDestination.getDestinationFile(),
										Constants.DOT_GIT));
			} catch (IOException e) {
				Activator
						.error("Error looking up repository at " + cloneDestination.getDestinationFile(), e); //$NON-NLS-1$
				return null;
			}
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
			final Set<ProjectRecord> projectsToCreate = new HashSet<>();
			final List<IWorkingSet> workingSets = new ArrayList<>();
			// get the data from the pages in the UI thread
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					projectsToCreate.addAll(projectsImportPage
							.getCheckedProjects());
					IWorkingSet[] workingSetArray = projectsImportPage
							.getSelectedWorkingSets();
					workingSets.addAll(Arrays.asList(workingSetArray));
					projectsImportPage.saveWidgetValues();
				}
			});
			ProjectUtils.createProjects(projectsToCreate,
					workingSets.toArray(new IWorkingSet[0]),
					monitor);
			break;
		}
		case GitSelectWizardPage.NEW_WIZARD: {
			final File[] repoDir = new File[1];
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					repoDir[0] = getTargetRepository().getDirectory();
				}
			});
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
					SubMonitor progress = SubMonitor.convert(actMonitor,
							currentProjects.length);
					for (IProject current : currentProjects)
						if (!previousProjects.contains(current)) {
							ConnectProviderOperation cpo = new ConnectProviderOperation(
									current, repoDir[0]);
							cpo.execute(progress.newChild(1));
						} else {
							progress.worked(1);
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
				@Override
				public void run() {
					projectName[0] = createGeneralProjectPage.getProjectName();
					defaultLocation[0] = createGeneralProjectPage
							.isDefaultLocation();
					path[0] = importWithDirectoriesPage.getPath();
					repoDir[0] = getTargetRepository().getDirectory();
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
						desc.setLocation(new Path(path[0]));
						SubMonitor progress = SubMonitor.convert(actMonitor, 4);
						IProject prj = ResourcesPlugin.getWorkspace().getRoot()
								.getProject(desc.getName());
						prj.create(desc, progress.newChild(1));
						prj.open(progress.newChild(1));
						ConnectProviderOperation cpo = new ConnectProviderOperation(
								prj, repoDir[0]);
						cpo.execute(progress.newChild(1));

						ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
								IResource.DEPTH_ONE, progress.newChild(1));
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

	static IDialogSettings getImportWizardDialogSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();

		IDialogSettings wizardSettings = settings
				.getSection(GitImportWizard.GIT_IMPORT_SECTION);
		if (wizardSettings == null) {
			wizardSettings = settings
					.addNewSection(GitImportWizard.GIT_IMPORT_SECTION);
		}
		return wizardSettings;
	}
}
