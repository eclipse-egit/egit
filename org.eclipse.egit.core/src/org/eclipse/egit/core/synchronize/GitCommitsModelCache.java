/*******************************************************************************
 * Copyright (C) 2011, 2012 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	 * Constant copied from org.eclipse.compare.structuremergeviewer.Differencer.LEFT
	 * in order to avoid UI dependencies introduced by the org.eclipse.compare bundle
	 */
	public static final int LEFT = 4;

	/**
	 * Constant copied from org.eclipse.compare.structuremergeviewer.Differencer.RIGHT
	 * in order to avoid UI dependencies introduced by the org.eclipse.compare bundle
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
				change.append("OUTGOING "); //$NON-NLS-1$
			else
				// should be RIGHT
				change.append("INCOMING "); //$NON-NLS-1$
			if ((kind & ADDITION) != 0)
				change.append("ADDITION "); //$NON-NLS-1$
			else if ((kind & DELETION) != 0)
				change.append("DELETION "); //$NON-NLS-1$
			else
				// should be CHANGE
				change.append("CHANGE "); //$NON-NLS-1$

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
	 *            RevCommit id that git history traverse will start from
	 * @param dstId
	 *            RevCommit id that git history traverse will end
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
			return new ArrayList<Commit>(0);

		final RevWalk rw = new RevWalk(repo);

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

		List<Commit> result = new ArrayList<Commit>();
		for (RevCommit revCommit : rw) {
			if (revCommit.hasAll(allFlags))
				break;

			Commit commit = new Commit();
			commit.shortMessage = revCommit.getShortMessage();
			commit.commitId = AbbreviatedObjectId.fromObjectId(revCommit);
			commit.authorName = revCommit.getAuthorIdent().getName();
			commit.committerName = revCommit.getCommitterIdent().getName();
			commit.commitDate = revCommit.getAuthorIdent().getWhen();

			RevCommit actualCommit, parentCommit;
			if (revCommit.has(localFlag)) {
				actualCommit = revCommit;
				parentCommit = getParentCommit(revCommit);
				commit.direction = RIGHT;
			} else if (revCommit.has(remoteFlag)) {
				actualCommit = getParentCommit(revCommit);
				parentCommit = revCommit;
				commit.direction = LEFT;
			} else
				throw new GitCommitsModelDirectionException();

			commit.children = getChangedObjects(repo, actualCommit,
					parentCommit, pathFilter, commit.direction);

			if (commit.children != null)
				result.add(commit);
		}
		rw.dispose();

		return result;
	}

	private static RevCommit getParentCommit(RevCommit commit) {
		if (commit.getParents().length > 0)
			return commit.getParents()[0];
		else
			return null;
	}

	private static Map<String, Change> getChangedObjects(Repository repo,
			RevCommit parentCommit, RevCommit remoteCommit,
			TreeFilter pathFilter, final int direction) throws IOException {
		final TreeWalk tw = new TreeWalk(repo);
		addTreeFilter(tw, parentCommit);
		addTreeFilter(tw, remoteCommit);

		tw.setRecursive(true);
		if (pathFilter == null)
			tw.setFilter(ANY_DIFF);
		else
			tw.setFilter(AndTreeFilter.create(ANY_DIFF, pathFilter));

		final int localTreeId = direction == LEFT ? 1 : 0;
		final int remoteTreeId = direction == LEFT ? 0 : 1;
		final Map<String, Change> result = new HashMap<String, GitCommitsModelCache.Change>();
		final AbbreviatedObjectId actualCommit = getAbbreviatedObjectId(parentCommit);
		final AbbreviatedObjectId remoteCommitAbb = getAbbreviatedObjectId(remoteCommit);

		MutableObjectId idBuf = new MutableObjectId();
		while (tw.next()) {
			Change change = new Change();
			change.commitId = actualCommit;
			change.remoteCommitId = remoteCommitAbb;
			change.name = tw.getNameString();
			tw.getObjectId(idBuf, localTreeId);
			change.objectId = AbbreviatedObjectId.fromObjectId(idBuf);
			tw.getObjectId(idBuf, remoteTreeId);
			change.remoteObjectId = AbbreviatedObjectId.fromObjectId(idBuf);

			calculateAndSetChangeKind(direction, change);

			result.put(tw.getPathString(), change);
		}
		tw.release();

		return result.size() > 0 ? result : null;
	}

	private static void addTreeFilter(TreeWalk tw, RevCommit commit)
			throws IOException {
		if (commit != null)
			tw.addTree(commit.getTree());
		else
			tw.addTree(new EmptyTreeIterator());
	}

	private static AbbreviatedObjectId getAbbreviatedObjectId(RevCommit commit) {
		if (commit != null)
			return AbbreviatedObjectId.fromObjectId(commit);
		else
			return ZERO_ID;
	}

	static void calculateAndSetChangeKind(final int direction, Change change) {
		if (ZERO_ID.equals(change.objectId)) { // missing locally
			change.objectId = null; // clear zero id;
			if (direction == LEFT)
				change.kind = direction | ADDITION;
			else // should be Differencer.RIGHT
				change.kind = direction | DELETION;
		} else if (ZERO_ID.equals(change.remoteObjectId)) { // missing remotely
			change.remoteObjectId = null; // clear zero id;
			if (direction == LEFT)
				change.kind = direction | DELETION;
			else // should be Differencer.RIGHT
				change.kind = direction | ADDITION;
		} else
			change.kind = direction | CHANGE;
	}

}
