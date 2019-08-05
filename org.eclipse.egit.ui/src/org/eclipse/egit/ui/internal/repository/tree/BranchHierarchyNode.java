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

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Branch Hierarchy" node
 */
public class BranchHierarchyNode extends RepositoryTreeNode<IPath> {

	/**
	 * Constructs the node.
	 *
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param path
	 *            the path
	 */
	public BranchHierarchyNode(RepositoryTreeNode parent,
			Repository repository, IPath path) {
		// path must end with /
		super(parent, RepositoryTreeNodeType.BRANCHHIERARCHY, repository, path.addTrailingSeparator());
	}

	/**
	 * @return all child Refs reachable from this hierarchy node
	 * @throws IOException
	 */
	public List<Ref> getChildRefsRecursive() throws IOException {
		try {
			return getRepository().getRefDatabase()
					.getRefsByPrefix(getObject().toPortableString()).stream()
					.filter(ref -> !ref.isSymbolic())
					.collect(Collectors.toList());
		} catch (NoSuchFileException e) {
			// The common navigator may trigger children calculation for a
			// branch hierarchy node after all children have been deleted. In
			// that case the underlying path is gone and we should deal with the
			// exception here.
			return Collections.emptyList();
		}
	}

}
