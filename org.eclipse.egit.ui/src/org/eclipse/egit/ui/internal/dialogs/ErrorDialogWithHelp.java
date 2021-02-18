/*******************************************************************************
 *  Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;

/**
 * An {@link ErrorDialog} with a optional help context.
 */
public class ErrorDialogWithHelp extends ErrorDialog {

	private String helpId;

	/**
	 * Creates a new error dialog to show the given status.
	 *
	 * @param parent
	 *            shell to parent the new dialog off
	 * @param title
	 *            of the dialog
	 * @param message
	 *            for the dialog
	 * @param status
	 *            {@link IStatus} to show
	 * @param helpId
	 *            if not {@code null} the ID of a help context to show via a
	 *            help button
	 */
	public ErrorDialogWithHelp(Shell parent, String title,
			String message, IStatus status, String helpId) {
		super(parent, title, message, status, ~0); // Show all
		this.helpId = helpId;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (helpId != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(shell,
					helpId);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite result = (Composite) super.createDialogArea(parent);
		GridLayout layout = (GridLayout) result.getLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		return result;
	}

	// Code below is based on org.eclipse.jface.dialogs.TrayDialog

	@Override
	protected Control createButtonBar(Composite parent) {
		if (helpId == null) {
			return super.createButtonBar(parent);
		}
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3)
				.applyTo(composite);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.span(2, 1).applyTo(composite);
		composite.setFont(parent.getFont());

		createHelpControl(composite);
		Control buttonSection = super.createButtonBar(composite);
		((GridData) buttonSection
				.getLayoutData()).grabExcessHorizontalSpace = true;
		return composite;
	}

	private void createHelpControl(Composite parent) {
		Image helpImage = JFaceResources.getImage(DLG_IMG_HELP);
		if (helpImage != null) {
			createHelpImageButton(parent, helpImage);
		} else {
			createHelpLink(parent);
		}
	}

	private void createHelpImageButton(Composite parent, Image image) {
		ToolBar toolBar = new ToolBar(parent, SWT.FLAT | SWT.NO_FOCUS);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER)
				.applyTo(toolBar);
		toolBar.setCursor(
				parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		// Use SWT.PUSH instead of SWT.CHECK. This is not a TrayDialog, so
		// we'll just get a link in a pop-up, and then the button state is
		// inconsistent. Moreover, if the help context identified by the ID
		// has no description and only one topic, that help page is opened
		// directly.
		ToolItem button = new ToolItem(toolBar, SWT.PUSH);
		button.setImage(image);
		button.setToolTipText(JFaceResources.getString("helpToolTip")); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				helpPressed();
			}
		});
	}

	private void createHelpLink(Composite parent) {
		Link link = new Link(parent, SWT.WRAP | SWT.NO_FOCUS);
		link.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		link.setText("<a>" + IDialogConstants.HELP_LABEL + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		link.setToolTipText(JFaceResources.getString("helpToolTip")); //$NON-NLS-1$
		link.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				helpPressed();
			}
		});
	}

	private void helpPressed() {
		Control c = getShell().getDisplay().getFocusControl();
		while (c != null) {
			if (c.isListening(SWT.Help)) {
				c.notifyListeners(SWT.Help, new Event());
				break;
			}
			c = c.getParent();
		}
	}

}