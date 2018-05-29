/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and other.
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

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Tag" node
 */
public class TagNode extends RepositoryTreeNode<Ref> {

	private boolean annotated;
	private String commitId;
	private String shortMessage;

	/**
	 * Constructs the node.
	 *
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param ref
	 *            the tag reference
	 */
	public TagNode(RepositoryTreeNode parent, Repository repository, Ref ref) {
		super(parent, RepositoryTreeNodeType.TAG, repository, ref);
	}

	/**
	 * Constructs the node including information for icon and labels.
	 *
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param ref
	 *            the tag reference
	 * @param annotated
	 *            whether tag is annotated or not (lightweight), for icon
	 * @param commitId
	 *            ID of commit that the tag points to, for label
	 * @param commitShortMessage
	 *            short message of commit that the tag points to, for label
	 */
	public TagNode(RepositoryTreeNode parent, Repository repository, Ref ref,
			boolean annotated, String commitId, String commitShortMessage) {
		super(parent, RepositoryTreeNodeType.TAG, repository, ref);
		this.annotated = annotated;
		this.commitId = commitId;
		this.shortMessage = commitShortMessage;
	}

	/**
	 * @return whether tag is annotated or not (lightweight)
	 */
	public boolean isAnnotated() {
		return annotated;
	}

	/**
	 * @return commit ID
	 */
	public String getCommitId() {
		return commitId;
	}

	/**
	 * @return short commit message
	 */
	public String getCommitShortMessage() {
		return shortMessage;
	}
}
