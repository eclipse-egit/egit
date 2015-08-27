/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.egit.ui.internal.dialogs;

import java.text.MessageFormat;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * @author cybuch
 *
 */
public class CommitWarningDialog extends TitleAreaDialog {

	private final static String PROBLEM_VIEW_ID = "org.eclipse.ui.views.ProblemView"; //$NON-NLS-1$

	private final int warnings;

	private final int errors;

	/**
	 * @param parentShell
	 * @param warnings
	 * @param errors
	 */
	public CommitWarningDialog(Shell parentShell, int warnings, int errors) {
		super(parentShell);
		this.warnings = warnings;
		this.errors = errors;
	}

	@Override
	public void create() {
		super.create();
		setTitle(UIText.CommitWarningDialogTitle);
		setMessage(UIText.CommitWarningDialogDescription);
		setHelpAvailable(false);
		super.getButton(OK).setEnabled(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(2, false);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(layout);
		createFeed(container);
		return area;
	}

	@SuppressWarnings("boxing")
	private void createFeed(Composite container) {
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		Label informationLabel = new Label(container, SWT.NONE);
		if (warnings < 1 && errors >= 1) {
			informationLabel.setText(MessageFormat
					.format(UIText.CommitWarningDialogErrors, errors));
		} else if (warnings >= 1 && errors < 1) {
			informationLabel.setText(MessageFormat
					.format(UIText.CommitWarningDialogWarnings, warnings));
		} else {
			informationLabel.setText(MessageFormat.format(
					UIText.CommitWarningDialogWarningsErrors, warnings,
					errors));
		}
		Link notificationsLink = new Link(container, SWT.NONE | SWT.WRAP);
		notificationsLink.setLayoutData(GridDataFactory.swtDefaults().span(2, 1)
				.align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());
		notificationsLink.setText(UIText.CommitWarningDialogProblemsViewLink);
		notificationsLink.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().showView(PROBLEM_VIEW_ID);
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	protected boolean isResizable() {
		return false;
	}

	@Override
	protected void okPressed() {
		super.okPressed();
	}
}
