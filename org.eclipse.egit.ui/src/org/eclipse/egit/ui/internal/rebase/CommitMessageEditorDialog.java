/*******************************************************************************
 * Copyright (c) 2013, 2019 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Matthias Sohn (SAP AG) - initial implementation
 *    Simon Muschel <smuschel@gmx.de> - Bug 451817
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Dialog for editing a commit message
 */
public class CommitMessageEditorDialog extends TitleAreaDialog {

	private String commitMessage;

	private SpellcheckableMessageArea messageArea;

	private FormToolkit toolkit;

	private static final String DIALOG_SETTINGS_SECTION_NAME = Activator
			.getPluginId() + ".COMMIT_MESSAGE_EDITOR_DIALOG_SECTION"; //$NON-NLS-1$

	private String title;

	private String okButtonLabel;

	private String cancelButtonLabel;

	/**
	 * @param parentShell
	 *            the parent SWT shell
	 * @param commitMessage
	 *            the commit message to be edited
	 */
	public CommitMessageEditorDialog(Shell parentShell,
			String commitMessage) {
		this(parentShell, commitMessage,
				UIText.CommitMessageEditorDialog_EditCommitMessageTitle);
	}

	/**
	 * @param parentShell
	 *            the parent SWT shell
	 * @param commitMessage
	 *            the commit message to be edited
	 * @param title
	 *            the dialog title
	 */
	public CommitMessageEditorDialog(Shell parentShell, String commitMessage,
			String title) {
		this(parentShell, commitMessage,
				UIText.CommitMessageEditorDialog_OkButton,
				IDialogConstants.CANCEL_LABEL);
		this.title = title;
	}

	/**
	 * @param parentShell
	 *            the parent SWT shell
	 * @param commitMessage
	 *            the commit message to be edited
	 * @param okButtonLabel
	 *            the label for the Ok button
	 * @param cancelButtonLabel
	 *            the label for the Cancel button
	 */
	public CommitMessageEditorDialog(Shell parentShell, String commitMessage,
			String okButtonLabel, String cancelButtonLabel) {
		super(parentShell);
		this.commitMessage = commitMessage;
		this.title = UIText.CommitMessageEditorDialog_EditCommitMessageTitle;
		this.okButtonLabel = okButtonLabel;
		this.cancelButtonLabel = cancelButtonLabel;
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(newShellStyle | SWT.RESIZE | SWT.MAX);
	}

	@Override
	protected Control createContents(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				CommitMessageEditorDialog.this.commitMessage = messageArea
						.getCommitMessage();
				toolkit.dispose();
			}
		});
		return super.createContents(parent);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		composite.setLayout(gridLayout);

		setTitle(UIText.RebaseInteractiveHandler_EditMessageDialogTitle);
		setMessage(UIText.RebaseInteractiveHandler_EditMessageDialogText);

		messageArea = new SpellcheckableMessageArea(composite, commitMessage);
		messageArea.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		Point size = messageArea.getTextWidget().getSize();
		int minHeight = messageArea.getTextWidget().getLineHeight() * 3;
		messageArea.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.hint(size).minSize(size.x, minHeight)
				.align(SWT.FILL, SWT.FILL).create());
		messageArea.setFocus();

		return composite;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings
				.getSection(DIALOG_SETTINGS_SECTION_NAME);
		if (section == null)
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION_NAME);
		return section;
	}

	/**
	 * @return the commit message
	 */
	public String getCommitMessage() {
		return this.commitMessage;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				okButtonLabel,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				cancelButtonLabel, false);
	}
}
