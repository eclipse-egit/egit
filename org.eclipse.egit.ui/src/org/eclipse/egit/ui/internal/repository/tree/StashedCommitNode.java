/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Node representing a stashed commit in a repository
 */
public class StashedCommitNode extends RepositoryTreeNode<RevCommit> {

	private final int index;

	/**
	 * Constructs the node
	 *
	 * @param parent
	 * @param repository
	 * @param index
	 * @param commit
	 */
	public StashedCommitNode(RepositoryTreeNode parent, Repository repository,
			int index, RevCommit commit) {
		super(parent, RepositoryTreeNodeType.STASHED_COMMIT, repository, commit);
		this.index = index;
	}

	/**
	 * @return index
	 */
	public int getIndex() {
		return index;
	}
}
