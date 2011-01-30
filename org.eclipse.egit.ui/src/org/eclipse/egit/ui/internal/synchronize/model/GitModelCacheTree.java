/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache.FileModelFactory;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Because Git cache holds changes on file level (SHA-1 of trees are same as in
 * repository) we must emulate tree representation of staged files.
 */
public class GitModelCacheTree extends GitModelTree {

	private final FileModelFactory factory;

	private final Map<String, GitModelObject> cacheTreeMap;

	/**
	 * @param parent
	 *            parent object
	 * @param commit
	 *            last {@link RevCommit} in repository
	 * @param repoId
	 *            {@link ObjectId} of blob in repository
	 * @param cacheId
	 *            {@link ObjectId} of blob in cache
	 * @param location
	 *            resource location
	 * @param factory
	 * @throws IOException
	 */
	public GitModelCacheTree(GitModelObjectContainer parent, RevCommit commit,
			ObjectId repoId, ObjectId cacheId, IPath location,
			FileModelFactory factory) throws IOException {
		super(parent, commit, null, repoId, repoId, cacheId, location);
		this.factory = factory;
		cacheTreeMap = new HashMap<String, GitModelObject>();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	void addChild(ObjectId repoId, ObjectId cacheId, String path)
			throws IOException {
		String[] entrys = path.split("/"); //$NON-NLS-1$
		String pathKey = entrys[0];
		IPath fullPath = getLocation().append(pathKey);
		if (entrys.length > 1) {
			GitModelCacheTree cacheEntry = (GitModelCacheTree) cacheTreeMap
					.get(pathKey);
			if (cacheEntry == null) {
				cacheEntry = new GitModelCacheTree(this, baseCommit, repoId,
						cacheId, fullPath, factory);
				cacheTreeMap.put(pathKey, cacheEntry);
			}
			cacheEntry.addChild(repoId, cacheId,
					path.substring(path.indexOf('/') + 1));
		} else
			cacheTreeMap.put(pathKey, factory.createFileModel(this,
					baseCommit, repoId, cacheId, fullPath));
	}

	@Override
	protected GitModelObject[] getChildrenImpl() {
		Collection<GitModelObject> values = cacheTreeMap.values();

		return values.toArray(new GitModelObject[values.size()]);
	}

}
