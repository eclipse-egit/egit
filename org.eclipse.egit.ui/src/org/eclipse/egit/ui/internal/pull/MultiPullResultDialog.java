/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
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
 * Presents the result of a pull for multiple repositories
 * <p>
 * Each line corresponds to one Repository; there is either a {@link PullResult}
 * or an {@link IStatus} corresponding to each Repository.
 *
 * The user can select a line and check the {@link PullResult} for this line by
 * hitting a "Details" button (or double-clicking the line); if there is no
 * {@link PullResult} for this line, only the text of the {@link IStatus} can be
 * inspected (double-clicking does nothing, Details button is inactive).
 */
public class MultiPullResultDialog extends Dialog {
	private static final int DETAIL_BUTTON = 99;

	// the value is either a PullResult or an IStatus
	private final Map<Repository, Object> results = new HashMap<Repository, Object>();

	private TableViewer tv;

	private final static class LabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {
		private final RepositoryUtil utils = Activator.getDefault()
				.getRepositoryUtil();

		@SuppressWarnings("unchecked")
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 3)
				return null;
			Entry<Repository, Object> item = (Entry<Repository, Object>) element;
			Object resultOrError = item.getValue();
			if (resultOrError instanceof IStatus)
				return PlatformUI.getWorkbench().getSharedImages().getImage(
						ISharedImages.IMG_ELCL_STOP);

			PullResult res = (PullResult) item.getValue();
			boolean success = res.isSuccessful();
			if (!success)
				return PlatformUI.getWorkbench().getSharedImages().getImage(
						ISharedImages.IMG_ELCL_STOP);
			return null;
		}

		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int columnIndex) {
			Entry<Repository, Object> item = (Entry<Repository, Object>) element;
			switch (columnIndex) {
			case 0:
				return utils.getRepositoryName(item.getKey());
			case 1: {
				if (item.getValue() instanceof IStatus)
					return UIText.MultiPullResultDialog_UnknownStatus;
				PullResult pullRes = (PullResult) item.getValue();
				if (pullRes.getFetchResult() == null)
					return UIText.MultiPullResultDialog_NothingFetchedStatus;
				else if (pullRes.getFetchResult().getTrackingRefUpdates()
						.isEmpty())
					return UIText.MultiPullResultDialog_NothingUpdatedStatus;
				else {
					int updated = pullRes.getFetchResult()
							.getTrackingRefUpdates().size();
					return NLS.bind(
							UIText.MultiPullResultDialog_UpdatedMessage,
							Integer.valueOf(updated));
				}
			}
			case 2: {
				if (item.getValue() instanceof IStatus)
					return UIText.MultiPullResultDialog_UnknownStatus;
				PullResult pullRes = (PullResult) item.getValue();
				if (pullRes.getMergeResult() != null) {
					return NLS.bind(
							UIText.MultiPullResultDialog_MergeResultMessage,
							pullRes.getMergeResult().getMergeStatus().name());
				} else if (pullRes.getRebaseResult() != null) {
					RebaseResult res = pullRes.getRebaseResult();
					return NLS.bind(
							UIText.MultiPullResultDialog_RebaseResultMessage,
							res.getStatus().name());
				} else {
					return UIText.MultiPullResultDialog_NothingUpdatedStatus;
				}
			}
			case 3:
				if (item.getValue() instanceof IStatus) {
					IStatus status = (IStatus) item.getValue();
					return status.getMessage();
				}
				PullResult res = (PullResult) item.getValue();
				if (res.isSuccessful())
					return UIText.MultiPullResultDialog_OkStatus;
				else
					return UIText.MultiPullResultDialog_FailedStatus;
			default:
				return null;
			}
		}
	}

	/**
	 * @param parentShell
	 * @param results
	 *            maps {@link Repository}s to either {@link PullResult} or
	 *            {@link IStatus}
	 */
	protected MultiPullResultDialog(Shell parentShell,
			Map<Repository, Object> results) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.results.putAll(results);
	}

	@Override
	public void create() {
		super.create();
		getButton(DETAIL_BUTTON).setEnabled(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		tv = new TableViewer(main, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
		tv.setContentProvider(ArrayContentProvider.getInstance());

		tv.addSelectionChangedListener(new ISelectionChangedListener() {
			@SuppressWarnings("unchecked")
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event
						.getSelection();
				boolean enabled = !sel.isEmpty();
				if (enabled) {
					Entry<Repository, Object> entry = (Entry<Repository, Object>) sel
							.getFirstElement();
					enabled = entry.getValue() instanceof PullResult;
				}
				getButton(DETAIL_BUTTON).setEnabled(enabled);
			}
		});

		tv.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				buttonPressed(DETAIL_BUTTON);
			}
		});
		tv.setLabelProvider(new LabelProvider());
		Table table = tv.getTable();
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		// repository
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setWidth(200);
		col.setText(UIText.MultiPullResultDialog_RepositoryColumnHeader);
		// fetch status
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(200);
		col.setText(UIText.MultiPullResultDialog_FetchStatusColumnHeader);
		// update status
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(200);
		col.setText(UIText.MultiPullResultDialog_UpdateStatusColumnHeader);
		// overall status
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(150);
		col.setText(UIText.MultiPullResultDialog_OverallStatusColumnHeader);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		tv.setInput(results.entrySet());
		return main;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, DETAIL_BUTTON,
				UIText.MultiPullResultDialog_DetailsButton, false);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == DETAIL_BUTTON) {
			IStructuredSelection sel = (IStructuredSelection) tv.getSelection();
			Entry<Repository, Object> item = (Entry<Repository, Object>) sel
					.getFirstElement();
			if (item.getValue() instanceof PullResult) {
				PullResultDialog dialog = new PullResultDialog(getShell(), item
						.getKey(), (PullResult) item.getValue());
				dialog.open();
			}
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.MultiPullResultDialog_WindowTitle);
	}
}
