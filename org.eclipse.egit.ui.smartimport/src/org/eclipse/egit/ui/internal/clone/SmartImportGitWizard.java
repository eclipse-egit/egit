/*******************************************************************************
 * Copyright (C) 2015 Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.) : initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo;
import org.eclipse.egit.ui.internal.provisional.wizards.NoRepositoryInfoException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportRootWizardPage;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportWizard;

/**
 * Alternative Git clone wizard using auto import framework incubating in e4
 */
public class SmartImportGitWizard extends AbstractGitCloneWizard
		implements IImportWizard, IPageChangedListener {

	private SmartImportWizard easymportWizard;

	private GitSelectRepositoryPage selectRepoPage = new GitSelectRepositoryPage(
			false);

	/**
	 * Constructor
	 */
	public SmartImportGitWizard() {
		super();
		IDialogSettings dialogSettings = super.getDialogSettings();
		if (dialogSettings == null) {
			dialogSettings = org.eclipse.egit.ui.Activator.getDefault()
					.getDialogSettings();
			setDialogSettings(dialogSettings);
		}
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		this.easymportWizard = new SmartImportWizard();
		setWindowTitle(UIText.GitImportWizard_WizardTitle);
	}

	@Override
	protected void addPreClonePages() {
		if (!hasSearchResult()) {
			addPage(selectRepoPage);
		}
	}

	@Override
	protected void addPostClonePages() {
		this.easymportWizard.addPages();
	}

	@Override
	public boolean performFinish() {
		return true; // delegated to EasymportWizard
	}

	@Override
	public boolean canFinish() {
		return getContainer().getCurrentPage()
				.getWizard() == this.easymportWizard
				&& this.easymportWizard.canFinish();
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

	/**
	 * @return the clone destination page
	 */
	public CloneDestinationPage getCloneDestinationPage() {
		return this.cloneDestination;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == selectRepoPage || page == this.cloneDestination) {
			return this.easymportWizard.getPages()[0];
		}
		return super.getNextPage(page);
	}

	/**
	 * @return whether according to current config, the selected repo needs to
	 *         be cloned
	 */
	public boolean needsClone() {
		return this.cloneDestination.cloneSettingsChanged();
	}

	private boolean needToCloneRepository() {
		return SmartImportGitWizard.this.cloneDestination.cloneSettingsChanged();
	}

	private File doClone(final SmartImportRootWizardPage importRootPage) {
		setCallerRunsCloneOperation(true);
		try {
			final GitRepositoryInfo repositoryInfo = currentSearchResult.getGitRepositoryInfo();
			performClone(repositoryInfo);
			getContainer().getShell().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					runCloneOperation(getContainer(), repositoryInfo);
					cloneDestination.saveSettingsForClonedRepo();
					importRootPage.setInitialImportRoot(
							getCloneDestinationPage().getDestinationFile());
				}
			});
		} catch (URISyntaxException e) {
			org.eclipse.egit.ui.Activator.error(UIText.GitImportWizard_errorParsingURI, e);
		} catch (NoRepositoryInfoException e) {
			org.eclipse.egit.ui.Activator.error(UIText.GitImportWizard_noRepositoryInfo, e);
		} catch (Exception e) {
			org.eclipse.egit.ui.Activator.error(e.getMessage(), e);
		}
		return getCloneDestinationPage().getDestinationFile();
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		SmartImportRootWizardPage selectRootPage = (SmartImportRootWizardPage) this.easymportWizard
				.getPages()[0];
		if (event.getSelectedPage() == selectRootPage) {
			Repository existingRepo = selectRepoPage.getRepository();
			if (existingRepo != null) {
				selectRootPage.setInitialImportRoot(existingRepo.getWorkTree());
			} else if (needToCloneRepository()) {
				// asynchronous clone, set root later.
				doClone(selectRootPage);
			}
		}
	}

	@Override
	public void setContainer(IWizardContainer container) {
		if (container instanceof IPageChangeProvider) {
			((IPageChangeProvider) container).addPageChangedListener(this);
		}
		super.setContainer(container);
	}

	@Override
	public void dispose() {
		if (getContainer() instanceof IPageChangeProvider) {
			((IPageChangeProvider) getContainer())
					.removePageChangedListener(this);
		}
		super.dispose();
	}
}
