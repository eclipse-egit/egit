/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler mathias.kinzler@sap.com>
 * Copyright (C) 2015, Christian Georgi <christian.georgi@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.core.op.PushOperationResult;
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

	/**
	 * Shows this dialog asynchronously
	 *
	 * @param repository
	 * @param result
	 * @param sourceString
	 * @param showConfigureButton
	 *            whether to show the "Configure..." button in the result dialog
	 *            or not
	 * @param modal
	 *            true to have application modal style
	 */
	public static void show(final Repository repository,
			final PushOperationResult result, final String sourceString,
			final boolean showConfigureButton, final boolean modal) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench()
						.getModalDialogShellProvider().getShell();
				PushResultDialog dialog = new PushResultDialog(shell,
						repository, result, sourceString, modal);
				dialog.showConfigureButton(showConfigureButton);
				dialog.open();
			}
		});
	}

	PushResultDialog(final Shell parentShell, final Repository localDb,
			final PushOperationResult result, final String destinationString,
			boolean modal) {
		super(parentShell);
		int shellStyle = getShellStyle() | SWT.RESIZE;
		if (!modal) {
			shellStyle &= ~SWT.APPLICATION_MODAL;
		}
		setShellStyle(shellStyle);
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
		if (buttonId == CONFIGURE) {
			super.buttonPressed(IDialogConstants.OK_ID);
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					Dialog dlg = SimpleConfigurePushDialog.getDialog(
							PlatformUI.getWorkbench()
									.getModalDialogShellProvider().getShell(),
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
		String title;
		if (pushErrors != null && pushErrors.length() > 0) {
			setErrorMessage(pushErrors);
			title = NLS.bind(UIText.PushResultDialog_label_failed,
					destinationString);
		} else
			title = NLS.bind(UIText.PushResultDialog_label, destinationString);
		setTitle(title);
		final PushResultTable table = new PushResultTable(composite,
				getDialogBoundsSettings());
		table.setData(localDb, result);
		final Control tableControl = table.getControl();
		final GridData tableLayout = new GridData(SWT.FILL, SWT.FILL, true,
				true);
		tableLayout.widthHint = 650;
		tableLayout.heightHint = 300;
		tableControl.setLayoutData(tableLayout);

		getShell().setText(
				NLS.bind(UIText.PushResultDialog_title, destinationString));
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

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return UIUtils.getDialogBoundSettings(getClass());
	}
}
