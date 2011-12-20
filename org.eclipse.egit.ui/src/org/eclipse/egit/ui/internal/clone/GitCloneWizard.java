/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.SecureStoreUtils;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.util.FileUtils;

/**
 * Import Git Repository Wizard. A front end to a git clone operation.
 */
public class GitCloneWizard extends AbstractGitCloneWizard {

	private static final String HELP_CONTEXT = "org.eclipse.egit.ui.GitCloneWizard"; //$NON-NLS-1$

	RepositorySelectionPage cloneSource;

	/**
	 * The default constructor
	 */
	public GitCloneWizard() {
		this(null);
	}

	/**
	 * Construct Clone Wizard
	 *
	 * @param presetURI
	 *            the clone URI to prepopulate the URI field of the clone wizard
	 *            with.
	 */
	public GitCloneWizard(String presetURI) {
		super();
		setWindowTitle(UIText.GitCloneWizard_title);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		setNeedsProgressMonitor(true);
		cloneSource = new RepositorySelectionPage(true, presetURI);
		cloneSource.setHelpContext(HELP_CONTEXT);
		validSource.setHelpContext(HELP_CONTEXT);
		cloneDestination.setHelpContext(HELP_CONTEXT);
		gerritConfiguration = new GerritConfigurationPage() {

			@Override
			public void setVisible(boolean visible) {
				if (visible)
					setSelection(cloneSource.getSelection());
				super.setVisible(visible);
			}
		};
		gerritConfiguration.setHelpContext(HELP_CONTEXT);
	}

	@Override
	protected RepositorySelection getRepositorySelection() {
		return cloneSource.getSelection();
	}

	@Override
	protected UserPasswordCredentials getCredentials() {
		return cloneSource.getCredentials();
	}

	/**
	 * Set whether to show project import options on the destination page
	 *
	 * @param show
	 * @return this wizard
	 */
	public AbstractGitCloneWizard setShowProjectImport(boolean show) {
		cloneDestination.setShowProjectImport(show);
		return this;
	}

	@Override
	public boolean performCancel() {
		if (alreadyClonedInto != null) {
			File test = new File(alreadyClonedInto);
			if (test.exists()
					&& MessageDialog.openQuestion(getShell(),
							UIText.GitCloneWizard_abortingCloneTitle,
							UIText.GitCloneWizard_abortingCloneMsg)) {
				try {
					FileUtils.delete(test, FileUtils.RECURSIVE | FileUtils.RETRY);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return true;
	}

	@Override
	protected void addPreClonePages() {
		addPage(cloneSource);
	}

	@Override
	protected void addPostClonePages() {
		addPage(gerritConfiguration);
	}

	@Override
	public boolean canFinish() {
		return cloneDestination.isPageComplete() &&
			gerritConfiguration.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		try {
			if (cloneSource.getStoreInSecureStore()) {
				if (!SecureStoreUtils.storeCredentials(cloneSource
						.getCredentials(), cloneSource.getSelection().getURI()))
					return false;
			}
			cloneSource.saveUriInPrefs();
			return performClone(cloneSource.getSelection().getURI(), cloneSource.getCredentials());
		} finally {
			setWindowTitle(UIText.GitCloneWizard_title);
		}
	}
}
