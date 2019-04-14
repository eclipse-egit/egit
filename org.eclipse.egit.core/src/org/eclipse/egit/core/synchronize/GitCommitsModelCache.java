/*******************************************************************************
 * Copyright (C) 2011, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.ObjectId.zeroId;
import static org.eclipse.jgit.treewalk.filter.TreeFilter.ANY_DIFF;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevFlagSet;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Retrieves list of commits and the changes associated with each commit
 */
public class GitCommitsModelCache {

	/**
	 * Constant copied from org.eclipse.compare.structuremergeviewer.Differencer.ADDITION
	 * in order to avoid UI dependencies introduced by the org.eclipse.compare bundle
	 */
	public static final int ADDITION = 1;

	/**
	 * Constant copied from org.eclipse.compare.structuremergeviewer.Differencer.DELETION
	 * in order to avoid UI dependencies introduced by the org.eclipse.compare bundle
	 */
	public static final int DELETION = 2;

	/**
	 * Constant copied from org.eclipse.compare.structuremergeviewer.Differencer.CHANGE
	 * in order to avoid UI dependencies introduced by the org.eclipse.compare bundle
	 */
	public static final int CHANGE = 3;

	/**
	 * Bit mask (value 3) for extracting the kind of difference.
	 */
	private static final int CHANGE_TYPE_MASK = 3;

	/**
	 * Constant copied from org.eclipse.compare.structuremergeviewer.Differencer.LEFT
	 * in order to avoid UI dependencies introduced by the org.eclipse.compare bundle
	 * <p>
	 * Note that in the context of synchronization, this means "INCOMING", shown
	 * by an arrow pointing from right to left.
	 */
	public static final int LEFT = 4;

	/**
	 * Constant copied from org.eclipse.compare.structuremergeviewer.Differencer.RIGHT
	 * in order to avoid UI dependencies introduced by the org.eclipse.compare bundle
	 * <p>
	 * Note that in the context of synchronization, this means "OUTGOING", shown
	 * by an arrow pointing from left to right.
	 */
	public static final int RIGHT = 8;

	/**
	 * Corresponds to {@link RevCommit} object, but contains only those data
	 * that are required by Synchronize view Change Set
	 */
	public static class Commit {
		private int direction;

		private String shortMessage;

		private AbbreviatedObjectId commitId;

		private Date commitDate;

		private String authorName;

		private String committerName;

		private Map<String, Change> children;

		private Commit() {
			// reduce the visibility of the default constructor
		}

		/**
		 * Indicates if this commit is incoming or outgoing. Returned value
		 * corresponds to org.eclipse.compare.structuremergeviewer.Differencer#LEFT for incoming and
		 * org.eclipse.compare.structuremergeviewer.Differencer#RIGHT for outgoing changes
		 *
		 * @return change direction
		 */
		public int getDirection() {
			return direction;
		}

		/**
		 * @return commit id
		 */
		public AbbreviatedObjectId getId() {
			return commitId;
		}

		/**
		 * @return commit author
		 */
		public String getAuthorName() {
			return authorName;
		}

		/**
		 * @return the committer name
		 */
		public String getCommitterName() {
			return committerName;
		}

		/**
		 * @return commit date
		 */
		public Date getCommitDate() {
			return commitDate;
		}

		/**
		 * @return commit short message
		 */
		public String getShortMessage() {
			return shortMessage;
		}

		/**
		 * @return list of changes made by this commit or {@code null} when
		 *         commit doesn't have any changes
		 */
		public Map<String, Change> getChildren() {
			return children;
		}

		/**
		 * Disposes nested resources
		 */
		public void dispose() {
			children.clear();
		}

	}

	/**
	 * Describes single tree or blob change in commit.
	 */
	public static class Change {
		int kind;

		String name;

		AbbreviatedObjectId objectId;

		AbbreviatedObjectId commitId;

		AbbreviatedObjectId remoteCommitId;

		AbbreviatedObjectId remoteObjectId;

		Change() {
			// reduce the visibility of the default constructor
		}

		/**
		 * Describes if this change is incoming/outgoing addition, deletion or
		 * change.
		 *
		 * It uses static values of LEFT, RIGHT, ADDITION, DELETION, CHANGE from
		 * org.eclipse.compare.structuremergeviewer.Differencer class.
		 *
		 * @return kind
		 */
		public int getKind() {
			return kind;
		}

		/**
		 * @return object name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return id of commit containing this change
		 */
		public AbbreviatedObjectId getCommitId() {
			return commitId;
		}

		/**
		 * @return id of parent commit
		 */
		public AbbreviatedObjectId getRemoteCommitId() {
			return remoteCommitId;
		}

		/**
		 * @return object id
		 */
		public AbbreviatedObjectId getObjectId() {
			return objectId;
		}

		/**
		 * @return remote object id
		 */
		public AbbreviatedObjectId getRemoteObjectId() {
			return remoteObjectId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((objectId == null) ? 0 : objectId.hashCode());
			result = prime
					* result
					+ ((remoteObjectId == null) ? 0 : remoteObjectId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Change other = (Change) obj;
			if (objectId == null) {
				if (other.objectId != null)
					return false;
			} else if (!objectId.equals(other.objectId))
				return false;
			if (remoteObjectId == null) {
				if (other.remoteObjectId != null)
					return false;
			} else if (!remoteObjectId.equals(other.remoteObjectId))
				return false;
			return true;
		}

		@Override
		public String toString() {
			StringBuilder change = new StringBuilder("Change("); //$NON-NLS-1$
			if ((kind & LEFT) != 0)
				change.append("INCOMING "); //$NON-NLS-1$
			else
				// should be RIGHT
				change.append("OUTGOING "); //$NON-NLS-1$
			int changeType = kind & CHANGE_TYPE_MASK;
			if (changeType == CHANGE)
				change.append("CHANGE "); //$NON-NLS-1$
			else if (changeType == ADDITION)
				change.append("ADDITION "); //$NON-NLS-1$
			else if (changeType == DELETION)
				change.append("DELETION "); //$NON-NLS-1$

			change.append(name);
			change.append(";\n\tcurrent objectId: "); //$NON-NLS-1$
			change.append(getObjectId(objectId));
			change.append(";\n\tparent objectId: "); //$NON-NLS-1$
			change.append(getObjectId(remoteObjectId));
			change.append(";\n\tcurrent commit: "); //$NON-NLS-1$
			change.append(getObjectId(commitId));
			change.append(";\n\tparent commit: "); //$NON-NLS-1$
			change.append(getObjectId(remoteCommitId));
			change.append("\n)"); //$NON-NLS-1$

			return change.toString();
		}

		private String getObjectId(AbbreviatedObjectId object) {
			if (object != null)
				return object.toObjectId().getName();
			else
				return ObjectId.zeroId().getName();
		}

	}

	static final AbbreviatedObjectId ZERO_ID = AbbreviatedObjectId
			.fromObjectId(zeroId());

	/**
	 * Scans given {@code repo} and build list of commits between two given
	 * RevCommit objectId's. Each commit contains list of changed resources
	 *
	 * @param repo
	 *            repository that should be scanned
	 * @param srcId
	 *            commit id that is considered the "local" version (e.g. from
	 *            master)
	 * @param dstId
	 *            commit id that is considered the "remote" version (e.g. from
	 *            origin/master)
	 * @param pathFilter
	 *            path filter definition or {@code null} when all paths should
	 *            be included
	 * @return list of {@link Commit} object's between {@code srcId} and
	 *         {@code dstId}
	 * @throws IOException
	 */
	public static List<Commit> build(Repository repo, ObjectId srcId,
			ObjectId dstId, TreeFilter pathFilter) throws IOException {
		if (dstId.equals(srcId))
			return new ArrayList<>(0);

		try (RevWalk rw = new RevWalk(repo)) {

			final RevFlag localFlag = rw.newFlag("local"); //$NON-NLS-1$
			final RevFlag remoteFlag = rw.newFlag("remote"); //$NON-NLS-1$
			final RevFlagSet allFlags = new RevFlagSet();
			allFlags.add(localFlag);
			allFlags.add(remoteFlag);
			rw.carry(allFlags);

			RevCommit srcCommit = rw.parseCommit(srcId);
			srcCommit.add(localFlag);
			rw.markStart(srcCommit);
			srcCommit = null; // free not needed resources

			RevCommit dstCommit = rw.parseCommit(dstId);
			dstCommit.add(remoteFlag);
			rw.markStart(dstCommit);
			dstCommit = null; // free not needed resources

			if (pathFilter != null)
				rw.setTreeFilter(pathFilter);

			List<Commit> result = new ArrayList<>();
			for (RevCommit revCommit : rw) {
				if (revCommit.hasAll(allFlags))
					break;

				Commit commit = new Commit();
				commit.shortMessage = revCommit.getShortMessage();
				commit.commitId = AbbreviatedObjectId.fromObjectId(revCommit);
				commit.authorName = revCommit.getAuthorIdent().getName();
				commit.committerName = revCommit.getCommitterIdent().getName();
				commit.commitDate = revCommit.getAuthorIdent().getWhen();

				RevCommit parentCommit = getParentCommit(revCommit);
				if (revCommit.has(localFlag))
					// Outgoing
					commit.direction = RIGHT;
				else if (revCommit.has(remoteFlag))
					// Incoming
					commit.direction = LEFT;
				else
					throw new GitCommitsModelDirectionException();

				commit.children = getChangedObjects(repo, revCommit,
						parentCommit, pathFilter, commit.direction);

				if (commit.children != null)
					result.add(commit);
			}
			rw.dispose();
			return result;
		}
	}

	private static RevCommit getParentCommit(RevCommit commit) {
		if (commit.getParents().length > 0)
			return commit.getParents()[0];
		else
			return null;
	}

	private static Map<String, Change> getChangedObjects(Repository repo,
			RevCommit commit, RevCommit parentCommit,
			TreeFilter pathFilter, final int direction) throws IOException {
		final Map<String, Change> result = new HashMap<>();
		try (final TreeWalk tw = new TreeWalk(repo)) {
			int commitIndex = addTree(tw, commit);
			int parentCommitIndex = addTree(tw, parentCommit);

			tw.setRecursive(true);
			if (pathFilter == null)
				tw.setFilter(ANY_DIFF);
			else
				tw.setFilter(AndTreeFilter.create(ANY_DIFF, pathFilter));

			final AbbreviatedObjectId commitId = getAbbreviatedObjectId(commit);
			final AbbreviatedObjectId parentCommitId = getAbbreviatedObjectId(parentCommit);

			MutableObjectId idBuf = new MutableObjectId();
			while (tw.next()) {
				Change change = new Change();
				change.commitId = commitId;
				change.remoteCommitId = parentCommitId;
				change.name = tw.getNameString();
				tw.getObjectId(idBuf, commitIndex);
				change.objectId = AbbreviatedObjectId.fromObjectId(idBuf);
				tw.getObjectId(idBuf, parentCommitIndex);
				change.remoteObjectId = AbbreviatedObjectId.fromObjectId(idBuf);

				calculateAndSetChangeKind(direction, change);

				result.put(tw.getPathString(), change);
			}
		}

		return result.size() > 0 ? result : null;
	}

	private static int addTree(TreeWalk tw, RevCommit commit)
			throws IOException {
		if (commit != null)
			return tw.addTree(commit.getTree());
		else
			return tw.addTree(new EmptyTreeIterator());
	}

	private static AbbreviatedObjectId getAbbreviatedObjectId(RevCommit commit) {
		if (commit != null)
			return AbbreviatedObjectId.fromObjectId(commit);
		else
			return ZERO_ID;
	}

	static void calculateAndSetChangeKind(final int direction, Change change) {
		if (ZERO_ID.equals(change.objectId)) { // removed in commit
			change.objectId = null; // clear zero id;
			change.kind = direction | DELETION;
		} else if (ZERO_ID.equals(change.remoteObjectId)) { // added in commit
			change.remoteObjectId = null; // clear zero id;
			change.kind = direction | ADDITION;
		} else
			change.kind = direction | CHANGE;
	}

}
