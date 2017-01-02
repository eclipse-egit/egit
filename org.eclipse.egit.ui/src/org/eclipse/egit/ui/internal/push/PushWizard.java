/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2016, 2017 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
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
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;

/**
 * Wizard allowing user to specify all needed data to push to another repository
 * - including selection of remote repository and refs specifications.
 * <p>
 * Push operation is performed upon successful completion of this wizard.
 */
public class PushWizard extends Wizard {
	private static final String HELP_CONTEXT = "org.eclipse.egit.ui.PushWizard"; //$NON-NLS-1$

	private static String getURIsString(final Collection<URIish> uris) {
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (final URIish uri : uris) {
			if (first)
				first = false;
			else
				sb.append(", "); //$NON-NLS-1$
			sb.append(uri);
		}
		return sb.toString();
	}

	private Repository localDb;

	private final RepositorySelectionPage repoPage;

	private final RefSpecPage refSpecPage;

	private ConfirmationPage confirmPage;

	/**
	 * Create push wizard for specified local repository.
	 *
	 * @param localDb
	 *            repository to push from.
	 * @throws URISyntaxException
	 *             when configuration of this repository contains illegal URIs.
	 */
	public PushWizard(final Repository localDb) throws URISyntaxException {
		this.localDb = localDb;
		final List<RemoteConfig> remotes = RemoteConfig
				.getAllRemoteConfigs(localDb.getConfig());
		repoPage = new RepositorySelectionPage(false, remotes, null);
		repoPage.setHelpContext(HELP_CONTEXT);
		refSpecPage = new RefSpecPage(localDb, true) {
			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					setSelection(repoPage.getSelection());
					setCredentials(repoPage.getCredentials());
				}
				super.setVisible(visible);
			}
		};
		refSpecPage.setHelpContext(HELP_CONTEXT);
		confirmPage = new ConfirmationPage(localDb) {
			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					setSelection(repoPage.getSelection(), refSpecPage
							.getRefSpecs());
					setCredentials(repoPage.getCredentials());
				}
				super.setVisible(visible);
			}
		};
		confirmPage.setHelpContext(HELP_CONTEXT);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_PUSH);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(repoPage);
		addPage(refSpecPage);
		addPage(confirmPage);
	}

	@Override
	public boolean canFinish() {
		if (getContainer().getCurrentPage() == repoPage) {
			RepositorySelection sel = repoPage.getSelection();
			if (sel.isConfigSelected()) {
				RemoteConfig config = sel.getConfig();
				return !config.getPushURIs().isEmpty()
						|| !config.getURIs().isEmpty();
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
				&& refSpecPage.isSaveRequested()) {
			saveRefSpecs();
		}

		if (repoPage.getStoreInSecureStore()) {
			if (!SecureStoreUtils.storeCredentials(repoPage
					.getCredentials(), repoPage.getSelection().getURI()))
				return false;
		}

		final PushOperation operation = createPushOperation(calledFromRepoPage);
		if (operation == null)
			return false;
		UserPasswordCredentials credentials = repoPage.getCredentials();
		if (credentials != null)
			operation.setCredentialsProvider(new EGitCredentialsProvider(
					credentials.getUser(), credentials.getPassword()));
		final PushOperationResult resultToCompare;
		if (confirmPage.isShowOnlyIfChangedSelected()) {
			resultToCompare = confirmPage.getConfirmedResult();
		} else {
			resultToCompare = null;
		}
		final Job job = new PushJob(
				NLS.bind(UIText.PushWizard_jobName,
						getURIsString(operation.getSpecification().getURIs())),
				localDb, operation, resultToCompare,
				getDestinationString(repoPage.getSelection()), true,
				PushMode.UPSTREAM);

		job.setUser(true);
		job.schedule();
		repoPage.saveUriInPrefs();

		return true;
	}

	@Override
	public String getWindowTitle() {
		final IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage == repoPage || currentPage == null)
			return UIText.PushWizard_windowTitleDefault;
		final String destination = getDestinationString(repoPage.getSelection());
		return NLS.bind(UIText.PushWizard_windowTitleWithDestination,
				destination);
	}

	private void saveRefSpecs() {
		final RemoteConfig rc = repoPage.getSelection().getConfig();
		rc.setPushRefSpecs(refSpecPage.getRefSpecs());
		final StoredConfig config = localDb.getConfig();
		rc.update(config);
		try {
			config.save();
		} catch (final IOException e) {
			ErrorDialog.openError(getShell(), UIText.PushWizard_cantSaveTitle,
					UIText.PushWizard_cantSaveMessage, new Status(
							IStatus.WARNING, Activator.getPluginId(), e
									.getMessage(), e));
			// Continue, it's not critical.
		}
	}

	private PushOperation createPushOperation(boolean calledFromRepoPage) {
		try {
			final PushOperationSpecification spec;
			final RemoteConfig config = repoPage.getSelection().getConfig();
			if (calledFromRepoPage) {
				// obtain the push ref specs from the configuration
				// use our own list here, as the config returns a non-modifiable
				// list
				final Collection<RefSpec> pushSpecs = new ArrayList<>();
				pushSpecs.addAll(config.getPushRefSpecs());
				final Collection<RemoteRefUpdate> updates = Transport
						.findRemoteRefUpdatesFor(localDb, pushSpecs,
								config.getFetchRefSpecs());
				spec = new PushOperationSpecification();
				for (final URIish uri : repoPage.getSelection().getPushURIs())
					spec.addURIRefUpdates(uri, ConfirmationPage
							.copyUpdates(updates));
			} else if (confirmPage.isConfirmed()) {
				final PushOperationResult confirmedResult = confirmPage
						.getConfirmedResult();
				spec = confirmedResult.deriveSpecification(confirmPage
						.isRequireUnchangedSelected());
			} else {
				final Collection<RefSpec> fetchSpecs;
				if (config != null)
					fetchSpecs = config.getFetchRefSpecs();
				else
					fetchSpecs = null;

				final Collection<RemoteRefUpdate> updates = Transport
						.findRemoteRefUpdatesFor(localDb, refSpecPage
								.getRefSpecs(), fetchSpecs);
				if (updates.isEmpty()) {
					ErrorDialog.openError(getShell(),
							UIText.PushWizard_missingRefsTitle, null,
							new Status(IStatus.ERROR, Activator.getPluginId(),
									UIText.PushWizard_missingRefsMessage));
					return null;
				}

				spec = new PushOperationSpecification();
				for (final URIish uri : repoPage.getSelection().getPushURIs())
					spec.addURIRefUpdates(uri, ConfirmationPage
							.copyUpdates(updates));
			}
			int timeout = Activator.getDefault().getPreferenceStore().getInt(
					UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			return new PushOperation(localDb, spec, false, timeout);
		} catch (final IOException e) {
			ErrorDialog.openError(getShell(),
					UIText.PushWizard_cantPrepareUpdatesTitle,
					UIText.PushWizard_cantPrepareUpdatesMessage, new Status(
							IStatus.ERROR, Activator.getPluginId(), e
									.getMessage(), e));
			return null;
		}
	}

	static String getDestinationString(RepositorySelection repoSelection) {
		final String destination;
		if (repoSelection.isConfigSelected())
			destination = repoSelection.getConfigName();
		else
			destination = repoSelection.getURI(true).toString();
		return destination;
	}

}
