/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2013, Tomasz Zarna <tomasz.zarna@tasktop.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

class PushResultDialog extends TitleAreaDialog {
	private static final int CONFIGURE = 99;

	private final Repository localDb;

	private final PushOperationResult result;

	private final String destinationString;

	private boolean hideConfigure = false;

	private Button toggleButton;

	/**
	 * Shows this dialog asynchronously
	 *
	 * @param repository
	 * @param result
	 * @param sourceString
	 */
	public static void show(final Repository repository,
			final PushOperationResult result, final String sourceString) {
		boolean shouldShow = !result.isSuccessfulConnectionForAnyURI()
				|| Activator.getDefault().getPreferenceStore()
						.getBoolean(UIPreferences.SHOW_PUSH_CONFIRM);

		if (!shouldShow) {
			Activator
					.getDefault()
					.getLog()
					.log(new org.eclipse.core.runtime.Status(IStatus.INFO,
							Activator.getPluginId(), NLS.bind(
									UIText.ResultDialog_label, sourceString)));
			return;
		}

		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				PlatformUI.getWorkbench().getDisplay().asyncExec(
						new Runnable() {
							public void run() {
								Shell shell = PlatformUI.getWorkbench()
										.getActiveWorkbenchWindow().getShell();
								new PushResultDialog(shell, repository, result,
										sourceString).open();
							}
						});
			}
		});
	}

	PushResultDialog(final Shell parentShell, final Repository localDb,
			final PushOperationResult result, final String destinationString) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.localDb = localDb;
		this.result = result;
		this.destinationString = destinationString;
		setHelpAvailable(false);
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		if (!hideConfigure
				&& SimpleConfigurePushDialog.getConfiguredRemote(localDb) != null)
			createButton(parent, CONFIGURE,
					UIText.PushResultDialog_ConfigureButton, false);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (toggleButton != null)
			Activator
					.getDefault()
					.getPreferenceStore()
					.setValue(UIPreferences.SHOW_PUSH_CONFIRM,
							!toggleButton.getSelection());
		if (buttonId == CONFIGURE) {
			super.buttonPressed(IDialogConstants.OK_ID);
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					Dialog dlg = SimpleConfigurePushDialog.getDialog(PlatformUI
							.getWorkbench().getDisplay().getActiveShell(),
							localDb);
					dlg.open();
				}
			});
		}
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);
		String pushErrors = getPushErrors();
		setTitle(NLS.bind(UIText.ResultDialog_label, destinationString));
		if (pushErrors != null && pushErrors.length() > 0)
			setErrorMessage(pushErrors);
		final PushResultTable table = new PushResultTable(composite);
		table.setData(localDb, result);
		final Control tableControl = table.getControl();
		final GridData tableLayout = new GridData(SWT.FILL, SWT.FILL, true,
				true);
		tableLayout.widthHint = 650;
		tableLayout.heightHint = 300;
		tableControl.setLayoutData(tableLayout);

		getShell().setText(
				NLS.bind(UIText.ResultDialog_title, destinationString));
		createToggleButton(composite);
		applyDialogFont(composite);
		return composite;
	}

	private String getPushErrors() {
		StringBuilder messages = new StringBuilder();
		for (URIish uri : result.getURIs()) {
			String errorMessage = result.getErrorMessage(uri);
			if (errorMessage != null && errorMessage.length() > 0) {
				if (messages.length() > 0)
					messages.append(System.getProperty("line.separator")); //$NON-NLS-1$
				messages.append(errorMessage);
			}
		}
		return messages.toString();
	}

	public void showConfigureButton(boolean show) {
		this.hideConfigure = !show;
	}

	protected IDialogSettings getDialogBoundsSettings() {
		return UIUtils.getDialogBoundSettings(getClass());
	}

	private void createToggleButton(Composite parent) {
		boolean toggleState = !Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.SHOW_REBASE_CONFIRM);
		toggleButton = new Button(parent, SWT.CHECK | SWT.LEFT);
		toggleButton.setText(UIText.PushResultDialog_ToggleShowButton);
		toggleButton.setSelection(toggleState);
	}
}
