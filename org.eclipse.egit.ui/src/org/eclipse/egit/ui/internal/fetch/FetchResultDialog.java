/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;

/**
 * Dialog displaying result of fetch operation.
 */
class FetchResultDialog extends Dialog {
	private final Repository localDb;

	private final FetchResult result;

	private final String sourceString;

	FetchResultDialog(final Shell parentShell, final Repository localDb,
			final FetchResult result, final String sourceString) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.localDb = localDb;
		this.result = result;
		this.sourceString = sourceString;
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);

		final Label label = new Label(composite, SWT.NONE);
		final String text;
		if (!result.getTrackingRefUpdates().isEmpty())
			text = NLS.bind(UIText.FetchResultDialog_labelNonEmptyResult,
					sourceString);
		else
			text = NLS.bind(UIText.FetchResultDialog_labelEmptyResult,
					sourceString);
		label.setText(text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final FetchResultTable table = new FetchResultTable(composite);
		table.setData(localDb, result);
		final Control tableControl = table.getControl();
		final GridData tableLayout = new GridData(SWT.FILL, SWT.FILL, true,
				true);
		tableLayout.widthHint = 600;
		tableLayout.heightHint = 300;
		tableControl.setLayoutData(tableLayout);

		getShell().setText(
				NLS.bind(UIText.FetchResultDialog_title, sourceString));
		applyDialogFont(composite);
		return composite;
	}
}
