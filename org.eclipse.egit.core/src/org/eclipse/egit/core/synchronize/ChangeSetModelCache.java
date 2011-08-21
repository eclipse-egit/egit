/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.compare.structuremergeviewer.Differencer.LEFT;
import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;
import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevFlagSet;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 *
 */
public class ChangeSetModelCache {

	/**
	 * Corresponds to {@link RevCommit} object, but contains only those data
	 * that are required by Synchronize view Change Set
	 */
	public static class Commit {
		int direction;

		String shortMessage;

		AbbreviatedObjectId commitId;

		Change[] children;

		private Commit() {
			// reduce the visibility of the default constructor
		}

		/**
		 * Indicates does this commit is incoming or outgoing. Returned value
		 * corresponds to {@link Differencer#LEFT} for incoming and
		 * {@link Differencer#RIGHT} for outgoing changes
		 *
		 * @return change direction
		 */
		public int getDirection() {
			return direction;
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
		public Change[] getChildren() {
			return children;
		}
	}

	/**
	 * Describes single tree or blob change in commit.
	 */
	public static class Change {
		int kind;

		String name;

		Change[] children;

		AbbreviatedObjectId objectId;

		private Change() {
			// reduce the visibility of the default constructor
		}

		/**
		 * Describes does this change in incoming/outgoing addition, deletion or
		 * change.
		 *
		 * It uses static values of LEFT, RIGHT, ADDITION, DELETION, CHANGE from
		 * {@link Differencer} class.
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
		 * @return object id
		 */
		public AbbreviatedObjectId getObjectId() {
			return objectId;
		}

		/**
		 * @return {@code true} when this change is tree and have children,
		 *         {@code false} otherwise
		 */
		public boolean isTree() {
			return children != null;
		}

		/**
		 * @return list of children (if change is a tree) or {@code null} when
		 *         there is no children (this could also means that this change
		 *         corresponds to blob object)
		 */
		public Change[] getChildren() {
			return children;
		}
	}

	/**
	 * Scans given {@code repo} and build list of commits between two given
	 * RevCommit objectId's. Each commit contains list of changed resources
	 *
	 * @param repo
	 *            repository that should be scanned
	 * @param srcId
	 *            source RevCommit id
	 * @param dstId
	 *            destination RevCommit id
	 * @return list of {@link Commit} object's between {@code srcId} and
	 *         {@code dstId}
	 * @throws IOException
	 */
	public static List<Commit> build(Repository repo, ObjectId srcId,
			ObjectId dstId) throws IOException {
		if (dstId.equals(srcId))
			return new ArrayList<Commit>(0);

		RevWalk rw = new RevWalk(repo);

		RevFlag localFlag = rw.newFlag("local"); //$NON-NLS-1$
		RevFlag remoteFlag = rw.newFlag("remote"); //$NON-NLS-1$
		RevFlagSet allFlags = new RevFlagSet();
		allFlags.add(localFlag);
		allFlags.add(remoteFlag);
		rw.carry(allFlags);

		RevCommit srcCommit = rw.parseCommit(srcId);
		srcCommit.add(localFlag);
		rw.markStart(srcCommit);

		RevCommit dstCommit = rw.parseCommit(dstId);
		dstCommit.add(remoteFlag);
		rw.markStart(dstCommit);

		ObjectId previousTree = null;

		List<Commit> result = new ArrayList<Commit>();
		for (RevCommit nextCommit : rw) {
			if (nextCommit.hasAll(allFlags))
				break;

			Commit commit = new Commit();
			commit.shortMessage = nextCommit.getShortMessage();
			commit.commitId = AbbreviatedObjectId.fromObjectId(nextCommit);

			if (nextCommit.has(localFlag))
				commit.direction = RIGHT;
			else if (nextCommit.has(remoteFlag))
				commit.direction = LEFT;
			else
				// should never happen
				continue;

			if (previousTree == null)
				previousTree = calculateAncestor(repo, nextCommit).getTree();

			commit.children = getChangedObjects(repo, nextCommit, previousTree,
					commit.direction);

			result.add(commit);
			previousTree = nextCommit.getTree();
		}
		rw.dispose();

		return result;
	}

	private static Change[] getChangedObjects(Repository repo,
			RevCommit currentCommit, ObjectId previousCommitTree, int direction)
			throws IOException {
		TreeWalk tw = new TreeWalk(repo);
		tw.addTree(currentCommit.getTree());
		tw.addTree(previousCommitTree);
		tw.setFilter(TreeFilter.ANY_DIFF);

		int localTreeId = direction == LEFT ? 0 : 1;
		int remoteTreeId = direction == LEFT ? 1 : 0;
		List<Change> result = new ArrayList<Change>();
		Stack<Change> parents = new Stack<Change>();
		Stack<List<Change>> children = new Stack<List<Change>>();

		int deep = 0;
		while (tw.next()) {
			if (deep > tw.getDepth()) {
				Change parentChange = parents.pop();
				parentChange.children = result
						.toArray(new Change[result.size()]);
				result = children.pop();
				deep = tw.getDepth();
			}
			Change change = new Change();
			change.name = tw.getNameString();
			change.objectId = AbbreviatedObjectId.fromObjectId(tw
					.getObjectId(localTreeId));
			if (zeroId().equals(tw.getObjectId(localTreeId)))
				change.kind = direction | Differencer.DELETION;
			else if (zeroId().equals(tw.getObjectId(remoteTreeId)))
				change.kind = direction | Differencer.ADDITION;
			else
				change.kind = direction | Differencer.CHANGE;

			result.add(change);
			if (tw.isSubtree()) {
				parents.push(change);
				children.push(result);
				result = new ArrayList<Change>();

				tw.enterSubtree();
				deep = tw.getDepth();
			}

		}
		if (children.size() > 0) {
			Change parentChange = parents.pop();
			parentChange.children = result.toArray(new Change[result.size()]);
			result = children.pop();
		}

		return result.size() > 0 ? result.toArray(new Change[result.size()])
				: null;
	}

	private static RevCommit calculateAncestor(Repository repo, RevCommit actual)
			throws IOException {
		RevWalk rw = new RevWalk(repo);
		rw.setRevFilter(RevFilter.MERGE_BASE);

		for (RevCommit parent : actual.getParents()) {
			RevCommit parentCommit = rw.parseCommit(parent.getId());
			rw.markStart(parentCommit);
		}

		rw.markStart(rw.parseCommit(actual.getId()));

		RevCommit result = rw.next();
		// result will never be null, in worst case RevWalk will return given
		// commit as ancestor
		return result;
	}

}
