/*******************************************************************************
 * Copyright (C) 2011, 2017 Mathias Kinzler <mathias.kinzler@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Factored out AbstractConfigureRemoteDialog
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.AbstractConfigureRemoteDialog;
import org.eclipse.egit.ui.internal.repository.SelectUriWizard;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

/**
 * A simplified wizard for configuring push
 */
public class SimpleConfigurePushDialog extends AbstractConfigureRemoteDialog {

	private TableViewer uriViewer;

	/**
	 * @param shell
	 * @param repository
	 * @return the dialog to open, or null
	 */
	public static Dialog getDialog(Shell shell, Repository repository) {
		RemoteConfig configToUse = getConfiguredRemote(repository);
		return new SimpleConfigurePushDialog(shell, repository, configToUse,
				true);
	}

	/**
	 * @param shell
	 * @param repository
	 * @param remoteName
	 *            the remote to use
	 * @return the dialog to open, or null
	 */
	public static Dialog getDialog(Shell shell, Repository repository,
			String remoteName) {
		RemoteConfig configToUse;
		try {
			configToUse = new RemoteConfig(repository.getConfig(), remoteName);
		} catch (URISyntaxException e) {
			Activator.handleError(e.getMessage(), e, true);
			return null;
		}
		return new SimpleConfigurePushDialog(shell, repository, configToUse,
				false);
	}

	/**
	 * @param repository
	 * @return the configured remote for the current branch if any, or null
	 */
	public static RemoteConfig getConfiguredRemote(Repository repository) {
		String branch;
		try {
			branch = repository.getBranch();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
			return null;
		}
		if (branch == null)
			return null;

		String remoteName = null;
		if (!ObjectId.isId(branch))
			remoteName = repository.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branch,
					ConfigConstants.CONFIG_REMOTE_SECTION);

		// check if we find the configured and default Remotes
		List<RemoteConfig> allRemotes;
		try {
			allRemotes = RemoteConfig.getAllRemoteConfigs(repository
					.getConfig());
		} catch (URISyntaxException e) {
			allRemotes = new ArrayList<>();
		}

		RemoteConfig configuredConfig = null;
		RemoteConfig defaultConfig = null;
		for (RemoteConfig config : allRemotes) {
			if (remoteName != null && config.getName().equals(remoteName))
				configuredConfig = config;
			if (config.getName().equals(Constants.DEFAULT_REMOTE_NAME))
				defaultConfig = config;
		}

		if (configuredConfig != null)
			return configuredConfig;

		if (defaultConfig != null)
			if (!defaultConfig.getPushRefSpecs().isEmpty())
				return defaultConfig;

		return null;
	}

	/**
	 *
	 * @param shell
	 * @param repository
	 * @param config
	 * @param showBranchInfo
	 *            should be true if this is used for upstream configuration; if
	 *            false, branch information will be hidden in the dialog
	 */
	private SimpleConfigurePushDialog(Shell shell, Repository repository,
			RemoteConfig config, boolean showBranchInfo) {
		super(shell, repository, config, showBranchInfo, true);
	}

	@Override
	protected Control createAdditionalUriArea(Composite parent) {
		ExpandableComposite pushUriArea = new ExpandableComposite(parent,
				ExpandableComposite.TREE_NODE
						| ExpandableComposite.CLIENT_INDENT);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(pushUriArea);
		pushUriArea.setExpanded(!getConfig().getPushURIs().isEmpty());
		pushUriArea.addExpansionListener(new ExpansionAdapter() {

			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				parent.layout(true, true);
				parent.getShell().pack();
			}
		});
		pushUriArea.setText(UIText.SimpleConfigurePushDialog_PushUrisLabel);
		final Composite pushUriDetails = new Composite(pushUriArea, SWT.NONE);
		pushUriArea.setClient(pushUriDetails);
		pushUriDetails.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(pushUriDetails);
		uriViewer = new TableViewer(pushUriDetails, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true)
				.minSize(SWT.DEFAULT, 30).applyTo(uriViewer.getTable());
		uriViewer.setContentProvider(ArrayContentProvider.getInstance());

		final Composite uriButtonArea = new Composite(pushUriDetails, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(uriButtonArea);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(uriButtonArea);

		Button addUri = new Button(uriButtonArea, SWT.PUSH);
		addUri.setText(UIText.SimpleConfigurePushDialog_AddPushUriButton);
		GridDataFactory.fillDefaults().applyTo(addUri);
		addUri.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SelectUriWizard wiz = new SelectUriWizard(false);
				if (new WizardDialog(getShell(), wiz).open() == Window.OK) {
					getConfig().addPushURI(wiz.getUri());
					updateControls();
				}
			}
		});

		final Button changeUri = new Button(uriButtonArea, SWT.PUSH);
		changeUri.setText(UIText.SimpleConfigurePushDialog_ChangePushUriButton);
		GridDataFactory.fillDefaults().applyTo(changeUri);
		changeUri.setEnabled(false);
		changeUri.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				URIish uri = (URIish) ((IStructuredSelection) uriViewer
						.getSelection()).getFirstElement();
				SelectUriWizard wiz = new SelectUriWizard(false, uri
						.toPrivateString());
				if (new WizardDialog(getShell(), wiz).open() == Window.OK) {
					getConfig().removePushURI(uri);
					getConfig().addPushURI(wiz.getUri());
					updateControls();
				}
			}
		});
		final Button deleteUri = new Button(uriButtonArea, SWT.PUSH);
		deleteUri.setText(UIText.SimpleConfigurePushDialog_DeletePushUriButton);
		GridDataFactory.fillDefaults().applyTo(deleteUri);
		deleteUri.setEnabled(false);
		deleteUri.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				URIish uri = (URIish) ((IStructuredSelection) uriViewer
						.getSelection()).getFirstElement();
				getConfig().removePushURI(uri);
				updateControls();
			}
		});

		uriViewer.addSelectionChangedListener(event -> {
			deleteUri.setEnabled(!uriViewer.getSelection().isEmpty());
			changeUri.setEnabled(
					((IStructuredSelection) uriViewer.getSelection())
							.size() == 1);
		});
		return pushUriArea;
	}

	@Override
	protected RefSpec getNewRefSpec() {
		RefSpecDialog dlg = new RefSpecDialog(getShell(), getRepository(),
				getConfig(), true);
		return dlg.open() == Window.OK ? dlg.getSpec() : null;
	}

	@Override
	protected void createOkButton(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.SimpleConfigurePushDialog_SaveAndPushButton, true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.SimpleConfigurePushDialog_WindowTitle);
	}

	@Override
	public void create() {
		super.create();
		setTitle(NLS.bind(UIText.SimpleConfigurePushDialog_DialogTitle,
				getConfig().getName()));
		setMessage(UIText.SimpleConfigurePushDialog_DialogMessage);
		updateControls();
	}

	@Override
	protected void updateControls() {
		boolean anyFetchUri = !getConfig().getURIs().isEmpty();
		boolean anyPushUri = !getConfig().getPushURIs().isEmpty();
		boolean anyUri = anyFetchUri || anyPushUri;
		setErrorMessage(null);
		if (!anyUri) {
			setErrorMessage(UIText.AbstractConfigureRemoteDialog_MissingUriMessage);
		}
		if (anyFetchUri) {
			commonUriText
					.setText(getConfig().getURIs().get(0).toPrivateString());
		} else {
			commonUriText.setText(""); //$NON-NLS-1$
		}
		uriViewer.getTable().setEnabled(anyPushUri);
		if (anyPushUri) {
			uriViewer.setInput(getConfig().getPushURIs());
		} else if (anyFetchUri) {
			uriViewer.setInput(new String[] { NLS.bind(
					UIText.SimpleConfigurePushDialog_UseUriForPushUriMessage,
					commonUriText.getText()) });
		} else {
			uriViewer.setInput(null);
		}
		if (getConfig().getPushRefSpecs().isEmpty()) {
			specViewer.setInput(new String[] { UIText.SimpleConfigurePushDialog_DefaultPushNoRefspec});
		} else {
			specViewer.setInput(getConfig().getPushRefSpecs());
		}
		specViewer.getTable()
				.setEnabled(!getConfig().getPushRefSpecs().isEmpty());

		addRefSpecAction.setEnabled(anyUri);
		addRefSpecAdvancedAction.setEnabled(anyUri);
		changeCommonUriAction.setEnabled(!anyPushUri);
		deleteCommonUriAction.setEnabled(!anyPushUri && anyUri);
		commonUriText.setEnabled(!anyPushUri);

		getButton(OK).setEnabled(anyUri);
		getButton(DRY_RUN).setEnabled(anyUri);
		getButton(SAVE_ONLY).setEnabled(anyUri);
	}

	@Override
	protected void dryRun(IProgressMonitor monitor) {
		PushOperationUI op = new PushOperationUI(getRepository(),
				getConfig(), true);
		try {
			final PushOperationResult result = op.execute(monitor);
			getShell().getDisplay().asyncExec(() -> {
				PushResultDialog dlg = new PushResultDialog(getShell(),
						getRepository(), result, op.getDestinationString(),
						true, PushMode.UPSTREAM);
				dlg.showConfigureButton(false);
				dlg.open();
			});
		} catch (CoreException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	@Override
	protected void performOperation() {
		PushOperationUI op = new PushOperationUI(getRepository(),
				getConfig().getName(), false);
		op.start();
	}
}
