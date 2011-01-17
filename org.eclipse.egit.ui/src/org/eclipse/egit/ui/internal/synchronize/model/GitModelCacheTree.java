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
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache.FileModelFactory;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Because Git cache holds changes on file level (SHA-1 of trees are same as in
 * repository) we must emulate tree representation of staged files.
 */
public class GitModelCacheTree extends GitModelTree {

	private final FileModelFactory factory;

	private final Map<IPath, GitModelObject> cacheTreeMap;

	/**
	 * @param parent
	 *            parent object
	 * @param location tree location
	 * @param commit
	 *            last {@link RevCommit} in repository
	 * @param repoId
	 *            {@link ObjectId} of blob in repository
	 * @param cacheId
	 *            {@link ObjectId} of blob in cache
	 * @param factory
	 * @throws IOException
	 */
	public GitModelCacheTree(GitModelObjectContainer parent, IPath location,
			RevCommit commit, ObjectId repoId, ObjectId cacheId,
			FileModelFactory factory) throws IOException {
		super(parent, location, commit, repoId, repoId, cacheId);
		this.factory = factory;
		cacheTreeMap = new HashMap<IPath, GitModelObject>();
	}

	void addChild(ObjectId repoId, ObjectId cacheId, String path)
			throws IOException {
		String[] entrys = path.split("/"); //$NON-NLS-1$
		String pathKey = entrys[0];
		Path childLoation = new Path(pathKey);
		if (entrys.length > 1) {
			GitModelCacheTree cacheEntry = (GitModelCacheTree) cacheTreeMap
					.get(childLoation);
			if (cacheEntry == null) {
				cacheEntry = new GitModelCacheTree(this, childLoation,
						baseCommit, repoId, cacheId, factory);
				cacheTreeMap.put(childLoation, cacheEntry);
			}
			cacheEntry.addChild(repoId, cacheId,
					path.substring(path.indexOf('/') + 1));
		} else
			cacheTreeMap.put(childLoation, factory.createFileModel(this,
					childLoation, baseCommit, repoId, cacheId));
	}

	@Override
	protected GitModelObject[] getChildrenImpl() {
		Collection<GitModelObject> values = cacheTreeMap.values();

		return values.toArray(new GitModelObject[values.size()]);
	}

}
