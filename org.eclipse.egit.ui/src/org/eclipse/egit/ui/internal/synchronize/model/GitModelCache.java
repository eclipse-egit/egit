/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Repository;

/**
 * Git cache representation in EGit Change Set
 */
public class GitModelCache extends GitModelObjectContainer {

	private final Path location;

	private final FileModelFactory fileFactory;

	private final Map<String, GitModelCacheTree> cacheTreeMap;

	private final Repository repo;

	private final Map<String, Change> cache;

	/**
	 * This interface enables creating proper instance of {@link GitModelBlob}
	 * for cached and working files. In case of working files the left side
	 * content of Compare View is loaded from local hard drive.
	 */
	protected interface FileModelFactory {
		/**
		 * Creates proper instance of {@link GitModelBlob} for cache and working
		 * tree model representation
		 *
		 * @param objParent
		 *            parent object
		 * @param repo
		 *            repository associated with file that will be created
		 * @param change
		 *            change associated with file that will be created
		 * @param fullPath
		 *            absolute path
		 * @return instance of {@link GitModelBlob}
		 */
		GitModelBlob createFileModel(GitModelObjectContainer objParent,
				Repository repo, Change change, IPath fullPath);

		/**
		 * Distinguish working tree from changed/staged tree
		 *
		 * @return {@code true} when this tree is working tree, {@code false}
		 *         when it is a cached tree
		 */
		boolean isWorkingTree();
	}

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
			public GitModelBlob createFileModel(
					GitModelObjectContainer objParent, Repository nestedRepo,
					Change change, IPath path) {
				return new GitModelCacheFile(objParent, nestedRepo, change,
						path);
			}

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
	 * @param cache
	 *            list of changes associated with this object
	 * @param fileFactory
	 *            leaf instance factory
	 */
	protected GitModelCache(GitModelRepository parent, Repository repo, Map<String, Change> cache,
			FileModelFactory fileFactory) {
		super(parent);
		this.repo = repo;
		this.cache = cache;
		this.fileFactory = fileFactory;
		cacheTreeMap = new HashMap<String, GitModelCacheTree>();
		location = new Path(repo.getWorkTree().toString());
	}

	@Override
	public String getName() {
		return UIText.GitModelIndex_index;
	}

	@Override
	public GitModelObject[] getChildren() {
		List<GitModelObject> result = new ArrayList<GitModelObject>();

		for (Entry<String, Change> cacheEntry : cache.entrySet()) {
			GitModelObject entry = extractFromCache(cacheEntry.getValue(), cacheEntry.getKey());
			if (entry == null)
				continue;

			result.add(entry);
		}

		return result.toArray(new GitModelObject[result.size()]);
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
		for (GitModelTree modelTree : cacheTreeMap.values())
			modelTree.dispose();

		cache.clear();
		cacheTreeMap.clear();
	}

	private GitModelObject extractFromCache(Change change, String path) {
		if (path.contains("/")) //$NON-NLS-1$
			return handleCacheTree(change, path);

		return fileFactory.createFileModel(this, repo, change,
				location.append(path));
	}

	private GitModelObject handleCacheTree(Change change, String path) {
		int firstSlash = path.indexOf("/");//$NON-NLS-1$
		String pathKey = path.substring(0, firstSlash);
		GitModelCacheTree cacheTree = cacheTreeMap.get(pathKey);
		if (cacheTree == null) {
			IPath newPath = location.append(pathKey);
			cacheTree = new GitModelCacheTree(this, repo, newPath, fileFactory);
			cacheTreeMap.put(pathKey, cacheTree);
		}

		cacheTree.addChild(change, path.substring(firstSlash + 1));

		return cacheTree;
	}

}
