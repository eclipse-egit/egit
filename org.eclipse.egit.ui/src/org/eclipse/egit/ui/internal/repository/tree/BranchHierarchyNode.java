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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
	 * @return the child paths
	 * @throws IOException
	 */
	public List<IPath> getChildPaths() throws IOException {
		List<IPath> result = new ArrayList<>();
		for (IPath childPath : getPathMap().keySet()) {
			if (getObject().isPrefixOf(childPath)) {
				int segmentDiff = childPath.segmentCount()
						- getObject().segmentCount();
				if (segmentDiff > 1) {
					IPath newPath = getObject().append(
							childPath.segment(getObject().segmentCount()));
					if (!result.contains(newPath))
						result.add(newPath);
				}
			}
		}
		return result;
	}

	/**
	 * @return the direct child Refs (branches) only
	 * @throws IOException
	 */
	public List<Ref> getChildRefs() throws IOException {
		List<Ref> childRefs = new ArrayList<>();
		for (Entry<IPath, Ref> entry : getPathMap().entrySet()) {
			IPath childPath = entry.getKey();
			if (getObject().isPrefixOf(childPath)) {
				int segmentDiff = childPath.segmentCount()
						- getObject().segmentCount();
				if (segmentDiff == 1) {
					childRefs.add(entry.getValue());
				}
			}
		}
		return childRefs;
	}

	/**
	 * @return all child Refs reachable from this hierarchy node
	 * @throws IOException
	 */
	public List<Ref> getChildRefsRecursive() throws IOException {
		return getRepository().getRefDatabase()
				.getRefsByPrefix(getObject().toPortableString()).stream()
				.filter(ref -> !ref.isSymbolic()).collect(Collectors.toList());
	}

	private Map<IPath, Ref> getPathMap() throws IOException {
		Map<String, Ref> refsMap = getRepository().getRefDatabase().getRefs(
				getObject().toPortableString()); // getObject() returns path ending with /
		return refsMap.entrySet().stream()
				.filter(entry -> !entry.getValue().isSymbolic())
				.collect(Collectors.toMap(
						entry -> getObject().append(new Path(entry.getKey())),
						entry -> entry.getValue()));
	}
}
