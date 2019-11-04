/*******************************************************************************
 * Copyright (c) 2010, 2019 SAP AG and others.
 *
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
 * Sorter for {@link RepositoryTreeNode}s, used in the Git Repositories View as
 * well as in different branch selection dialogs.
 */
// TODO extend ViewerComparator as soon as minimum platform version is 4.7
// (Oxygen)
@SuppressWarnings("deprecation") // used as navigator commonSorter extension
public class RepositoryTreeNodeSorter extends
		org.eclipse.jface.viewers.ViewerSorter {

	/**
	 * Default constructor
	 */
	public RepositoryTreeNodeSorter() {
		this(null);
	}

	/**
	 * Construct sorter from collator
	 * @param collator to be used for locale-sensitive sorting
	 */
	public RepositoryTreeNodeSorter(Collator collator) {
		super(collator);
	}

	@Override
	public int category(Object element) {
		if (element instanceof RepositoryTreeNode) {
			RepositoryTreeNode<?> node = (RepositoryTreeNode<?>) element;
			RepositoryTreeNodeType type = node.getType();
			switch (type) {
			case REPOGROUP:
				return RepositoryTreeNodeType.values().length; // At the bottom
			case BRANCHHIERARCHY:
				return RepositoryTreeNodeType.REF.ordinal();
			default:
				return type.ordinal();
			}
		}
		return super.category(element);
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int category1 = category(e1);
		int category2 = category(e2);

		if (category1 != category2) {
			return category1 - category2;
		}
		if (e1 instanceof RepositoryTreeNode
				&& e2 instanceof RepositoryTreeNode) {
			RepositoryTreeNode<?> node1 = (RepositoryTreeNode<?>) e1;
			RepositoryTreeNode<?> node2 = (RepositoryTreeNode<?>) e2;
			return node1.compareTo(node2);
		} else {
			return super.compare(viewer, e1, e2);
		}
	}
}
