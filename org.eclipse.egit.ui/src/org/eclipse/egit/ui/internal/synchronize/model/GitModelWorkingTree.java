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
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;

/**
 * Representation of working tree in EGit ChangeSet model
 */
public class GitModelWorkingTree extends GitModelCache {

	/**
	 * @param parent
	 *            parent of working tree instance
	 * @throws IOException
	 */
	public GitModelWorkingTree(GitModelObject parent)
			throws IOException {
		super(parent, null, new FileModelFactory() {
			public GitModelBlob createFileModel(
					GitModelObjectContainer modelParent, RevCommit modelCommit,
					ObjectId repoId, ObjectId cacheId, IPath location)
					throws IOException {
				return new GitModelWorkingFile(modelParent, modelCommit,
						repoId, location);
			}

			public boolean isWorkingTree() {
				return true;
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

		if (obj == null)
			return false;

		if (obj.getClass() != getClass())
			return false;

		GitModelCache left = (GitModelCache) obj;
		return left.getParent().equals(getParent());
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
		tw.setFilter(idf);

		return tw;
	}

}
