/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler mathias.kinzler@sap.com>
 * Copyright (C) 2015, Christian Georgi <christian.georgi@sap.com>
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.TitleAndImageDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

class PushResultDialog extends TitleAndImageDialog {
	private static final int CONFIGURE = 99;

	private final Repository localDb;

	private final PushOperationResult result;

	private final String destinationString;

	private boolean hideConfigure = false;

	PushResultDialog(final Shell parentShell, final Repository localDb,
			final PushOperationResult result, final String destinationString,
			boolean modal, @NonNull PushMode pushMode) {
		super(parentShell, pushMode == PushMode.UPSTREAM ? UIIcons.WIZBAN_PUSH
				: UIIcons.WIZBAN_PUSH_GERRIT);
		int shellStyle = getShellStyle() | SWT.RESIZE;
		if (!modal) {
			shellStyle &= ~SWT.APPLICATION_MODAL;
			setBlockOnOpen(false);
		}
		setShellStyle(shellStyle);
		this.localDb = localDb;
		this.result = result;
		this.destinationString = destinationString;
		setHelpAvailable(false);
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		if (!hideConfigure && SimpleConfigurePushDialog
				.getConfiguredRemote(localDb) != null) {
			createButton(parent, CONFIGURE,
					UIText.PushResultDialog_ConfigureButton, false);
		}
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.CLOSE_LABEL, true);
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
				if (messages.length() > 0) {
					messages.append(System.lineSeparator());
				}
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
