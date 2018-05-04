/*******************************************************************************
 * Copyright (c) 2013, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 460595
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.ElementAction;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.ElementType;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
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
 * Toolbar provider for editing rebase interactive steps
 */
public class RebaseInteractiveStepActionToolBarProvider {

	private static final int[] MOVE_ACCELERATORS = { SWT.MOD1 + SWT.ARROW_UP,
			SWT.MOD1 + SWT.ARROW_DOWN };

	private ToolItem itemMoveUp;

	private ToolItem itemMoveDown;

	private final Map<RebaseInteractivePlan.ElementAction, ToolItem> rebaseActionItems = new EnumMap<>(
			RebaseInteractivePlan.ElementAction.class);

	private final Map<RebaseInteractivePlan.ElementAction, Integer> accelerators = new EnumMap<>(
			RebaseInteractivePlan.ElementAction.class);

	private final RebaseInteractiveView view;

	private final ToolBar theToolbar;

	private LocalResourceManager resources = new LocalResourceManager(
			JFaceResources.getResources());

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
		accelerators.put(ElementAction.EDIT, Integer.valueOf('E'));
		accelerators.put(ElementAction.FIXUP, Integer.valueOf('F'));
		accelerators.put(ElementAction.PICK, Integer.valueOf('P'));
		accelerators.put(ElementAction.REWORD, Integer.valueOf('R'));
		accelerators.put(ElementAction.SKIP, Integer.valueOf(SWT.DEL));
		accelerators.put(ElementAction.SQUASH, Integer.valueOf('S'));
		this.theToolbar = new ToolBar(parent, style);
		this.view = view;
		createToolBarItems();
		this.theToolbar.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				dispose();
			}
		});
	}

	private Image getImage(ImageDescriptor descriptor) {
		return (Image) this.resources.get(descriptor);
	}

	private void dispose() {
		resources.dispose();
	}

	private String textFor(ElementAction action, String base) {
		int accelerator = getActionAccelerators().get(action).intValue();
		return base + " (" + SWTKeySupport.getKeyFormatterForPlatform().format( //$NON-NLS-1$
				SWTKeySupport.convertAcceleratorToKeyStroke(accelerator)) + ')';
	}

	private String textFor(int idx, String base) {
		int accelerator = MOVE_ACCELERATORS[idx];
		return base + " (" + SWTKeySupport.getKeyFormatterForPlatform().format( //$NON-NLS-1$
				SWTKeySupport.convertAcceleratorToKeyStroke(accelerator)) + ')';
	}

	@SuppressWarnings("unused")
	private void createToolBarItems() {
		ToolItem itemPick = new ToolItem(theToolbar, SWT.RADIO);
		itemPick.setImage(getImage(UIIcons.CHERRY_PICK));
		itemPick.addSelectionListener(new ActionSelectionListener(
				RebaseInteractivePlan.ElementAction.PICK));
		itemPick.setText(UIText.RebaseInteractiveStepActionToolBarProvider_PickText);
		itemPick.setToolTipText(textFor(ElementAction.PICK,
				UIText.RebaseInteractiveStepActionToolBarProvider_PickDesc));
		rebaseActionItems.put(ElementAction.PICK, itemPick);

		ToolItem itemSkip = new ToolItem(theToolbar, SWT.RADIO);
		itemSkip.setImage(getImage(UIIcons.REBASE_SKIP));
		itemSkip.addSelectionListener(new ActionSelectionListener(
				RebaseInteractivePlan.ElementAction.SKIP));
		itemSkip.setText(UIText.RebaseInteractiveStepActionToolBarProvider_SkipText);
		itemSkip.setToolTipText(textFor(ElementAction.SKIP,
				UIText.RebaseInteractiveStepActionToolBarProvider_SkipDesc));
		rebaseActionItems.put(ElementAction.SKIP, itemSkip);

		ToolItem itemEdit = new ToolItem(theToolbar, SWT.RADIO);
		itemEdit.setImage(getImage(UIIcons.EDITCONFIG));
		itemEdit.addSelectionListener(new ActionSelectionListener(
				RebaseInteractivePlan.ElementAction.EDIT));
		itemEdit.setText(UIText.RebaseInteractiveStepActionToolBarProvider_EditText);
		itemEdit.setToolTipText(textFor(ElementAction.EDIT,
				UIText.RebaseInteractiveStepActionToolBarProvider_EditDesc));
		rebaseActionItems.put(ElementAction.EDIT, itemEdit);

		ToolItem itemSquash = new ToolItem(theToolbar, SWT.RADIO);
		itemSquash.setImage(getImage(UIIcons.SQUASH_UP));
		itemSquash.addSelectionListener(new ActionSelectionListener(
				RebaseInteractivePlan.ElementAction.SQUASH));
		itemSquash.setText(
				UIText.RebaseInteractiveStepActionToolBarProvider_SquashText);
		itemSquash.setToolTipText(textFor(ElementAction.SQUASH,
				UIText.RebaseInteractiveStepActionToolBarProvider_SquashDesc));
		rebaseActionItems.put(ElementAction.SQUASH, itemSquash);

		ToolItem itemFixup = new ToolItem(theToolbar, SWT.RADIO);
		itemFixup.setImage(getImage(UIIcons.FIXUP_UP));
		itemFixup.addSelectionListener(new ActionSelectionListener(
				RebaseInteractivePlan.ElementAction.FIXUP));
		itemFixup.setText(
				UIText.RebaseInteractiveStepActionToolBarProvider_FixupText);
		itemFixup.setToolTipText(textFor(ElementAction.FIXUP,
				UIText.RebaseInteractiveStepActionToolBarProvider_FixupDesc));
		rebaseActionItems.put(ElementAction.FIXUP, itemFixup);

		ToolItem itemReword = new ToolItem(theToolbar, SWT.RADIO);
		itemReword.setImage(getImage(UIIcons.REWORD));
		itemReword.addSelectionListener(new ActionSelectionListener(
				RebaseInteractivePlan.ElementAction.REWORD));
		itemReword.setText(
				UIText.RebaseInteractiveStepActionToolBarProvider_RewordText);
		itemReword.setToolTipText(textFor(ElementAction.REWORD,
				UIText.RebaseInteractiveStepActionToolBarProvider_RewordDesc));
		rebaseActionItems.put(ElementAction.REWORD, itemReword);

		new ToolItem(theToolbar, SWT.SEPARATOR);

		itemMoveUp = new ToolItem(theToolbar, SWT.NONE);
		itemMoveUp.setImage(getImage(UIIcons.ELCL16_PREVIOUS));
		itemMoveUp.setText(
				UIText.RebaseInteractiveStepActionToolBarProvider_MoveUpText);
		itemMoveUp.setToolTipText(textFor(0,
				UIText.RebaseInteractiveStepActionToolBarProvider_MoveUpDesc));
		itemMoveUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveUp();
				view.planTreeViewer.getControl().setFocus();
			}
		});

		itemMoveDown = new ToolItem(theToolbar, SWT.NONE);
		itemMoveDown.setImage(getImage(UIIcons.ELCL16_NEXT));
		itemMoveDown.setText(
				UIText.RebaseInteractiveStepActionToolBarProvider_MoveDownText);
		itemMoveDown.setToolTipText(textFor(1,
				UIText.RebaseInteractiveStepActionToolBarProvider_MoveDownDesc));
		itemMoveDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveDown();
				view.planTreeViewer.getControl().setFocus();
			}
		});
	}

	private class ActionSelectionListener implements SelectionListener {
		private final RebaseInteractivePlan.ElementAction type;

		ActionSelectionListener(final RebaseInteractivePlan.ElementAction action) {
			this.type = action;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			List<RebaseInteractivePlan.PlanElement> selected = getSelectedRebaseTodoLines();
			if (selected == null || selected.isEmpty())
				return;

			ElementAction typeToSet = type;
			if (type != ElementAction.PICK) {
				boolean allItemsHaveTargetType = true;
				for (RebaseInteractivePlan.PlanElement element : selected)
					allItemsHaveTargetType &= element.getPlanElementAction() == type;
				if (allItemsHaveTargetType) {
					typeToSet = ElementAction.PICK;
					rebaseActionItems.get(ElementAction.PICK).setSelection(true);
					if (e.getSource() instanceof ToolItem)
						((ToolItem) e.getSource()).setSelection(false);
				}
			}

			for (RebaseInteractivePlan.PlanElement element : selected)
				element.setPlanElementAction(typeToSet);
			mapActionItemsToSelection(view.planTreeViewer.getSelection());
			view.planTreeViewer.getControl().setFocus();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
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
		List<RebaseInteractivePlan.PlanElement> planEntries = new ArrayList<>(
				selection.size());
		@SuppressWarnings("unchecked")
		List<RebaseInteractivePlan.PlanElement> candidates = selection.toList();
		for (Object candidate : candidates) {
			if (candidate instanceof RebaseInteractivePlan.PlanElement)
				planEntries.add((RebaseInteractivePlan.PlanElement) candidate);
		}
		return planEntries;
	}

	void mapActionItemsToSelection(ISelection selection) {
		setMoveItemsEnabled(false);
		if (selection == null || selection.isEmpty()) {
			if (theToolbar.isEnabled())
				theToolbar.setEnabled(false);

			unselectAllActionItemsExecpt(null);
			return;
		}
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection) selection;

			Object obj = structured.getFirstElement();
			if (!(obj instanceof PlanElement))
				return;
			PlanElement firstSelectedEntry = (PlanElement) obj;
			PlanElement lastSelectedEntry = firstSelectedEntry;

			ElementAction type = firstSelectedEntry.getPlanElementAction();

			boolean singleTypeSelected = true;

			if (!theToolbar.isEnabled()
					&& !view.getCurrentPlan().hasRebaseBeenStartedYet())
				theToolbar.setEnabled(true);

			if (structured.size() > 1) {
				// multi selection
				for (Object selectedObj : structured.toList()) {
					if (!(selectedObj instanceof PlanElement))
						continue;
					PlanElement entry = (PlanElement) selectedObj;
					lastSelectedEntry = entry;
					if (type != entry.getPlanElementAction()) {
						singleTypeSelected = false;
					}
				}
			}

			if (singleTypeSelected)
				unselectAllActionItemsExecpt(type);
			else
				unselectAllActionItemsExecpt(null);

			enableMoveButtons(firstSelectedEntry, lastSelectedEntry);

		}
	}

	private void enableMoveButtons(
			PlanElement firstSelectedEntry, PlanElement lastSelectedEntry) {
		List<PlanElement> list = view.getCurrentPlan().getList();
		List<PlanElement> stepList = new ArrayList<>();
		for (PlanElement planElement : list) {
			if (!planElement.isComment())
				stepList.add(planElement);
		}

		int firstEntryIndex = stepList.indexOf(firstSelectedEntry);
		int lastEntryIndex = stepList.indexOf(lastSelectedEntry);
		if (!RebaseInteractivePreferences.isOrderReversed()) {
			itemMoveUp.setEnabled(firstEntryIndex > 0);
			itemMoveDown.setEnabled(lastEntryIndex < stepList.size() - 1);
		} else {
			itemMoveUp.setEnabled(firstEntryIndex < stepList.size() - 1);
			itemMoveDown.setEnabled(lastEntryIndex > 0);
		}
	}

	private void unselectAllActionItemsExecpt(ElementAction action) {
		for (Map.Entry<ElementAction, ToolItem> entry : rebaseActionItems
				.entrySet()) {
			entry.getValue().setSelection(entry.getKey() == action);
		}
	}

	private void setMoveItemsEnabled(boolean enabled) {
		itemMoveDown.setEnabled(enabled);
		itemMoveUp.setEnabled(enabled);
	}

	void moveUp() {
		List<PlanElement> selectedRebaseTodoLines = getSelectedRebaseTodoLines();
		for (PlanElement planElement : selectedRebaseTodoLines) {
			if (planElement.getElementType() != ElementType.TODO)
				return;

			if (!RebaseInteractivePreferences.isOrderReversed())
				view.getCurrentPlan().moveTodoEntryUp(planElement);
			else
				view.getCurrentPlan().moveTodoEntryDown(planElement);

			mapActionItemsToSelection(view.planTreeViewer.getSelection());
		}
	}

	void moveDown() {
		List<PlanElement> selectedRebaseTodoLines = getSelectedRebaseTodoLines();
		Collections.reverse(selectedRebaseTodoLines);
		for (PlanElement planElement : selectedRebaseTodoLines) {
			if (planElement.getElementType() != ElementType.TODO)
				return;

			if (!RebaseInteractivePreferences.isOrderReversed())
				view.getCurrentPlan().moveTodoEntryDown(planElement);
			else
				view.getCurrentPlan().moveTodoEntryUp(planElement);

			mapActionItemsToSelection(view.planTreeViewer.getSelection());
		}
	}

	Map<ElementAction, Integer> getActionAccelerators() {
		return accelerators;
	}

	int[] getMoveAccelerators() {
		return MOVE_ACCELERATORS;
	}
}
