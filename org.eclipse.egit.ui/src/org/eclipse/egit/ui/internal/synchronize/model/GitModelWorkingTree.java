/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.io.IOException;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.AdaptableFileTreeIterator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Representation of working tree in EGit ChangeSet model
 */
public class GitModelWorkingTree extends GitModelCache {

	/**
	 * Constructor used by JUnits
	 *
	 * @param parent
	 *            parent of working tree instance
	 * @throws IOException
	 */
	GitModelWorkingTree(GitModelObject parent) throws IOException {
		this(parent, null);
	}

	/**
	 * @param parent
	 *            parent of working tree instance
	 * @param pathFilter synchronize configuration
	 * @throws IOException
	 */
	public GitModelWorkingTree(GitModelObject parent, TreeFilter pathFilter)
			throws IOException {
		super(parent, null, pathFilter, new FileModelFactory() {
			public GitModelBlob createFileModel(
					GitModelObjectContainer modelParent, RevCommit modelCommit,
					ObjectId repoId, ObjectId cacheId, IPath location)
					throws IOException {
				return new GitModelWorkingFile(modelParent, modelCommit,
						repoId, location);
			}
		});
	}

	@Override
	public String getName() {
		return UIText.GitModelWorkingTree_workingTree;
	}

	@Override
	public int getKind() {
		// changes in working tree are always outgoing modifications
		return Differencer.RIGHT | Differencer.CHANGE;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof GitModelWorkingTree) {
			GitModelCache left = (GitModelCache) obj;
			return left.getParent().equals(getParent());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getParent().hashCode();
	}

	@Override
	public String toString() {
		return "ModelWorkingTree"; //$NON-NLS-1$
	}

	@Override
	protected TreeWalk createAndConfigureTreeWalk() throws IOException {
		TreeWalk tw = createTreeWalk();
		tw.setRecursive(true);

		Repository repo = getRepository();
		int ftIndex = tw.addTree(new AdaptableFileTreeIterator(repo,
				ResourcesPlugin.getWorkspace().getRoot()));
		int dirCacheIteratorNth = tw.addTree(new DirCacheIterator(repo.readDirCache()));
		IndexDiffFilter idf = new IndexDiffFilter(dirCacheIteratorNth, ftIndex, true);

		if (pathFilter != null)
			tw.setFilter(AndTreeFilter.create(pathFilter, idf));
		else
			tw.setFilter(idf);

		return tw;
	}

}
