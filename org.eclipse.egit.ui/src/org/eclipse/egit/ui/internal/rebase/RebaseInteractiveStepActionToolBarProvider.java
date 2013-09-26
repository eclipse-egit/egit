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

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.ElementAction;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.ElementType;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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
	public RebaseInteractiveStepActionToolBarProvider(Composite parent,
			int style, RebaseInteractiveView view) {
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
				RebaseInteractivePlan.ElementAction.PICK));
		itemPick.setText(UIText.RebaseInteractiveStepActionToolBarProvider_PickText);
		rebaseActionItems[0] = itemPick;

		itemDelete = new ToolItem(theToolbar, SWT.RADIO);
		deleteImage = UIIcons.ELCL16_DELETE.createImage();
		itemDelete.setImage(deleteImage);
		itemDelete.addSelectionListener(new ActionSelectionListener(
				RebaseInteractivePlan.ElementAction.SKIP));
		itemDelete
				.setText(UIText.RebaseInteractiveStepActionToolBarProvider_SkipText);
		rebaseActionItems[1] = itemDelete;

		itemEdit = new ToolItem(theToolbar, SWT.RADIO);
		editImage = UIIcons.EDITCONFIG.createImage();
		itemEdit.setImage(editImage);
		itemEdit.addSelectionListener(new ActionSelectionListener(
				RebaseInteractivePlan.ElementAction.EDIT));
		itemEdit.setText(UIText.RebaseInteractiveStepActionToolBarProvider_EditText);
		rebaseActionItems[2] = itemEdit;

		itemSquash = new ToolItem(theToolbar, SWT.RADIO);
		itemSquash.addSelectionListener(new ActionSelectionListener(
						RebaseInteractivePlan.ElementAction.SQUASH));
		itemSquash.setText(UIText.RebaseInteractiveStepActionToolBarProvider_SquashText);
		rebaseActionItems[3] = itemSquash;

		itemFixup = new ToolItem(theToolbar, SWT.RADIO);
		itemFixup.addSelectionListener(new ActionSelectionListener(
						RebaseInteractivePlan.ElementAction.FIXUP));
		itemFixup.setText(UIText.RebaseInteractiveStepActionToolBarProvider_FixupText);
		rebaseActionItems[4] = itemFixup;

		itemReword = new ToolItem(theToolbar, SWT.RADIO);
		itemReword.addSelectionListener(new ActionSelectionListener(
						RebaseInteractivePlan.ElementAction.REWORD));
		itemReword.setText(UIText.RebaseInteractiveStepActionToolBarProvider_RewordText);
		rebaseActionItems[5] = itemReword;

		new ToolItem(theToolbar, SWT.SEPARATOR);

		itemMoveUp = new ToolItem(theToolbar, SWT.NONE);
		itemMoveUp.setText(UIText.RebaseInteractiveStepActionToolBarProvider_MoveUpText);
		itemMoveUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RebaseInteractivePlan.PlanElement selectedEntry = getSingleSelectedTodoLine(false);
				if (selectedEntry == null)
					return;
				if (selectedEntry.getElementType() != ElementType.TODO)
					return;
				view.getCurrentPlan().moveTodoEntryUp(selectedEntry);
			}
		});

		itemMoveDown = new ToolItem(theToolbar, SWT.NONE);
		itemMoveDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RebaseInteractivePlan.PlanElement selectedEntry = getSingleSelectedTodoLine(false);
				if (selectedEntry == null)
					return;
				if (selectedEntry.getElementType() != ElementType.TODO)
					return;
				view.getCurrentPlan().moveTodoEntryDown(selectedEntry);
			}
		});
		itemMoveDown.setText(UIText.RebaseInteractiveStepActionToolBarProvider_MoveDownText);

	}

	private class ActionSelectionListener implements SelectionListener {
		private final RebaseInteractivePlan.ElementAction type;

		ActionSelectionListener(
				final RebaseInteractivePlan.ElementAction action) {
			this.type = action;
		}

		public void widgetSelected(SelectionEvent e) {
			List<RebaseInteractivePlan.PlanElement> selected = getSelectedRebaseTodoLines();
			if (selected == null || selected.isEmpty())
				return;
			for (RebaseInteractivePlan.PlanElement element : selected) {
				element.setPlanElementAction(type);
			}
			mapActionItemsToSelection(view.planTreeViewer.getSelection());
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	}

	/**
	 * Return a single instance of
	 * {@link org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement}
	 * representing the current selection in
	 * {@link RebaseInteractiveView#planTreeViewer}
	 *
	 * @param firstOfMultipleSelection
	 *            indicating whether to pick first element if multiple instances
	 *            of
	 *            {@link org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement}
	 *            are selected
	 * @return the selected instance of
	 *         {@link org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement}
	 *         if a single instance is selected. The first element of multiple
	 *         selected
	 *         {@link org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement}
	 *         instances only if firstOfMultipleSelection is set, null otherwise
	 *         or null if no instance of
	 *         {@link org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement}
	 *         is selected.
	 */
	protected RebaseInteractivePlan.PlanElement getSingleSelectedTodoLine(
			boolean firstOfMultipleSelection) {
		List<RebaseInteractivePlan.PlanElement> selected = getSelectedRebaseTodoLines();
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
	 * {@link RebaseInteractiveView#planTreeViewer} of type
	 * {@link org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement}
	 *
	 * @return a {@link List}<
	 *         {@link org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement}
	 *         > as the current selection.
	 */
	protected List<RebaseInteractivePlan.PlanElement> getSelectedRebaseTodoLines() {
		IStructuredSelection selection = (IStructuredSelection) view.planTreeViewer
				.getSelection();
		List<RebaseInteractivePlan.PlanElement> planEntries = new ArrayList<RebaseInteractivePlan.PlanElement>(
				selection.size());
		@SuppressWarnings("unchecked")
		List<RebaseInteractivePlan.PlanElement> candidates = selection
				.toList();
		for (Object candidate : candidates) {
			if (candidate instanceof RebaseInteractivePlan.PlanElement)
				planEntries
						.add((RebaseInteractivePlan.PlanElement) candidate);
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
			if (!(obj instanceof PlanElement))
				return;
			PlanElement firstSelectedEntry = (PlanElement) obj;

			if (structured.size() > 1) {
				// multi selection
				setMoveItemsEnabled(false);
				ElementAction type = firstSelectedEntry
						.getPlanElementAction();
				for (Iterator iterator = structured.iterator(); iterator
						.hasNext();) {
					Object selectedObj = iterator.next();
					if (!(selectedObj instanceof PlanElement))
						continue;
					PlanElement entry = (PlanElement) selectedObj;
					if (type != entry.getPlanElementAction()) {
						unselectAllActionItemsExecpt(null);
						break;
					}
				}
				return;
			}

			// single selection
			setMoveItemsEnabled(true);
			unselectAllActionItemsExecpt(getItemFor(firstSelectedEntry
					.getPlanElementAction()));
		}
	}

	private ToolItem getItemFor(ElementAction type) {
		switch (type) {
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
		case SKIP:
			return itemDelete;
		default:
			return null;
		}
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
