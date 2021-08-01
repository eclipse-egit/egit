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

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action to compare two selected {@link DiffNode}s for files in the compare
 * editor. The action is enabled only if the current selection has exactly two
 * elements and both are files. The main purpose is to have a way to compare
 * renamed files that were not detected as renames, but as an addition and a
 * deletion.
 */
public class CompareWithEachOtherAction extends Action implements IUpdate {

	/**
	 * The command ID to register this action with.
	 */
	public static final String COMMAND_ID = "org.eclipse.egit.ui.CompareWithEachOther"; //$NON-NLS-1$

	private final GitDiffTreeViewer viewer;

	/**
	 * Creates a new {@link CompareWithEachOtherAction}.
	 *
	 * @param viewer
	 *            to operate on
	 * @param label
	 *            for the action
	 * @param icon
	 *            for the action
	 */
	public CompareWithEachOtherAction(GitDiffTreeViewer viewer, String label,
			ImageDescriptor icon) {
		super(label, icon);
		this.viewer = viewer;
		viewer.addSelectionChangedListener(event -> update());
	}

	@Override
	public void run() {
		DiffNode[] nodes = getNodes();
		if (nodes.length != 2) {
			return;
		}
		DiffNode newNode = new DiffNode(getElement(nodes[0]),
				getElement(nodes[1]));
		IStructuredSelection selection = new StructuredSelection(newNode);
		viewer.fireOpen(new OpenEvent(viewer, selection));
	}

	@Override
	public void update() {
		setEnabled(getNodes().length == 2);
	}

	private DiffNode[] getNodes() {
		IStructuredSelection selection = viewer.getStructuredSelection();
		if (selection.size() != 2) {
			return new DiffNode[0];
		}
		DiffNode[] nodes = new DiffNode[2];
		int i = 0;
		for (Object obj : selection.toList()) {
			if (obj instanceof DiffNode) {
				DiffNode node = (DiffNode) obj;
				if (node.hasChildren()
						|| ITypedElement.FOLDER_TYPE.equals(node.getType())) {
					return new DiffNode[0];
				}
				nodes[i++] = node;
			}
		}
		return nodes;
	}

	private ITypedElement getElement(DiffNode node) {
		ITypedElement left = node.getLeft();
		if (left != null) {
			return left;
		}
		return node.getRight();
	}
}
