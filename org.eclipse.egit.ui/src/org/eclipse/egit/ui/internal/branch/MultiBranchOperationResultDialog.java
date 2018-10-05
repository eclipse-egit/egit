/*******************************************************************************
 * Copyright (C) 2018, Luís Copetti <lhcopetti@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.LabelColumnComparator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Presents the result of a checkout operation for multiple repositories
 * <p>
 */
public class MultiBranchOperationResultDialog extends TitleAreaDialog {

	private final Map<Repository, CheckoutResult> results = new LinkedHashMap<>();

	private TableViewer tv;

	private final RepositoryUtil utils = Activator.getDefault()
			.getRepositoryUtil();

	private EnumMap<CheckoutResult.Status, String> resultMessages;

	/**
	 * @param parentShell
	 * @param results
	 */
	protected MultiBranchOperationResultDialog(Shell parentShell,
			Map<Repository, CheckoutResult> results) {
		super(parentShell);
		setShellStyle(
				getShellStyle() & ~SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
		setBlockOnOpen(false);
		this.results.putAll(results);

		this.initializeResultMessages();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		tv = new TableViewer(main, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		tv.setContentProvider(ArrayContentProvider.getInstance());
		TableColumnLayout layout = new TableColumnLayout();
		main.setLayout(layout);

		Table table = tv.getTable();
		TableViewerColumn tc = new TableViewerColumn(tv, SWT.NONE);
		TableColumn col = tc.getColumn();
		tc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Entry<Repository, CheckoutResult> item = (Entry<Repository, CheckoutResult>) element;
				return utils.getRepositoryName(item.getKey());
			}
		});
		col.setText(
				UIText.MultiBranchOperationResultDialog_RepositoryColumnHeader);
		layout.setColumnData(col, new ColumnWeightData(200, 200));
		createComparator(col, 0);

		// update status
		tc = new TableViewerColumn(tv, SWT.NONE);
		col = tc.getColumn();
		tc.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public Image getImage(Object element) {
				@SuppressWarnings("unchecked")
				Entry<Repository, CheckoutResult> item = (Entry<Repository, CheckoutResult>) element;

				if (item.getValue().getStatus() == Status.OK) {
					return null;
				}
				return PlatformUI.getWorkbench().getSharedImages()
						.getImage(ISharedImages.IMG_ELCL_STOP);
			}

			@Override
			public String getText(Object element) {
				@SuppressWarnings("unchecked")
				Entry<Repository, CheckoutResult> item = (Entry<Repository, CheckoutResult>) element;

				CheckoutResult.Status status = item.getValue().getStatus();
				return getMessageForStatus(status);
			}
		});
		col.setText(
				UIText.MultiBranchOperationResultDialog_CheckoutStatusColumnHeader);
		layout.setColumnData(col, new ColumnWeightData(200, 450));
		createComparator(col, 1);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		tv.setInput(results.entrySet());

		int linesToShow = Math.min(Math.max(results.size(), 5), 15);
		int height = table.getItemHeight() * linesToShow;

		GridDataFactory.fillDefaults().grab(true, true)
				.minSize(SWT.DEFAULT, height).applyTo(main);

		setTitle(UIText.MultiBranchOperationResultDialog_DialogTitle);
		setErrorMessage(
				UIText.MultiBranchOperationResultDialog_DialogErrorMessage);
		return main;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.MultiBranchOperationResultDialog_WindowTitle);
	}

	private LabelColumnComparator createComparator(TableColumn column,
			int columnIndex) {
		return new LabelColumnComparator(this.tv, column, columnIndex);
	}

	private void initializeResultMessages() {
		this.resultMessages = new EnumMap<>(CheckoutResult.Status.class);

		this.resultMessages.put(Status.OK,
				UIText.MultiBranchOperationResultDialog_CheckoutResultOK);
		this.resultMessages.put(Status.CONFLICTS,
				UIText.MultiBranchOperationResultDialog_CheckoutResultConflicts);
		this.resultMessages.put(Status.NOT_TRIED,
				UIText.MultiBranchOperationResultDialog_CheckoutResultNotTried);
		this.resultMessages.put(Status.NONDELETED,
				UIText.MultiBranchOperationResultDialog_CheckoutResultNonDeleted);
		this.resultMessages.put(Status.ERROR,
				UIText.MultiBranchOperationResultDialog_CheckoutResultError);
	}

	private String getMessageForStatus(CheckoutResult.Status status) {
		return this.resultMessages.getOrDefault(status, ""); //$NON-NLS-1$
	}
}