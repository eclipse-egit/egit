/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
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
public class RepositoryCommit extends WorkbenchAdapter implements IAdaptable {

	private static DateFormat FORMAT = DateFormat.getDateTimeInstance(
			DateFormat.MEDIUM, DateFormat.SHORT);

	/**
	 * Format commit date
	 *
	 * @param date
	 * @return date string
	 */
	public static String formatDate(final Date date) {
		if (date == null)
			return ""; //$NON-NLS-1$
		synchronized (FORMAT) {
			return FORMAT.format(date);
		}
	}

	/**
	 * NAME_LENGTH
	 */
	public static final int NAME_LENGTH = 8;

	private Repository repository;

	private RevCommit commit;

	private FileDiff[] diffs;

	private RepositoryCommitNote[] notes;

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

	public Object getAdapter(Class adapter) {
		if (Repository.class == adapter)
			return repository;

		if (RevCommit.class == adapter)
			return commit;

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
	public Repository getRepository() {
		return repository;
	}

	/**
	 * Get rev commit
	 *
	 * @return rev commit
	 */
	public RevCommit getRevCommit() {
		return commit;
	}

	/**
	 * Get file diffs
	 *
	 * @return non-null but possibly empty array of {@link FileDiff} instances.
	 */
	public FileDiff[] getDiffs() {
		if (diffs == null) {
			RevWalk revWalk = new RevWalk(repository);
			TreeWalk treewalk = new TreeWalk(revWalk.getObjectReader());
			treewalk.setRecursive(true);
			treewalk.setFilter(TreeFilter.ANY_DIFF);
			try {
				for (RevCommit parent : commit.getParents())
					if (parent.getTree() == null)
						revWalk.parseBody(parent);
				diffs = FileDiff.compute(treewalk, commit);
			} catch (IOException e) {
				diffs = new FileDiff[0];
			} finally {
				revWalk.release();
				treewalk.release();
			}
		}
		return diffs;
	}

	/**
	 * Get notes for this commit.
	 *
	 * @return non-null but possibly empty array of {@link RepositoryCommitNote}
	 *         instances.
	 */
	public RepositoryCommitNote[] getNotes() {
		if (notes == null) {
			List<RepositoryCommitNote> noteList = new ArrayList<RepositoryCommitNote>();
			try {
				Repository repo = getRepository();
				Git git = Git.wrap(repo);
				RevCommit revCommit = getRevCommit();
				for (Ref ref : repo.getRefDatabase().getRefs(Constants.R_NOTES)
						.values()) {
					Note note = git.notesShow().setNotesRef(ref.getName())
							.setObjectId(revCommit).call();
					if (note != null)
						noteList.add(new RepositoryCommitNote(this, ref, note));
				}
				notes = noteList.toArray(new RepositoryCommitNote[noteList
						.size()]);
			} catch (IOException e) {
				Activator.logError("Error showing notes", e); //$NON-NLS-1$
				notes = new RepositoryCommitNote[0];
			}
		}
		return notes;
	}

	public Object[] getChildren(Object o) {
		return new Object[0];
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		return UIIcons.CHANGESET;
	}

	public String getLabel(Object o) {
		return abbreviate();
	}

	public Object getParent(Object o) {
		return null;
	}

	public StyledString getStyledText(Object object) {
		StyledString styled = new StyledString();
		styled.append(abbreviate());
		styled.append(": "); //$NON-NLS-1$
		styled.append(commit.getShortMessage());

		PersonIdent person = commit.getAuthorIdent();
		if (person == null)
			person = commit.getCommitterIdent();
		if (person != null)
			styled.append(MessageFormat.format(
					UIText.RepositoryCommit_UserAndDate, person.getName(),
					formatDate(person.getWhen())),
					StyledString.QUALIFIER_STYLER);
		return styled;
	}

}
