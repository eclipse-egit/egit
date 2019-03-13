/*******************************************************************************
 * Copyright (C) 2019, Alexander Nittka <alex@nittka.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

/**
 * This class represents the tree node of a repository group.
 */
public class RepositoryGroupNode extends RepositoryTreeNode<String> {

	private RepositoryGroup group;

	/**
	 * @param parent
	 * @param group
	 */
	public RepositoryGroupNode(RepositoryTreeNode parent,
			RepositoryGroup group) {
		super(parent, RepositoryTreeNodeType.REPOGROUP, null, group.getName());
		this.group = group;
	}

	/**
	 * @return whether there are repositories in this group
	 */
	public boolean hasChildren() {
		return group.hasRepositories();
	}

	/**
	 * @return the group represented by the node
	 */
	public RepositoryGroup getGroup() {
		return group;
	}

}
