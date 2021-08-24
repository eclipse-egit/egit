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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Wizard allowing user to specify all needed data to fetch from another
 * repository - including selection of remote repository, ref specifications,
 * annotated tags fetching strategy.
 * <p>
 * Fetch operation is performed upon successful completion of this wizard.
 */
public class FetchWizard extends Wizard {
	private final Repository localDb;

	private final FetchRefspecPage refSpecPage;

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
		refSpecPage = new FetchRefspecPage(localDb);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_FETCH);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(refSpecPage);
	}

	@Override
	public boolean performFinish() {
		if (refSpecPage.isSaveRequested())
			saveConfig();
		final FetchOperationUI op;
		final RemoteConfig repoSelection = refSpecPage.getRemoteConfig();

		op = new FetchOperationUI(localDb, repoSelection.getURIs().get(0),
				refSpecPage.getRefSpecs(),
				false);

		// UserPasswordCredentials credentials = repoPage.getCredentials();
		// if (credentials != null)
		// op.setCredentialsProvider(new EGitCredentialsProvider(
		// credentials.getUser(), credentials.getPassword()));

		// even if a RemoteConfig is selected, we need to make sure to
		// add the RefSpecs from the RefSpec page into the FetchOperation
		op.setTagOpt(refSpecPage.getTagOpt());
		op.start();

		return true;
	}

	private void saveConfig() {
		final RemoteConfig rc = refSpecPage.getRemoteConfig();
		rc.setFetchRefSpecs(refSpecPage.getRefSpecs());
		rc.setTagOpt(refSpecPage.getTagOpt());
		final StoredConfig config = localDb.getConfig();
		rc.update(config);
		try {
			config.save();
		} catch (final IOException e) {
			ErrorDialog.openError(getShell(), UIText.FetchWizard_cantSaveTitle,
					UIText.FetchWizard_cantSaveMessage,
					new Status(IStatus.WARNING, Activator.PLUGIN_ID,
							e.getMessage(), e));
			// Continue, it's not critical.
		}
	}
}
