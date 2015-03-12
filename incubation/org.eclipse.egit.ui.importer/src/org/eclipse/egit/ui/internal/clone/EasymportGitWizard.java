/*******************************************************************************
 * Copyright (C) 2015 Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.) : initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.wizards.datatransfer.Activator;
import org.eclipse.ui.internal.wizards.datatransfer.NestedProjectsWizardPage;
import org.eclipse.ui.internal.wizards.datatransfer.SelectImportRootWizardPage;

public class EasymportGitWizard extends AbstractGitCloneWizard implements IImportWizard {

	private SelectImportRootWizardPage selectRootPage;
	private NestedProjectsWizardPage nestedProjectsPage;
	private GitSelectRepositoryPage selectRepoPage = new GitSelectRepositoryPage();
	private Repository existingRepo;

	public EasymportGitWizard() {
		super();
		IDialogSettings dialogSettings = super.getDialogSettings();
		if (dialogSettings == null) {
			dialogSettings = Activator.getDefault().getDialogSettings();
			setDialogSettings(dialogSettings);
		}
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
	}

	@Override
	protected void addPreClonePages() {
		if (!hasSearchResult()) {
			addPage(selectRepoPage);
		}
	}

	@Override
	protected void addPostClonePages() {
		this.selectRootPage = new SelectImportRootWizardPage(this, null, null) {
			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					if (existingRepo != null) {
						EasymportGitWizard.this.selectRootPage.setInitialSelectedDirectory(existingRepo.getWorkTree());
					} else  if ( cloneDestination.cloneSettingsChanged()) {
						setCallerRunsCloneOperation(true);
						try {
							final GitRepositoryInfo repositoryInfo = currentSearchResult.getGitRepositoryInfo();
							performClone(repositoryInfo);
						} catch (Exception e) {
							Activator.getDefault().getLog().log(new Status(
									IStatus.ERROR,
									Activator.getDefault().getBundle().getSymbolicName(),
									e.getMessage(),
									e));
						}
						this.setInitialSelectedDirectory(getCloneDestinationPage().getDestinationFile());
					}
				}
				super.setVisible(visible);
			}
		};
		addPage(this.selectRootPage);
		this.nestedProjectsPage = new NestedProjectsWizardPage(this, this.selectRootPage);
		addPage(this.nestedProjectsPage);
	}

	@Override
	public boolean performFinish() {
		this.nestedProjectsPage.performNestedImport();
		return true;
	}

	@Override
	public boolean canFinish() {
		return getContainer().getCurrentPage().equals(this.nestedProjectsPage) ||
				(getContainer().getCurrentPage().equals(this.selectRootPage) && this.selectRootPage.canFlipToNextPage());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to do
	}

	@Override
	protected List<CloneSourceProvider> getCloneSourceProviders() {
		List<CloneSourceProvider> cloneSourceProvider = super.getCloneSourceProviders();
		cloneSourceProvider.add(0, CloneSourceProvider.LOCAL);
		return cloneSourceProvider;
	}

	public CloneDestinationPage getCloneDestinationPage() {
		return this.cloneDestination;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == selectRepoPage) {
			this.existingRepo = this.selectRepoPage.getRepository();
			return this.selectRootPage;
		}
		return super.getNextPage(page);
	}
}
