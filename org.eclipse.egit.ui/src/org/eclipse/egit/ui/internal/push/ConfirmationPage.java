/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

class ConfirmationPage extends WizardPage {
	static Collection<RemoteRefUpdate> copyUpdates(
			final Collection<RemoteRefUpdate> refUpdates) throws IOException {
		final Collection<RemoteRefUpdate> copy = new ArrayList<>(
				refUpdates.size());
		for (final RemoteRefUpdate rru : refUpdates)
			copy.add(new RemoteRefUpdate(rru, null));
		return copy;
	}

	private final Repository local;

	private RepositorySelection displayedRepoSelection;

	private List<RefSpec> displayedRefSpecs;

	private PushOperationResult confirmedResult;

	private PushResultTable resultPanel;

	private Button requireUnchangedButton;

	private Button showOnlyIfChanged;

	private UserPasswordCredentials credentials;

	private String helpContext = null;

	public ConfirmationPage(final Repository local) {
		super(ConfirmationPage.class.getName());
		this.local = local;

		setTitle(UIText.ConfirmationPage_title);
		setDescription(UIText.ConfirmationPage_description);
	}

	@Override
	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());

		resultPanel = new PushResultTable(panel);
		final Control tableControl = resultPanel.getControl();
		tableControl
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		requireUnchangedButton = new Button(panel, SWT.CHECK);
		requireUnchangedButton
				.setText(UIText.ConfirmationPage_requireUnchangedButton);

		showOnlyIfChanged = new Button(panel, SWT.CHECK);
		showOnlyIfChanged.setText(UIText.ConfirmationPage_showOnlyIfChanged);

		Dialog.applyDialogFont(panel);
		setControl(panel);
	}

	public void setSelection(RepositorySelection repositorySelection, List<RefSpec> specSelection){
		checkPreviousPagesSelections(repositorySelection, specSelection);
		revalidate(repositorySelection, specSelection);
	}

	public void setCredentials(UserPasswordCredentials credentials) {
		this.credentials = credentials;
	}

	/**
	 * Set the ID for context sensitive help
	 *
	 * @param id
	 *            help context
	 */
	public void setHelpContext(String id) {
		helpContext = id;
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpContext);
	}

	boolean isConfirmed() {
		return confirmedResult != null;
	}

	PushOperationResult getConfirmedResult() {
		return confirmedResult;
	}

	boolean isRequireUnchangedSelected() {
		return requireUnchangedButton.getSelection();
	}

	boolean isShowOnlyIfChangedSelected() {
		return showOnlyIfChanged.getSelection();
	}

	private void checkPreviousPagesSelections(RepositorySelection repositorySelection,  List<RefSpec> refSpecs) {
		if (!repositorySelection.equals(displayedRepoSelection)
				|| !refSpecs.equals(displayedRefSpecs)) {
			// Allow user to finish by skipping confirmation...
			setPageComplete(true);
		} else {
			// ... but if user doesn't skip confirmation, allow only when no
			// critical errors occurred
			setPageComplete(confirmedResult != null);
		}
	}

	private void revalidate(RepositorySelection repositorySelection, List<RefSpec> refSpecs) {
		// always update this page
		resultPanel.setData(local, null);
		confirmedResult = null;
		displayedRepoSelection = repositorySelection;
		displayedRefSpecs = refSpecs;
		setErrorMessage(null);
		setPageComplete(false);
		getControl().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				revalidateImpl();
			}
		});
	}

	private void revalidateImpl() {
		if (getControl().isDisposed() || !isCurrentPage())
			return;

		final List<RefSpec> fetchSpecs;
		if (displayedRepoSelection.isConfigSelected())
			fetchSpecs = displayedRepoSelection.getConfig().getFetchRefSpecs();
		else
			fetchSpecs = null;

		final PushOperation operation;
		try {
			final Collection<RemoteRefUpdate> updates = Transport
					.findRemoteRefUpdatesFor(local, displayedRefSpecs,
							fetchSpecs);
			if (updates.isEmpty()) {
				// It can happen only when local refs changed in the mean time.
				setErrorMessage(UIText.ConfirmationPage_errorRefsChangedNoMatch);
				setPageComplete(false);
				return;
			}

			final PushOperationSpecification spec = new PushOperationSpecification();
			for (final URIish uri : displayedRepoSelection.getPushURIs())
				spec.addURIRefUpdates(uri, copyUpdates(updates));
			operation = new PushOperation(local, spec, true,
					GitSettings.getRemoteConnectionTimeout());
			if (credentials != null)
				operation.setCredentialsProvider(new EGitCredentialsProvider(
						credentials.getUser(), credentials.getPassword()));
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					operation.run(monitor);
				}
			});
		} catch (final IOException e) {
			setErrorMessage(NLS.bind(
					UIText.ConfirmationPage_errorCantResolveSpecs, e
							.getMessage()));
			return;
		} catch (final InvocationTargetException e) {
			setErrorMessage(NLS.bind(UIText.ConfirmationPage_errorUnexpected, e
					.getCause().getMessage()));
			return;
		} catch (final InterruptedException e) {
			setErrorMessage(UIText.ConfirmationPage_errorInterrupted);
			setPageComplete(true);
			displayedRefSpecs = null;
			displayedRepoSelection = null;
			return;
		}

		final PushOperationResult result = operation.getOperationResult();
		resultPanel.setData(local, result);
		if (result.isSuccessfulConnectionForAnyURI()) {
			setPageComplete(true);
			confirmedResult = result;
		} else {
			final String message = NLS.bind(
					UIText.ConfirmationPage_cantConnectToAny, result
							.getErrorStringForAllURis());
			setErrorMessage(message);
			ErrorDialog
					.openError(getShell(),
							UIText.ConfirmationPage_cantConnectToAnyTitle,
							null,
							new Status(IStatus.ERROR, Activator.getPluginId(),
									message));
		}
	}
}
