/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.util.Map;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.synchronize.model.TreeBuilder.FileModelFactory;
import org.eclipse.egit.ui.internal.synchronize.model.TreeBuilder.TreeModelFactory;
import org.eclipse.jgit.lib.Repository;

/**
 * Git cache representation in EGit Change Set
 */
public class GitModelCache extends GitModelObjectContainer {

	private final Path location;

	private final Repository repo;

	private GitModelObject[] children;

	/**
	 * Constructs model node that represents current status of Git cache.
	 *
	 * @param parent
	 *            parent object
	 *
	 * @param repo
	 *            repository associated with this object
	 * @param cache
	 *            cache containing all changed objects
	 */
	public GitModelCache(GitModelRepository parent, Repository repo,
			Map<String, Change> cache) {
		this(parent, repo, cache, new FileModelFactory() {
			@Override
			public GitModelBlob createFileModel(
					GitModelObjectContainer objParent, Repository nestedRepo,
					Change change, IPath path) {
				return new GitModelCacheFile(objParent, nestedRepo, change,
						path);
			}

			@Override
			public boolean isWorkingTree() {
				return false;
			}
		});
	}

	/**
	 * @param parent
	 *            parent object
	 * @param repo
	 *            repository associated with this object
	 * @param changes
	 *            list of changes associated with this object
	 * @param fileFactory
	 *            leaf instance factory
	 */
	protected GitModelCache(GitModelRepository parent, final Repository repo,
			Map<String, Change> changes, final FileModelFactory fileFactory) {
		super(parent);
		this.repo = repo;
		this.location = new Path(repo.getWorkTree().toString());

		this.children = TreeBuilder.build(this, repo, changes, fileFactory,
				new TreeModelFactory() {
					@Override
					public GitModelTree createTreeModel(
							GitModelObjectContainer parentObject,
							IPath fullPath,
							int kind) {
						return new GitModelCacheTree(parentObject, repo,
								fullPath, fileFactory);
					}
				});
	}

	@Override
	public String getName() {
		return UIText.GitModelIndex_index;
	}

	@Override
	public GitModelObject[] getChildren() {
		return children;
	}

	@Override
	public int getKind() {
		return Differencer.CHANGE | Differencer.RIGHT;
	}

	@Override
	public int repositoryHashCode() {
		return repo.getWorkTree().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof GitModelCache
				&& !(obj instanceof GitModelWorkingTree)) {
			GitModelCache left = (GitModelCache) obj;
			return left.getParent().equals(getParent());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return repositoryHashCode();
	}

	@Override
	public IPath getLocation() {
		return location;
	}

	@Override
	public String toString() {
		return "ModelCache"; //$NON-NLS-1$
	}

	@Override
	public void dispose() {
		if (children != null) {
			for (GitModelObject object : children)
				object.dispose();
			children = null;
		}
	}
}
