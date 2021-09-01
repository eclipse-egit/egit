/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import java.util.Collection;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ICompareContainer;
import org.eclipse.compare.structuremergeviewer.DiffTreeViewer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * An enhanced {@link DiffTreeViewer} providing "collapse/expand all" toolbar
 * buttons and the "Show In" sub-menu in the context menu.
 */
public class GitDiffTreeViewer extends DiffTreeViewer {

	// The parent class is marked @noextend, but clearly intended to be
	// subclassed.

	private final ICompareContainer container;

	private IAction collapseAction;

	private IAction expandAction;

	private Collection<IAction> extraActions;

	/**
	 * Creates a new {@link GitDiffTreeViewer}.
	 *
	 * @param parent
	 *            to contain the new viewer
	 * @param container
	 *            of the compare editor containing this viewer
	 * @param configuration
	 *            of the comparison
	 */
	public GitDiffTreeViewer(Composite parent, ICompareContainer container,
			CompareConfiguration configuration) {
		super(parent, configuration);
		this.container = container;
	}

	@Override
	public void fireOpen(OpenEvent event) {
		// Make this accessible
		super.fireOpen(event);
	}

	/**
	 * Add more actions that shall be added to the context menu.
	 *
	 * @param actions
	 *            to add
	 */
	public void setActions(Collection<IAction> actions) {
		extraActions = actions;
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		super.fillContextMenu(manager);
		if (!manager.isEmpty()) {
			IWorkbenchPart part = container.getWorkbenchPart();
			if (part != null) {
				manager.add(new Separator());
				manager.add(UIUtils
						.createShowInMenu(part.getSite().getWorkbenchWindow()));
			}
		}
		if (extraActions != null && !extraActions.isEmpty()) {
			manager.add(new Separator());
			extraActions.forEach(manager::add);
		}
	}

	@Override
	protected void createToolItems(ToolBarManager toolbarManager) {
		// This is called via the super constructor. Local fields in this
		// subclass are not initialized yet.
		super.createToolItems(toolbarManager);
		collapseAction = new Action(UIText.UIUtils_CollapseAll,
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
						ISharedImages.IMG_ELCL_COLLAPSEALL)) {
			@Override
			public void run() {
				// TODO: improve to leave the currently visible item expanded?
				UIUtils.collapseAll(GitDiffTreeViewer.this);
			}
		};
		toolbarManager.add(collapseAction);
		expandAction = new Action(UIText.UIUtils_ExpandAll,
				UIIcons.EXPAND_ALL) {
			@Override
			public void run() {
				UIUtils.expandAll(GitDiffTreeViewer.this);
			}
		};
		toolbarManager.add(expandAction);
	}

	@Override
	protected void inputChanged(Object in, Object oldInput) {
		super.inputChanged(in, oldInput);
		if (in != oldInput) {
			updateActions(in);
		}
	}

	private void updateActions(Object input) {
		boolean enabled = false;
		if (input instanceof IDiffContainer) {
			IDiffElement[] children = ((IDiffContainer) input).getChildren();
			for (IDiffElement child : children) {
				if ((child instanceof IDiffContainer)
						&& ((IDiffContainer) child).hasChildren()) {
					enabled = true;
					break;
				}
			}
		}
		collapseAction.setEnabled(enabled);
		expandAction.setEnabled(enabled);
	}
}
