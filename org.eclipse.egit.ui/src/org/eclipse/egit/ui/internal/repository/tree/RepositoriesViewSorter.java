/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import java.text.Collator;

import org.eclipse.jface.viewers.Viewer;

/**
 * Sorter for the Git Repositories View.
 */
public class RepositoriesViewSorter extends
		org.eclipse.jface.viewers.ViewerSorter {

	/**
	 * Default constructor
	 */
	public RepositoriesViewSorter() {
		this(null);
	}

	/**
	 * Construct sorter from collator
	 * @param collator to be used for locale-sensitive sorting
	 */
	public RepositoriesViewSorter(Collator collator) {
		super(collator);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int category(Object element) {
		if (element instanceof RepositoryTreeNode) {
			RepositoryTreeNode<? extends Object> node = (RepositoryTreeNode<? extends Object>) element;
			if (node.getType() == RepositoryTreeNodeType.BRANCHHIERARCHY)
				return RepositoryTreeNodeType.REF.ordinal();
			return node.getType().ordinal();
		}
		return super.category(element);
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof RepositoryTreeNode
				&& e2 instanceof RepositoryTreeNode) {
			RepositoryTreeNode node1 = (RepositoryTreeNode) e1;
			RepositoryTreeNode node2 = (RepositoryTreeNode) e2;
			return node1.compareTo(node2);
		} else {
			return super.compare(viewer, e1, e2);
		}
	}
}
