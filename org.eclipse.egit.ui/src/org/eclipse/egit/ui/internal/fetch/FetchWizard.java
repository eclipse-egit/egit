/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.SecureStoreUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RefSpecPage;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;

/**
 * Wizard allowing user to specify all needed data to fetch from another
 * repository - including selection of remote repository, ref specifications,
 * annotated tags fetching strategy.
 * <p>
 * Fetch operation is performed upon successful completion of this wizard.
 */
public class FetchWizard extends Wizard {
	private final Repository localDb;

	private final RepositorySelectionPage repoPage;

	private final RefSpecPage refSpecPage;

	/**
	 * Create wizard for provided local repository.
	 *
	 * @param localDb
	 *            local repository to fetch to.
	 * @throws URISyntaxException
	 *             when configuration of this repository contains illegal URIs.
	 */
	public FetchWizard(final Repository localDb) throws URISyntaxException {
		this.localDb = localDb;
		final List<RemoteConfig> remotes = RemoteConfig
				.getAllRemoteConfigs(localDb.getConfig());
		repoPage = new RepositorySelectionPage(true, remotes, null);
		refSpecPage = new RefSpecPage(localDb, false) {
			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					setSelection(repoPage.getSelection());
					setCredentials(repoPage.getCredentials());
				}
				super.setVisible(visible);
			}
		};
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_FETCH);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(repoPage);
		addPage(refSpecPage);
	}

	@Override
	public boolean canFinish() {
		if (getContainer().getCurrentPage() == repoPage) {
			RepositorySelection sel = repoPage.getSelection();
			if (sel.isConfigSelected()) {
				RemoteConfig config = sel.getConfig();
				return !config.getURIs().isEmpty()
						&& !config.getFetchRefSpecs().isEmpty();
			}
		}
		return super.canFinish();
	}

	@Override
	public boolean performFinish() {
		boolean calledFromRepoPage = false;
		if (getContainer().getCurrentPage()==repoPage)
			calledFromRepoPage = true;
		if (repoPage.getSelection().isConfigSelected()
				&& refSpecPage.isSaveRequested())
			saveConfig();
		if (repoPage.getStoreInSecureStore()) {
			if (!SecureStoreUtils.storeCredentials(repoPage.getCredentials(),
					repoPage.getSelection().getURI()))
				return false;
		}

		final FetchOperationUI op;
		int timeout = Activator.getDefault().getPreferenceStore().getInt(
				UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		final RepositorySelection repoSelection = repoPage.getSelection();

		if (calledFromRepoPage)
			op = new FetchOperationUI(localDb, repoSelection.getConfig(),
					timeout, false);
		else if (repoSelection.isConfigSelected())
			op = new FetchOperationUI(localDb, repoSelection.getConfig()
					.getURIs().get(0), refSpecPage.getRefSpecs(), timeout,
					false);
		else
			op = new FetchOperationUI(localDb, repoSelection.getURI(false),
					refSpecPage.getRefSpecs(), timeout, false);

		UserPasswordCredentials credentials = repoPage.getCredentials();
		if (credentials != null)
			op.setCredentialsProvider(new EGitCredentialsProvider(
					credentials.getUser(), credentials.getPassword()));

		// even if a RemoteConfig is selected, we need to make sure to
		// add the RefSpecs from the RefSpec page into the FetchOperation
		if (!calledFromRepoPage)
			op.setTagOpt(refSpecPage.getTagOpt());
		op.start();

		repoPage.saveUriInPrefs();

		return true;
	}

	@Override
	public String getWindowTitle() {
		final IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage == repoPage || currentPage == null)
			return UIText.FetchWizard_windowTitleDefault;
		return NLS.bind(UIText.FetchWizard_windowTitleWithSource,
				getSourceString());
	}

	private void saveConfig() {
		final RemoteConfig rc = repoPage.getSelection().getConfig();
		rc.setFetchRefSpecs(refSpecPage.getRefSpecs());
		rc.setTagOpt(refSpecPage.getTagOpt());
		final StoredConfig config = localDb.getConfig();
		rc.update(config);
		try {
			config.save();
		} catch (final IOException e) {
			ErrorDialog.openError(getShell(), UIText.FetchWizard_cantSaveTitle,
					UIText.FetchWizard_cantSaveMessage, new Status(
							IStatus.WARNING, Activator.getPluginId(), e
									.getMessage(), e));
			// Continue, it's not critical.
		}
	}

	private String getSourceString() {
		final RepositorySelection repoSelection = repoPage.getSelection();
		if (repoSelection.isConfigSelected())
			return repoSelection.getConfigName();
		return repoSelection.getURI(false).toString();
	}
}
