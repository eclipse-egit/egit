/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;
import static org.eclipse.jgit.lib.FileMode.MISSING;
import static org.eclipse.jgit.lib.FileMode.TREE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Git cache representation in EGit Change Set
 */
public class GitModelCache extends GitModelObjectContainer {

	/**
	 * NTH of {@link DirCacheIterator}
	 */
	protected int dirCacheIteratorNth;

	private final FileModelFactory fileFactory;

	private final Map<IPath, GitModelCacheTree> cacheTreeMap;

	private static final int BASE_NTH = 0;

	private static final int REMOTE_NTH = 1;

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
		 * @param parent
		 *            parent object
		 * @param location file location
		 * @param commit
		 *            last {@link RevCommit} in repository
		 * @param repoId
		 *            {@link ObjectId} of blob in repository
		 * @param cacheId
		 *            {@link ObjectId} of blob in cache
		 * @return instance of {@link GitModelBlob}
		 * @throws IOException
		 */
		GitModelBlob createFileModel(GitModelObjectContainer parent, IPath location,
				RevCommit commit, ObjectId repoId, ObjectId cacheId)
				throws IOException;
	}

	/**
	 * Constructs model node that represents current status of Git cache.
	 *
	 * @param parent
	 *            parent object
	 * @param baseCommit
	 *            last {@link RevCommit} in repository
	 * @throws IOException
	 */
	public GitModelCache(GitModelObject parent, RevCommit baseCommit)
			throws IOException {
		this(parent, baseCommit, new FileModelFactory() {

			public GitModelBlob createFileModel(
					GitModelObjectContainer modelParent, IPath location, RevCommit commit,
					ObjectId repoId, ObjectId cacheId)
					throws IOException {
				return new GitModelCacheFile(modelParent, location, commit,
						repoId, cacheId);
			}
		});
	}

	/**
	 * @param parent
	 * @param baseCommit
	 * @param fileFactory
	 * @throws IOException
	 */
	protected GitModelCache(GitModelObject parent, RevCommit baseCommit,
			FileModelFactory fileFactory) throws IOException {
		super(parent, null, baseCommit, RIGHT);
		this.fileFactory = fileFactory;
		cacheTreeMap = new HashMap<IPath, GitModelCacheTree>();
	}

	@Override
	public String getName() {
		return UIText.GitModelIndex_index;
	}

	protected GitModelObject[] getChildrenImpl() {
		List<GitModelObject> result = new ArrayList<GitModelObject>();

		try {
			TreeWalk tw = createAndConfigureTreeWalk();

			while (tw.next()) {
				GitModelObject entry = extractFromCache(tw);
				if (entry == null)
					continue;

				result.add(entry);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return result.toArray(new GitModelObject[result.size()]);
	}

	/**
	 * Creates and configures {@link TreeWalk} instance for
	 * {@link GitModelCache#getChildrenImpl()} method. It is IMPORTANT to add
	 * tree that will be used as a base as first, remote tree should be added as
	 * second; {@link GitModelCache#dirCacheIteratorNth} should be set with
	 * value of NTH that corresponds with {@link DirCacheIterator}.
	 *
	 * @return configured instance of TreeW
	 * @throws IOException
	 */
	protected TreeWalk createAndConfigureTreeWalk() throws IOException {
		TreeWalk tw = createTreeWalk();
		tw.setRecursive(true);

		Repository repo = getRepository();
		DirCache index = repo.readDirCache();
		ObjectId headId = repo.getRef(Constants.HEAD).getObjectId();
		tw.addTree(new RevWalk(repo).parseTree(headId));
		tw.addTree(new DirCacheIterator(index));
		dirCacheIteratorNth = 1;

		return tw;
	}

	private GitModelObject extractFromCache(TreeWalk tw) throws IOException {
		DirCacheIterator cacheIterator = tw.getTree(dirCacheIteratorNth,
				DirCacheIterator.class);
		if (cacheIterator == null)
			return null;

		DirCacheEntry cacheEntry = cacheIterator.getDirCacheEntry();
		if (cacheEntry == null)
			return null;

		if (shouldIncludeEntry(tw)) {
			String path = new String(tw.getRawPath());
			ObjectId repoId = tw.getObjectId(BASE_NTH);
			ObjectId cacheId = tw.getObjectId(REMOTE_NTH);

			if (path.split("/").length > 1) //$NON-NLS-1$
				return handleCacheTree(repoId, cacheId, path);

			return fileFactory.createFileModel(this, new Path(path),
					baseCommit, repoId, cacheId);
		}

		return null;
	}

	private boolean shouldIncludeEntry(TreeWalk tw) {
		final int mHead = tw.getRawMode(BASE_NTH);
		final int mCache = tw.getRawMode(REMOTE_NTH);

		return mHead == MISSING.getBits() // initial add to cache
				|| mCache == MISSING.getBits() // removed from cache
				|| (mHead != mCache || (mCache != TREE.getBits() && !tw
						.idEqual(BASE_NTH, REMOTE_NTH))); // modified
	}

	private GitModelObject handleCacheTree(ObjectId repoId, ObjectId cacheId,
			String path) throws IOException {
		IPath childLocation = new Path(path);
		GitModelCacheTree cacheTree = cacheTreeMap.get(childLocation);
		if (cacheTree == null) {
			cacheTree = new GitModelCacheTree(this, childLocation, baseCommit,
					repoId, cacheId, fileFactory);
			cacheTreeMap.put(childLocation, cacheTree);
		}

		cacheTree.addChild(repoId, cacheId,
				path.substring(path.indexOf('/') + 1));

		return cacheTree;
	}

}
