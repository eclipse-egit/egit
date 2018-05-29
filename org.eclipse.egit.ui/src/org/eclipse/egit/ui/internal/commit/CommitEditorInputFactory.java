/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

/**
 * Element factory for saving and restoring the state of a
 * {@link CommitEditorInput} instance.
 */
public class CommitEditorInputFactory implements IElementFactory {

	/**
	 * ID
	 */
	public static final String ID = "org.eclipse.egit.ui.internal.commit.CommitEditorInputFactory"; //$NON-NLS-1$

	/**
	 * COMMIT
	 */
	public static final String COMMIT = "commit"; //$NON-NLS-1$

	/**
	 * PATH
	 */
	public static final String PATH = "path"; //$NON-NLS-1$

	/**
	 * STASH
	 */
	public static final String STASH = "stash"; //$NON-NLS-1$

	/**
	 * Save state of input to memento
	 *
	 * @param memento
	 * @param input
	 */
	public static void saveState(IMemento memento, CommitEditorInput input) {
		RepositoryCommit commit = input.getCommit();
		memento.putString(COMMIT, commit.getRevCommit().name());
		memento.putString(PATH, commit.getRepository().getDirectory()
				.getAbsolutePath());
		memento.putBoolean(STASH, commit.isStash());
	}

	/**
	 * Get repository from memento
	 *
	 * @param memento
	 * @return repository
	 */
	protected Repository getRepository(IMemento memento) {
		String path = memento.getString(PATH);
		if (path == null)
			return null;

		File gitDir = new File(path);
		if (!gitDir.exists())
			return null;

		try {
			return Activator.getDefault().getRepositoryCache()
					.lookupRepository(gitDir);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Get commit from memento and repository
	 *
	 * @param memento
	 * @param repository
	 * @return rev commit
	 */
	protected RepositoryCommit getCommit(IMemento memento, Repository repository) {
		String id = memento.getString(COMMIT);
		if (id == null)
			return null;

		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(ObjectId.fromString(id));
			for (RevCommit parent : commit.getParents())
				walk.parseBody(parent);
			RepositoryCommit repositoryCommit = new RepositoryCommit(
					repository, commit);
			Boolean isStash = memento.getBoolean(STASH);
			if (isStash != null)
				repositoryCommit.setStash(isStash.booleanValue());
			return repositoryCommit;
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	@Override
	public IAdaptable createElement(IMemento memento) {
		Repository repository = getRepository(memento);
		if (repository == null)
			return null;

		RepositoryCommit commit = getCommit(memento, repository);
		if (commit == null)
			return null;

		return new CommitEditorInput(commit);
	}
}
