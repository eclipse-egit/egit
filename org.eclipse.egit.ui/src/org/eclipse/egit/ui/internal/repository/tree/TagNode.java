/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP AG and others.
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

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * A node for a git tag.
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
	 *            ID of commit that the tag points to
	 * @param shortMessage
	 *            short message of the commit that the tag points to, or the
	 *            tag's short message
	 */
	public TagNode(RepositoryTreeNode parent, Repository repository, Ref ref,
			boolean annotated, String commitId, String shortMessage) {
		super(parent, RepositoryTreeNodeType.TAG, repository, ref);
		this.annotated = annotated;
		this.commitId = commitId;
		this.shortMessage = shortMessage;
	}

	/**
	 * Tells whether the tag is annotated or not (lightweight).
	 *
	 * @return {@code true} if the tag is annotated, {@code false} otherwise
	 */
	public boolean isAnnotated() {
		return annotated;
	}

	/**
	 * Retrieves the OID of the commit the tag ultimately points to.
	 *
	 * @return the commit id, or {@code null} if the tag doesn't point to a
	 *         commit
	 */
	public String getCommitId() {
		return commitId;
	}

	/**
	 * Retrieves the message associated with the tag; either the short commit
	 * message (if {@link #getCommitId()} != {@code null}), or the short tag
	 * message if it's an annotated tag not pointing to a commit.
	 *
	 * @return the message text
	 */
	public @NonNull String getShortMessage() {
		String msg = shortMessage;
		return msg == null ? "" : msg; //$NON-NLS-1$
	}
}
