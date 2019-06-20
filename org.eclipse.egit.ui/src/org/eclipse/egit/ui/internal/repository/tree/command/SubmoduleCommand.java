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
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jgit.lib.Repository;

/**
 * Base submodule command with helpers for finding the selected submodule paths
 * and parent repositories
 *
 * @param <V>
 */
public abstract class SubmoduleCommand<V extends RepositoryTreeNode<?>> extends
		RepositoriesViewCommandHandler<V> {

	/**
	 * Get submodule from selected nodes
	 * <p>
	 * Keys with null values denote repositories where all submodules should be
	 * used for the current command being executed
	 *
	 * @param nodes
	 * @return non-null but possibly empty map of parent repository's to
	 *         submodule paths
	 */
	protected Map<Repository, List<String>> getSubmodules(
			final List<RepositoryTreeNode<?>> nodes) {
		final Map<Repository, List<String>> repoPaths = new HashMap<>();
		for (RepositoryTreeNode<?> node : nodes) {
			if (node.getType() == RepositoryTreeNodeType.REPO) {
				Repository parent = node.getParent().getRepository();
				String path = Repository.stripWorkDir(parent.getWorkTree(),
						node.getRepository().getWorkTree());
				List<String> paths = repoPaths.computeIfAbsent(parent,
						key -> new ArrayList<>());
				paths.add(path);
			}
		}
		for (RepositoryTreeNode<?> node : nodes)
			if (node.getType() == RepositoryTreeNodeType.SUBMODULES)
				// Clear paths so all submodules are updated
				repoPaths.put(node.getParent().getRepository(), null);
		return repoPaths;
	}

}
