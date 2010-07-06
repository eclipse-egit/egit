/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;

/**
 * Represents the "Remote" node
 */
public class RemoteNode extends RepositoryTreeNode<String> {

	/**
	 * Constructs the node.
	 *
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param remoteName
	 *            the name of the remote specification
	 */
	public RemoteNode(RepositoryTreeNode parent, FileRepository repository,
			String remoteName) {
		super(parent, RepositoryTreeNodeType.REMOTE, repository, remoteName);
	}

}
