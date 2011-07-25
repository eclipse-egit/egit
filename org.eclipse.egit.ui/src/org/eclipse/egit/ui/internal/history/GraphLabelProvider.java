/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.eclipse.egit.core.UtilCommit;
import org.eclipse.egit.core.UtilWalk;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.util.RelativeDateFormatter;
import org.eclipse.swt.graphics.Image;

class GraphLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {
	private static Repository repo;

	private static final int CUTOFF_DATE_SLOP = 86400; // 1 day

	/* How many generations are maximally preferred over _one_ merge traversal? */
	private static final int MERGE_TRAVERSAL_WEIGHT = 65535;

	private static UtilWalk revWalk;

	private final DateFormat absoluteFormatter;

	private RevCommit lastCommit;

	private PersonIdent lastAuthor;

	private PersonIdent lastCommitter;

	private boolean relativeDate;

	GraphLabelProvider() {
		absoluteFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$
	}

	private String getNameRev(RevCommit commit) throws MissingObjectException,
			IOException {
		if (repo == null) {
			return ""; //$NON-NLS-1$
		}

		Map<String, Ref> tagsMap = repo.getTags();
		UtilCommit current = (UtilCommit) revWalk.parseCommit(commit);

		for (Map.Entry<String, Ref> entry : tagsMap.entrySet()) {
			RevObject any = revWalk.peel(revWalk.parseAny(entry.getValue()
					.getObjectId()));

			if (!(any instanceof UtilCommit)) {
				continue;
			}

			UtilCommit newTag = (UtilCommit) any;
			fillNames(newTag, entry.getKey(), 0, 0, current.getCommitTime()
					- CUTOFF_DATE_SLOP, revWalk);
		}

		return current.getUtil() == null ? "" : String.valueOf(current.getUtil()); //$NON-NLS-1$
	}

	private void fillNames(UtilCommit commit, String name, int generation,
			int distance, int cutoff, UtilWalk revWalk)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		if (commit.getCommitTime() < cutoff) {
			return;
		}

		NameInfo nameInfo = (NameInfo) commit.getUtil();

		if (nameInfo == null) {
			nameInfo = new NameInfo();
			commit.setUtil(nameInfo);
		} else if (nameInfo.distance <= distance) {
			return;
		}

		nameInfo.name = name;
		nameInfo.distance = distance;
		nameInfo.generation = generation;

		for (int parent_number = 0; parent_number < commit.getParentCount(); parent_number++) {
			UtilCommit parent = (UtilCommit) revWalk.parseCommit(commit
					.getParent(parent_number).getId());

			if (parent_number > 0) {
				String new_name;

				if (generation > 0) {
					new_name = String.format(
							"%s~%d^%d", name, generation, parent_number + 1); //$NON-NLS-1$
				} else {
					new_name = String.format("%s^%d", name, parent_number + 1); //$NON-NLS-1$
				}

				fillNames(parent, new_name, 0, distance
						+ MERGE_TRAVERSAL_WEIGHT, cutoff, revWalk);
			} else {
				fillNames(parent, name, generation + 1, distance + 1, cutoff,
						revWalk);
			}
		}
	}

	private String tagOf(final RevCommit c) {
		try {
			return getNameRev(c);
		} catch (Exception e) {
			return ""; //$NON-NLS-1$
		}
	}

	public String getColumnText(final Object element, final int columnIndex) {
		final RevCommit c = (RevCommit) element;
		if (columnIndex == 0)
			return c.getShortMessage();
		if (columnIndex == 3)
			return c.getId().abbreviate(8).name() + "..."; //$NON-NLS-1$
		if (columnIndex == 1 || columnIndex == 2) {
			final PersonIdent author = authorOf(c);
			if (author != null) {
				switch (columnIndex) {
				case 1:
					return author.getName()
							+ " <" + author.getEmailAddress() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
				case 2:
					if (relativeDate)
						return RelativeDateFormatter.format(author.getWhen());
					else
						return absoluteFormatter.format(author.getWhen());
				}
			}
		}
		if (columnIndex == 4) {
			final PersonIdent committer = committerOf(c);
			if (committer != null) {
				return committer.getName()
						+ " <" + committer.getEmailAddress() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (columnIndex == 5) {
			return tagOf(c);
		}

		return ""; //$NON-NLS-1$
	}

	private PersonIdent authorOf(final RevCommit c) {
		if (lastCommit != c) {
			lastCommit = c;
			lastAuthor = c.getAuthorIdent();
			lastCommitter = c.getCommitterIdent();
		}
		return lastAuthor;
	}

	private PersonIdent committerOf(final RevCommit c) {
		if (lastCommit != c) {
			lastCommit = c;
			lastAuthor = c.getAuthorIdent();
			lastCommitter = c.getCommitterIdent();
		}
		return lastCommitter;
	}

	public Image getColumnImage(final Object element, final int columnIndex) {
		return null;
	}

	/**
	 * @param relative {@code true} if the date column should show relative dates
	 * @return {@code true} if the value was changed in this call
	 */
	public boolean setRelativeDate(boolean relative) {
		if (relative == relativeDate)
			return false;
		relativeDate = relative;
		return true;
	}

	public static void setRepo(Repository repo) {
		if (GraphLabelProvider.repo != repo) {
			GraphLabelProvider.repo = repo;
			if (revWalk != null)
				revWalk.release();
			revWalk = new UtilWalk(repo);
		}
	}

	private class NameInfo {
		private String name;

		private int generation;

		private int distance;

		@Override
		public String toString() {
			if (generation == 0) {
				return name;
			} else {
				return String.format("%s~%d", name, generation); //$NON-NLS-1$
			}
		}
	}
}
