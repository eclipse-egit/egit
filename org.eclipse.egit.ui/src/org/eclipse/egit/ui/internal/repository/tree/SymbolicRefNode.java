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

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;

/**
 * Represents the "Symbolic Reference" node
 */
public class SymbolicRefNode extends RepositoryTreeNode<Ref> {

	/**
	 * Constructs the node.
	 *
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param ref
	 *            the symbolic reference
	 */
	public SymbolicRefNode(RepositoryTreeNode parent, FileRepository repository,
			Ref ref) {
		super(parent, RepositoryTreeNodeType.SYMBOLICREF, repository, ref);
	}

}
