/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.egit.ui.internal.dialogs;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 *
 */
public class CommitWarningDialog extends TitleAreaDialog {

	private final int warnings;

	private final int errors;

	private final Map<Integer, List<CommitItem>> items;

	/**
	 * @param parentShell
	 * @param items
	 */
	@SuppressWarnings("boxing")
	public CommitWarningDialog(Shell parentShell,
			Map<Integer, List<CommitItem>> items) {
		super(parentShell);
		this.items = items;
		this.warnings = items.get(IMarker.SEVERITY_WARNING) != null
				? items.get(IMarker.SEVERITY_WARNING).size() : 0;
		this.errors = items.get(IMarker.SEVERITY_ERROR) != null
				? items.get(IMarker.SEVERITY_ERROR).size() : 0;
	}

	@Override
	public void create() {
		super.create();
		setTitle(UIText.CommitWarningDialog_Title);
		setMessage(UIText.CommitWarningDialog_Description);
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(1, false);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(layout);
		processData(container);
		return area;
	}

	@SuppressWarnings("boxing")
	private void processData(Composite container) {
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		Label informationLabel = new Label(container, SWT.NONE);
		if (warnings < 1 && errors >= 1) {
			informationLabel.setText(MessageFormat
					.format(UIText.CommitWarningDialog_Errors, errors));
		} else if (warnings >= 1 && errors < 1) {
			informationLabel.setText(MessageFormat
					.format(UIText.CommitWarningDialog_Warnings, warnings));
		} else {
			informationLabel.setText(MessageFormat.format(
					UIText.CommitWarningDialog_WarningsErrors, warnings,
					errors));
		}
		if (items.get(IMarker.SEVERITY_WARNING) != null) {
			Label warningsLabel = new Label(container, SWT.NONE);
			warningsLabel.setText(UIText.CommitWarningDialog_WarningsLabel);
			TableViewer warningsTableViewer = getTableControl(container);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
					.span(1, 1).grab(false, false)
					.applyTo(warningsTableViewer.getTable());
			warningsTableViewer.setInput(items.get(IMarker.SEVERITY_WARNING));
		}
		if (items.get(IMarker.SEVERITY_ERROR) != null) {
			Label errorsLabel = new Label(container, SWT.NONE);
			errorsLabel.setText(UIText.CommitWarningDialog_ErrorsLabel);
			TableViewer errorsTableViewer = getTableControl(container);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
					.span(1, 1).grab(false, false)
					.applyTo(errorsTableViewer.getTable());
			errorsTableViewer.setInput(items.get(IMarker.SEVERITY_ERROR));
		}
		addProblemsViewLink(container);
	}

	private void addProblemsViewLink(Composite container) {
		Link notificationsLink = new Link(container, SWT.NONE | SWT.WRAP);
		notificationsLink.setLayoutData(GridDataFactory.swtDefaults().span(2, 1)
				.align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());
		notificationsLink.setText(UIText.CommitWarningDialog_ProblemsViewLink);
		notificationsLink.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage()
							.showView(IPageLayout.ID_PROBLEM_VIEW);
				} catch (PartInitException e) {
					Activator.logError(
							UIText.CommitWarningDialog_ProblemsViewError, e);
				}
			}
		});
	}

	private TableViewer getTableControl(Composite parent) {
		TableViewer tableViewer = new TableViewer(parent, SWT.BORDER);
		tableViewer.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				CommitItem item = (CommitItem) element;
				return item.path;
			}

		});
		ColumnViewerToolTipSupport.enableFor(tableViewer);
		tableViewer.setContentProvider(new ArrayContentProvider());
		return tableViewer;
	}

	@Override
	protected boolean isResizable() {
		return false;
	}
}
