/*******************************************************************************
 * Copyright (C) 2015, Peter Karena <peter.karena@arcor.de>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.internal.UIText;
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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Presents the result of a push for multiple repositories
 * <p>
 * Each line corresponds to one Repository; there is either a
 * {@link PushOperationResult} corresponding to each Repository.
 *
 * The user can select a line and check the {@link PushOperationResult} for this
 * line by hitting a "Details" button (or double-clicking the line); if there is
 * no {@link PushOperationResult} for this line, only the text of the
 * {@link IStatus} can be inspected (double-clicking does nothing, Details
 * button is inactive).
 */
public class MultiPushResultDialog extends Dialog {
	private static final int DETAIL_BUTTON = 99;

	// the value is either a PushOperationResult or an IStatus
	private final Map<Repository, Object> results = new LinkedHashMap<Repository, Object>();

	private TableViewer tv;

	private final static class LabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {
		private final RepositoryUtil utils = Activator.getDefault()
				.getRepositoryUtil();

		@SuppressWarnings("unchecked")
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 2)
				return null;

			Entry<Repository, Object> item = (Entry<Repository, Object>) element;
			Object resultOrError = item.getValue();
			if (resultOrError instanceof IStatus)
				return PlatformUI.getWorkbench().getSharedImages()
						.getImage(ISharedImages.IMG_ELCL_STOP);

			PushOperationResult res = (PushOperationResult) item.getValue();
			boolean success = res.isSuccessfulConnectionForAllURI();
			if (!success)
				return PlatformUI.getWorkbench().getSharedImages()
						.getImage(ISharedImages.IMG_ELCL_STOP);
			return null;
		}

		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int columnIndex) {
			Entry<Repository, Object> item = (Entry<Repository, Object>) element;

			switch (columnIndex) {
			case 0:
				return utils.getRepositoryName(item.getKey());
			case 1: {
				if (item.getValue() instanceof IStatus) {
					IStatus status = (IStatus)item.getValue();
					return status.getMessage();
				}
				PushOperationResult pushOperationResult = (PushOperationResult) item
						.getValue();

				if (!pushOperationResult.isSuccessfulConnectionForAllURI()) {
					return pushOperationResult.getErrorStringForAllURis();
				}
				return UIText.MultiPushResultDialog_NoErrors;
			}
			case 2: {
				if (item.getValue() instanceof IStatus) {
					return UIText.MultiPushResultDialog_FailedStatus;
				}

				PushOperationResult pushOperationResult = (PushOperationResult) item
						.getValue();

				if (pushOperationResult.isSuccessfulConnectionForAllURI()) {
					return UIText.MultiPushResultDialog_OkStatus;
				}
				return UIText.MultiPushResultDialog_FailedStatus;
			}
			default:
				return null;
			}
		}
	}

	/**
	 * @param parentShell
	 * @param results
	 *            maps {@link Repository}s to either {@link PushResult}
	 */
	public MultiPushResultDialog(Shell parentShell,
			Map<Repository, Object> results) {
		super(parentShell);
		setShellStyle(getShellStyle() & ~SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
		setBlockOnOpen(true);
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
		tv = new TableViewer(main, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		tv.setContentProvider(ArrayContentProvider.getInstance());

		tv.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event
						.getSelection();
				boolean enabled = false;
				for (Entry<Repository, Object> entry : (List<Entry<Repository, Object>>) sel
						.toList())
					enabled |= entry.getValue() instanceof PushOperationResult;
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
		int linesToShow = Math.min(Math.max(results.size(), 5), 15);
		int heightHint = table.getItemHeight() * linesToShow;
		GridDataFactory.fillDefaults().grab(true, true).hint(800, heightHint)
				.applyTo(table);
		// repository
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setWidth(200);
		col.setText(UIText.MultiPushResultDialog_RepositoryColumnHeader);

		// errors occurred
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(200);
		col.setText(UIText.MultiPushResultDialog_ErrorsOccuredColumnHeader);

		// overall status
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(150);
		col.setText(UIText.MultiPushResultDialog_OverallStatusColumnHeader);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		tv.setInput(results.entrySet());
		return main;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, DETAIL_BUTTON,
				UIText.MultiPushResultDialog_DetailsButton, false);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == DETAIL_BUTTON) {
			detailButtonPressed();
		}
		super.buttonPressed(buttonId);
	}

	private void detailButtonPressed() {
		final Shell shell = getShell();
		Rectangle trim = shell.computeTrim(0, 0, 0, 0);
		int xOffset = 0;
		int xDelta = -trim.x + 3;
		int yOffset = 0;
		int yDelta = -trim.y - 3;

		final LinkedList<PushResultDialog> dialogs = new LinkedList<PushResultDialog>();
		IStructuredSelection sel = (IStructuredSelection) tv.getSelection();
		for (Entry<Repository, Object> item : (List<Entry<Repository, Object>>) sel
				.toList()) {

			if (item.getValue() instanceof PushOperationResult) {
				final int x = xOffset;
				final int y = yOffset;
				xOffset += xDelta;
				yOffset += yDelta;

				Repository repository = item.getKey();

				RemoteConfig config = SimpleConfigurePushDialog
						.getConfiguredRemote(repository);

				String destinationString = NLS.bind(
						"{0} - {1}", repository.getDirectory() //$NON-NLS-1$
								.getParentFile().getName(), config.getName());

				final PushResultDialog dialog = new PushResultDialog(shell,
						repository, (PushOperationResult) item.getValue(),
						destinationString) {
					private Point initialLocation;

					@Override
					protected Point getInitialLocation(Point initialSize) {
						initialLocation = super.getInitialLocation(initialSize);
						initialLocation.x += x;
						initialLocation.y += y;
						return initialLocation;
					}

					@Override
					public boolean close() {
						// restore shell location if we moved it:
						Shell resultShell = getShell();
						if (resultShell != null && !resultShell.isDisposed()) {
							Point location = resultShell.getLocation();
							if (location.equals(initialLocation)) {
								resultShell.setVisible(false);
								resultShell.setLocation(location.x - x,
										location.y - y);
							}
						}
						boolean result = super.close();

						// activate next result dialog (not the multi-result
						// dialog):

						// TODO: This doesn't work due to
						// https://bugs.eclipse.org/388667 :
						// Shell[] subShells = shell.getShells();
						// if (subShells.length > 0) {
						// subShells[subShells.length - 1].setActive();
						// }

						dialogs.remove(this);
						if (dialogs.size() > 0)
							dialogs.getLast().getShell().setActive();

						return result;
					}
				};
				dialog.create();
				dialog.getShell().addShellListener(new ShellAdapter() {
					public void shellActivated(
							org.eclipse.swt.events.ShellEvent e) {
						dialogs.remove(dialog);
						dialogs.add(dialog);
					}
				});
				dialog.open();
			}
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.MultiPushResultDialog_WindowTitle);
	}
}
