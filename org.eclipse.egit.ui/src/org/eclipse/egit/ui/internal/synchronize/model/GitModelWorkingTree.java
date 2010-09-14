/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.jgit.treewalk.filter.TreeFilter.ANY_DIFF;

import java.io.IOException;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;

/**
 * Representation of working tree in EGit ChangeSet model
 */
public class GitModelWorkingTree extends GitModelCache {

	/**
	 * @param parent
	 *            parent of working tree instance
	 * @param commit
	 *            last {@link RevCommit} in repository
	 * @throws IOException
	 */
	public GitModelWorkingTree(GitModelObject parent, RevCommit commit)
			throws IOException {
		super(parent, commit, new FileModelFactory() {
			public GitModelBlob createFileModel(
					GitModelObjectContainer modelParent, RevCommit modelCommit,
					ObjectId repoId, ObjectId cacheId, String name)
					throws IOException {
				return new GitModelWorkingFile(modelParent, modelCommit,
						repoId, name);
			}
		});
	}

	@Override
	public String getName() {
		return UIText.GitModelWorkingTree_workingTree;
	}

	@Override
	protected TreeWalk createAndConfigureTreeWalk() throws IOException {
		TreeWalk tw = createTreeWalk();
		tw.setRecursive(true);

		Repository repo = getRepository();
		tw.addTree(new DirCacheIterator(repo.readDirCache()));
		tw.addTree(new FileTreeIterator(repo));
		dirCacheIteratorNth = 0;

		NotIgnoredFilter notIgnoredFilter = new NotIgnoredFilter(1);
		tw.setFilter(AndTreeFilter.create(ANY_DIFF, notIgnoredFilter));

		return tw;
	}

}
