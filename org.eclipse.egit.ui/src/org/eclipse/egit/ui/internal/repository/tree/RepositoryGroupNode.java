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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.jgit.lib.Repository;

/**
 * This class represents the tree node of a repository group.
 */
public class RepositoryGroupNode extends RepositoryTreeNode<RepositoryGroup> {

	private RepositoryGroup group;

	/**
	 * @param group
	 *            repository group represented by ths tree node
	 */
	public RepositoryGroupNode(RepositoryGroup group) {
		// parent is null as long as nested groups are not supported
		super(null, RepositoryTreeNodeType.REPOGROUP, null, group);
		this.group = group;
	}

	/**
	 * @return whether there are repositories in this group
	 */
	public boolean hasChildren() {
		return group.hasRepositories();
	}

	/**
	 * @return contained repositories
	 */
	public Collection<? extends Repository> getRepositories() {
		LinkedHashSet<Repository> result = new LinkedHashSet<>();
		RepositoryCache repositoryCache = org.eclipse.egit.core.Activator
				.getDefault().getRepositoryCache();
		List<File> repoDirs = getObject().getRepositoryDirectories();
		for (File repoDir : repoDirs) {
			Repository repo = null;
			try {
				repo = repositoryCache.lookupRepository(repoDir);
			} catch (IOException e) {
				// ignore
			}
			if (repo != null) {
				result.add(repo);
			}
		}
		return result;
	}
}
