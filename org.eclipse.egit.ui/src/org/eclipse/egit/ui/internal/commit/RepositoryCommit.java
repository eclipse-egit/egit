/*******************************************************************************
 *  Copyright (c) 2011, 2017 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Robin Stocker (independent)
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 477248
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.ui.model.WorkbenchAdapter;

/**
 * Class that encapsulates a particular {@link Repository} instance and
 * {@link RevCommit} instance.
 *
 * This class computes and provides access to the {@link FileDiff} objects
 * introduced by the commit.
 */
public class RepositoryCommit extends WorkbenchAdapter
		implements IAdaptable, IRepositoryCommit {

	/**
	 * NAME_LENGTH
	 */
	public static final int NAME_LENGTH = 8;

	private Repository repository;

	private RevCommit commit;

	private FileDiff[] diffs;

	private RepositoryCommitNote[] notes;

	/**
	 * Marks this commit as a stash commit.
	 */
	private boolean stash;

	/**
	 * Create a repository commit
	 *
	 * @param repository
	 * @param commit
	 */
	public RepositoryCommit(Repository repository, RevCommit commit) {
		Assert.isNotNull(repository, "Repository cannot be null"); //$NON-NLS-1$
		Assert.isNotNull(commit, "Commit cannot be null"); //$NON-NLS-1$
		this.repository = repository;
		this.commit = commit;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (Repository.class == adapter) {
			return adapter.cast(repository);
		}
		if (RevCommit.class == adapter) {
			return adapter.cast(commit);
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/**
	 * Abbreviate commit id to {@link #NAME_LENGTH} size.
	 *
	 * @return abbreviated commit id
	 */
	public String abbreviate() {
		return commit.abbreviate(NAME_LENGTH).name();
	}

	/**
	 * Get repository name
	 *
	 * @return repo name
	 */
	public String getRepositoryName() {
		if (!repository.isBare())
			return repository.getDirectory().getParentFile().getName();
		else
			return repository.getDirectory().getName();
	}

	/**
	 * Get repository
	 *
	 * @return repository
	 */
	@Override
	public Repository getRepository() {
		return repository;
	}

	/**
	 * Get rev commit
	 *
	 * @return rev commit
	 */
	@Override
	public RevCommit getRevCommit() {
		return commit;
	}

	/**
	 * Get the changes between this commit and all parent commits
	 *
	 * @return non-null but possibly empty array of {@link FileDiff} instances.
	 */
	public FileDiff[] getDiffs() {
		if (diffs == null) {
			RevCommit[] parents = commit.getParents();
			if (isStash() && commit.getParentCount() > 0)
				parents = new RevCommit[] { commit.getParent(0) };

			try (RevWalk revWalk = new RevWalk(repository);
					TreeWalk treewalk = new TreeWalk(revWalk.getObjectReader())) {
				treewalk.setRecursive(true);
				treewalk.setFilter(TreeFilter.ANY_DIFF);
				for (RevCommit parent : commit.getParents())
					revWalk.parseBody(parent);
				diffs = FileDiff.compute(repository, treewalk, commit, parents,
						null, TreeFilter.ALL);
			} catch (IOException e) {
				diffs = new FileDiff[0];
			}
		}
		return diffs;
	}

	/**
	 * Gets the changes between this commit and specific parent commits
	 *
	 * @param parents
	 *            parents to which the current commit is compared
	 *
	 * @return non-null but possibly empty array of {@link FileDiff} instances.
	 */
	public FileDiff[] getDiffs(RevCommit... parents) {
		FileDiff[] diffsResult = null;
		try (RevWalk revWalk = new RevWalk(repository);
				TreeWalk treewalk = new TreeWalk(revWalk.getObjectReader())) {
			treewalk.setRecursive(true);
			treewalk.setFilter(TreeFilter.ANY_DIFF);
			loadParents();
			diffsResult = FileDiff.compute(repository, treewalk, commit,
					parents, null, TreeFilter.ALL);
		} catch (IOException e) {
			diffsResult = new FileDiff[0];
		}
		return diffsResult;
	}

	private void loadParents() throws IOException {
		try (RevWalk revWalk = new RevWalk(repository)) {
			for (RevCommit parent : commit.getParents())
				revWalk.parseBody(parent);
		}
	}

	/**
	 * Get notes for this commit.
	 *
	 * @return non-null but possibly empty array of {@link RepositoryCommitNote}
	 *         instances.
	 */
	public RepositoryCommitNote[] getNotes() {
		if (notes == null) {
			List<RepositoryCommitNote> noteList = new ArrayList<>();
			try {
				Repository repo = getRepository();
				Git git = Git.wrap(repo);
				RevCommit revCommit = getRevCommit();
				for (Ref ref : repo.getRefDatabase()
						.getRefsByPrefix(Constants.R_NOTES)) {
					Note note = git.notesShow().setNotesRef(ref.getName())
							.setObjectId(revCommit).call();
					if (note != null)
						noteList.add(new RepositoryCommitNote(this, ref, note));
				}
				notes = noteList.toArray(new RepositoryCommitNote[noteList
						.size()]);
			} catch (Exception e) {
				Activator.logError("Error showing notes", e); //$NON-NLS-1$
				notes = new RepositoryCommitNote[0];
			}
		}
		return notes;
	}

	@Override
	public Object[] getChildren(Object o) {
		return new Object[0];
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return UIIcons.CHANGESET;
	}

	@Override
	public String getLabel(Object o) {
		return abbreviate();
	}

	@Override
	public Object getParent(Object o) {
		return null;
	}

	/**
	 * @param object
	 * @return styled text
	 */
	@Override
	public StyledString getStyledText(Object object) {
		StyledString styled = new StyledString();
		styled.append(abbreviate());
		styled.append(": "); //$NON-NLS-1$
		styled.append(commit.getShortMessage());

		PersonIdent author = commit.getAuthorIdent();
		PersonIdent committer = commit.getCommitterIdent();
		if (author != null && committer != null) {
			PreferenceBasedDateFormatter formatter = PreferenceBasedDateFormatter
					.create();
			if (author.getName().equals(committer.getName())) {
				styled.append(
						MessageFormat.format(UIText.RepositoryCommit_AuthorDate,
								author.getName(), formatter.formatDate(author)),
						StyledString.QUALIFIER_STYLER);
			} else {
				styled.append(MessageFormat.format(
						UIText.RepositoryCommit_AuthorDateCommitter,
								author.getName(), formatter.formatDate(author),
						committer.getName()), StyledString.QUALIFIER_STYLER);
			}
		}
		return styled;
	}

	/**
	 * Marks this commit as a stash commit.
	 *
	 * @param stash
	 *            true whether this is a stash commit
	 */
	public void setStash(boolean stash) {
		this.stash = stash;
	}

	/**
	 * Whether this is a stash commit.
	 *
	 * @return true if this is a stash commit
	 */
	public boolean isStash() {
		return stash;
	}

}
