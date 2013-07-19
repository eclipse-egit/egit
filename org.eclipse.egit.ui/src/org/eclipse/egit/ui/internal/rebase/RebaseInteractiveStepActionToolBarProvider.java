/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractivePlan.PlanEntry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;



/**
 *
 */
public class RebaseInteractiveStepActionToolBarProvider {

	private ToolItem itemPick;

	private ToolItem itemDelete;

	private ToolItem itemEdit;

	private ToolItem itemSquash;

	private ToolItem itemFixup;

	private ToolItem itemReword;

	private ToolItem itemMoveUp;

	private ToolItem itemMoveDown;

	private final ToolItem[] rebaseActionItems = new ToolItem[6];

	private final RebaseInteractiveView view;

	private Image deleteImage;

	private Image editImage;

	private final ToolBar theToolbar;

	/**
	 * @return the theToolbar
	 */
	final ToolBar getTheToolbar() {
		return theToolbar;
	}

	/**
	 * @param parent
	 * @param style
	 * @param view
	 */
	public RebaseInteractiveStepActionToolBarProvider(Composite parent, int style,
			RebaseInteractiveView view) {
		this.theToolbar = new ToolBar(parent, style);
		this.view = view;
		createToolBarItems();
		this.theToolbar.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				dispose();
			}
		});
	}

	private void dispose() {
		deleteImage.dispose();
		editImage.dispose();
	}

	private void createToolBarItems() {
		itemPick = new ToolItem(theToolbar, SWT.RADIO);
		itemPick.addSelectionListener(new ActionSelectionListener(
				RebaseCommand.Action.PICK));
		itemPick.setText(UIText.InteractiveRebaseView_itemPick_text);
		rebaseActionItems[0] = itemPick;

		itemDelete = new ToolItem(theToolbar, SWT.RADIO);
		deleteImage = UIIcons.ELCL16_DELETE.createImage();
		itemDelete.setImage(deleteImage);
		itemDelete.addSelectionListener(new ActionSelectionListener(null));
		itemDelete.setText(UIText.InteractiveRebaseView_itemDelete_text);
		rebaseActionItems[1] = itemDelete;

		itemEdit = new ToolItem(theToolbar, SWT.RADIO);
		editImage = UIIcons.EDITCONFIG.createImage();
		itemEdit.setImage(editImage);
		itemEdit.addSelectionListener(new ActionSelectionListener(
				RebaseCommand.Action.EDIT));
		itemEdit.setText(UIText.InteractiveRebaseView_itemEdit_text);
		rebaseActionItems[2] = itemEdit;

		itemSquash = new ToolItem(theToolbar, SWT.RADIO);
		itemSquash.addSelectionListener(new ActionSelectionListener(
				RebaseCommand.Action.SQUASH));
		itemSquash.setText(UIText.InteractiveRebaseView_itemSquash_text);
		rebaseActionItems[3] = itemSquash;

		itemFixup = new ToolItem(theToolbar, SWT.RADIO);
		itemFixup.addSelectionListener(new ActionSelectionListener(
				RebaseCommand.Action.FIXUP));
		itemFixup.setText(UIText.InteractiveRebaseView_itemFixup_text);
		rebaseActionItems[4] = itemFixup;

		itemReword = new ToolItem(theToolbar, SWT.RADIO);
		itemReword.addSelectionListener(new ActionSelectionListener(
				RebaseCommand.Action.REWORD));
		itemReword.setText(UIText.InteractiveRebaseView_itemReword_text);
		rebaseActionItems[5] = itemReword;

		new ToolItem(theToolbar, SWT.SEPARATOR);

		itemMoveUp = new ToolItem(theToolbar, SWT.NONE);
		itemMoveUp.setText(UIText.InteractiveRebaseView_itemMoveUp_text);
		itemMoveUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PlanEntry selectedEntry = getSingleSelectedPlanEntry(false);
				if (selectedEntry != null) {
					view.input.getPlan().moveUp(selectedEntry);
					view.input.persist();
					view.planTreeViewer.refresh(true);
				}
			}
		});

		itemMoveDown = new ToolItem(theToolbar, SWT.NONE);
		itemMoveDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PlanEntry selectedEntry = getSingleSelectedPlanEntry(false);
				if (selectedEntry != null) {
					view.input.getPlan().moveDown(selectedEntry);
					view.input.persist();
					view.planTreeViewer.refresh(true);
				}
			}
		});
		itemMoveDown.setText(UIText.InteractiveRebaseView_itemMoveDown_text);

		new ToolItem(theToolbar, SWT.SEPARATOR);

		ToolItem itemRedo = new ToolItem(theToolbar, SWT.NONE);
		itemRedo.setWidth(10);
		// TODO: UIICons
		itemRedo.setImage(UIIcons.ELCL16_NEXT.createImage());
		itemRedo.setEnabled(false);
		itemRedo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				view.redo();
			}
		});
		itemRedo.setText(UIText.InteractiveRebaseView_itemRedo_text);

		ToolItem itemUndo = new ToolItem(theToolbar, SWT.NONE);
		// TODO: UIICons
		itemUndo.setImage(UIIcons.ELCL16_PREVIOUS.createImage());
		itemUndo.setEnabled(false);
		itemUndo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				view.undo();
			}
		});
		itemUndo.setText(UIText.InteractiveRebaseView_itemUndo_text);
	}

	private class ActionSelectionListener implements SelectionListener {
		private final Action action;

		ActionSelectionListener(final Action action) {
			this.action = action;
		}

		public void widgetSelected(SelectionEvent e) {
			List<PlanEntry> selected = getSelectedPlanEntries();
			if (selected == null || selected.isEmpty())
				return;
			RebaseInteractivePlan.markAll(selected, action);
			view.input.persist();
			mapActionItemsToSelection(view.planTreeViewer.getSelection());
			view.planTreeViewer.refresh(true);
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	}

	/**
	 * Return a single instance of {@link PlanEntry} representing the current
	 * selection in {@link RebaseInteractiveView#planTreeViewer}
	 *
	 * @param firstOfMultipleSelection
	 *            indicating whether to pick first element if multiple instances
	 *            of {@link PlanEntry} are selected
	 * @return the selected instance of {@link PlanEntry} if a single instance
	 *         is selected. The first element of multiple selected
	 *         {@link PlanEntry} instances only if firstOfMultipleSelection is
	 *         set, null otherwise or null if no instance of {@link PlanEntry}
	 *         is selected.
	 */
	protected PlanEntry getSingleSelectedPlanEntry(
			boolean firstOfMultipleSelection) {
		List<PlanEntry> selected = getSelectedPlanEntries();
		switch (selected.size()) {
		case 0:
			return null;
		case 1:
			return selected.get(0);
		default:
			if (firstOfMultipleSelection) {
				return selected.get(0);
			}
			break;
		}
		return null;
	}

	/**
	 * Returns the current selected entries in
	 * {@link RebaseInteractiveView#planTreeViewer} of type {@link PlanEntry}
	 *
	 * @return a {@link List}<{@link PlanEntry}> as the current selection.
	 */
	protected List<PlanEntry> getSelectedPlanEntries() {
		IStructuredSelection selection = (IStructuredSelection) view.planTreeViewer
				.getSelection();
		List<PlanEntry> planEntries = new ArrayList<PlanEntry>(selection.size());
		List<Object> candidates = selection.toList();
		for (Object candidate : candidates) {
			if (candidate instanceof PlanEntry)
				planEntries.add((PlanEntry) candidate);
		}
		return planEntries;
	}

	void mapActionItemsToSelection(ISelection selection) {
		setMoveItemsEnabled(false);
		if (selection == null || selection.isEmpty())
			return;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection) selection;

			Object obj = structured.getFirstElement();
			if (!(obj instanceof PlanEntry))
				return;
			PlanEntry firstSelectedEntry = (PlanEntry) obj;

			if (structured.size() > 1) {
				// multi selection
				setMoveItemsEnabled(false);
				Action action = firstSelectedEntry.getAction();
				for (Iterator iterator = structured.iterator(); iterator
						.hasNext();) {
					Object selectedObj = iterator.next();
					if (!(selectedObj instanceof PlanEntry))
						continue;
					PlanEntry entry = (PlanEntry) selectedObj;
					if (action != entry.getAction()) {
						unselectAllActionItemsExecpt(null);
						break;
					}
				}
				return;
			}

			// single selection
			setMoveItemsEnabled(true);
			unselectAllActionItemsExecpt(getItemFor(firstSelectedEntry
					.getAction()));
		}
	}

	private ToolItem getItemFor(Action action) {
		if (action == null)
			return itemDelete;
		switch (action) {
		case EDIT:
			return itemEdit;
		case FIXUP:
			return itemFixup;
		case PICK:
			return itemPick;
		case REWORD:
			return itemReword;
		case SQUASH:
			return itemSquash;
		}
		return null;
	}

	private void unselectAllActionItemsExecpt(ToolItem item) {
		for (int i = 0; i < rebaseActionItems.length; i++) {
			ToolItem currItem = rebaseActionItems[i];
			if (currItem == null)
				continue;
			if (currItem == item)
				currItem.setSelection(true);
			else
				currItem.setSelection(false);
		}
	}

	private void setMoveItemsEnabled(boolean enabled) {
		itemMoveDown.setEnabled(enabled);
		itemMoveUp.setEnabled(enabled);
	}

}
