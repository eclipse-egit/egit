/*******************************************************************************
 * Copyright (C) 2011, 2017 Mathias Kinzler <mathias.kinzler@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Factored out AbstractConfigureRemoteDialog
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.AbstractConfigureRemoteDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * A simplified wizard for configuring fetch.
 */
public class SimpleConfigureFetchDialog extends AbstractConfigureRemoteDialog {

	/**
	 * @param shell
	 * @param repository
	 * @return the dialog to open, or null
	 */
	public static Dialog getDialog(Shell shell, Repository repository) {
		RemoteConfig configToUse = getConfiguredRemote(repository);
		return new SimpleConfigureFetchDialog(shell, repository, configToUse,
				true);
	}

	/**
	 * @param shell
	 * @param repository
	 * @param remoteName
	 *            the remote name to use
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
		return new SimpleConfigureFetchDialog(shell, repository, configToUse,
				false);
	}

	/**
	 * @param repository
	 * @return the configured remote for the current branch, or the default
	 *         remote; <code>null</code> if a local branch is checked out that
	 *         points to "." as remote
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

		String remoteName;
		if (ObjectId.isId(branch))
			remoteName = Constants.DEFAULT_REMOTE_NAME;
		else
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

		RemoteConfig defaultConfig = null;
		RemoteConfig configuredConfig = null;
		for (RemoteConfig config : allRemotes) {
			if (config.getName().equals(Constants.DEFAULT_REMOTE_NAME))
				defaultConfig = config;
			if (remoteName != null && config.getName().equals(remoteName))
				configuredConfig = config;
		}

		RemoteConfig configToUse = configuredConfig != null ? configuredConfig
				: defaultConfig;
		return configToUse;
	}

	/**
	 * @param shell
	 * @param repository
	 * @param config
	 * @param showBranchInfo
	 *            should be true if this is used for upstream configuration; if
	 *            false, branch information will be hidden in the dialog
	 */
	private SimpleConfigureFetchDialog(Shell shell, Repository repository,
			RemoteConfig config, boolean showBranchInfo) {
		super(shell, repository, config, showBranchInfo, false);
		// Add default fetch ref spec if this is a new remote config
		if (config.getFetchRefSpecs().isEmpty()
				&& !repository.getConfig()
						.getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION)
						.contains(config.getName())) {
			StringBuilder defaultRefSpec = new StringBuilder();
			defaultRefSpec.append('+');
			defaultRefSpec.append(Constants.R_HEADS);
			defaultRefSpec.append('*').append(':');
			defaultRefSpec.append(Constants.R_REMOTES);
			defaultRefSpec.append(config.getName());
			defaultRefSpec.append(RefSpec.WILDCARD_SUFFIX);
			config.addFetchRefSpec(new RefSpec(defaultRefSpec.toString()));
		}
	}

	@Override
	protected RefSpec getNewRefSpec() {
		SimpleFetchRefSpecWizard wiz = new SimpleFetchRefSpecWizard(
				getRepository(), getConfig());
		WizardDialog dlg = new WizardDialog(getShell(), wiz);
		return dlg.open() == Window.OK ? wiz.getSpec() : null;
	}

	@Override
	protected void createOkButton(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.SimpleConfigureFetchDialog_SaveAndFetchButton, true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.SimpleConfigureFetchDialog_WindowTitle);
	}

	@Override
	public void create() {
		super.create();
		setTitle(NLS.bind(UIText.SimpleConfigureFetchDialog_DialogTitle,
				getConfig().getName()));
		setMessage(UIText.SimpleConfigureFetchDialog_DialogMessage);
		updateControls();
	}

	@Override
	protected void updateControls() {
		setErrorMessage(null);
		boolean anyUri = false;
		if (!getConfig().getURIs().isEmpty()) {
			commonUriText
					.setText(getConfig().getURIs().get(0).toPrivateString());
			anyUri = true;
		} else {
			commonUriText.setText(""); //$NON-NLS-1$
		}
		specViewer.setInput(getConfig().getFetchRefSpecs());
		specViewer.getTable().setEnabled(true);

		addRefSpecAction.setEnabled(anyUri);
		addRefSpecAdvancedAction.setEnabled(anyUri);

		if (getConfig().getURIs().isEmpty()) {
			setErrorMessage(
					UIText.AbstractConfigureRemoteDialog_MissingUriMessage);
		} else if (getConfig().getFetchRefSpecs().isEmpty()) {
			setErrorMessage(UIText.SimpleConfigureFetchDialog_MissingMappingMessage);
		}
		boolean anySpec = !getConfig().getFetchRefSpecs().isEmpty();
		getButton(OK).setEnabled(anyUri && anySpec);
		getButton(SAVE_ONLY).setEnabled(anyUri && anySpec);
	}

	@Override
	protected void dryRun(IProgressMonitor monitor) {
		int timeout = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		final FetchOperationUI op = new FetchOperationUI(getRepository(),
				getConfig(), timeout, true);
		try {
			final FetchResult result = op.execute(monitor);
			getShell().getDisplay().asyncExec(new Runnable() {

				@Override
				public void run() {
					FetchResultDialog dlg;
					dlg = new FetchResultDialog(getShell(), getRepository(),
							result, op.getSourceString());
					dlg.showConfigureButton(false);
					dlg.open();
				}
			});
		} catch (CoreException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	@Override
	protected void performOperation() {
		int timeout = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		FetchOperationUI op = new FetchOperationUI(getRepository(), getConfig(),
				timeout, false);
		op.start();
	}
}
