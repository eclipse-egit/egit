/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Based on {@link org.eclipse.jgit.diff.DiffEntry}. Represents change to a file
 * with additional information about change direction.
 */
public final class ThreeWayDiffEntry {

	/** Magical SHA1 used for file adds or deletes */
	static final AbbreviatedObjectId A_ZERO = AbbreviatedObjectId
			.fromObjectId(ObjectId.zeroId());

	/** General type of change a single file-level patch describes. */
	public static enum ChangeType {
		/** Add a new file to the project */
		ADD,

		/** Modify an existing file in the project (content and/or mode) */
		MODIFY,

		/** Delete an existing file from the project */
		DELETE;
	}

	/**	Change direction */
	public static enum Direction {
		/**
		 *
		 */
		INCOMING,

		/**
		 *
		 */
		OUTGOING,

		/** */
		CONFLICTING;
	}

	private ThreeWayDiffEntry() {
		// reduce the visibility of the default constructor
	}

	/**
	 * Converts the TreeWalk into TreeWayDiffEntry headers.
	 *
	 * @param walk
	 *            the TreeWalk to walk through. Must have exactly three trees in
	 *            this order: local, base and remote and can't be recursive.
	 * @return headers describing the changed file.
	 * @throws IOException
	 *             the repository cannot be accessed.
	 * @throws IllegalArgumentException
	 *             when {@code walk} doen't have exactly three trees, or when
	 *             {@code walk} is recursive
	 */
	public static List<ThreeWayDiffEntry> scan(TreeWalk walk)
		throws IOException {
		if (walk.getTreeCount() != 3)
			throw new IllegalArgumentException(
					"TreeWalk need to have exactly three trees"); //$NON-NLS-1$
		if (walk.isRecursive())
			throw new IllegalArgumentException(
					"TreeWalk shouldn't be recursive."); //$NON-NLS-1$

		List<ThreeWayDiffEntry> r = new ArrayList<ThreeWayDiffEntry>();
		MutableObjectId idBuf = new MutableObjectId();
		while (walk.next()) {
			ThreeWayDiffEntry e = new ThreeWayDiffEntry();

			walk.getObjectId(idBuf, 0);
			e.localId = AbbreviatedObjectId.fromObjectId(idBuf);

			walk.getObjectId(idBuf, 1);
			e.baseId = AbbreviatedObjectId.fromObjectId(idBuf);

			walk.getObjectId(idBuf, 2);
			e.remoteId = AbbreviatedObjectId.fromObjectId(idBuf);

			boolean localSameAsBase = e.localId.equals(e.baseId);
			if (!A_ZERO.equals(e.localId) && localSameAsBase
					&& e.baseId.equals(e.remoteId))
				continue;

			e.path = walk.getPathString();
			boolean localIsMissing = walk.getFileMode(0) == FileMode.MISSING;
			boolean baseIsMissing = walk.getFileMode(1) == FileMode.MISSING;
			boolean remoteIsMissing = walk.getFileMode(2)  == FileMode.MISSING;

			if (localIsMissing || baseIsMissing || remoteIsMissing) {
				if (!localIsMissing && baseIsMissing && remoteIsMissing) {
					e.direction = Direction.OUTGOING;
					e.changeType = ChangeType.ADD;
				} else if (localIsMissing && baseIsMissing && !remoteIsMissing) {
					e.direction = Direction.INCOMING;
					e.changeType = ChangeType.ADD;
				} else if (!localIsMissing && !baseIsMissing && remoteIsMissing) {
					e.direction = Direction.INCOMING;
					e.changeType = ChangeType.DELETE;
				} else if (localIsMissing && !baseIsMissing && !remoteIsMissing) {
					e.direction = Direction.OUTGOING;
					e.changeType = ChangeType.DELETE;
				} else {
					e.direction = Direction.CONFLICTING;
					e.changeType = ChangeType.MODIFY;
				}
			} else {
				if (localSameAsBase && !e.localId.equals(e.remoteId))
					e.direction = Direction.INCOMING;
				else if (e.remoteId.equals(e.baseId)
						&& !e.remoteId.equals(e.localId))
					e.direction = Direction.OUTGOING;
				else
					e.direction = Direction.CONFLICTING;

				e.changeType = ChangeType.MODIFY;
			}

			r.add(e);
			if (walk.isSubtree()) {
				e.isTree = true;
				walk.enterSubtree();
			}
		}

		return r;
	}

	private String path;

	private ChangeType changeType;

	private Direction direction;

	private AbbreviatedObjectId localId;

	private AbbreviatedObjectId baseId;

	private AbbreviatedObjectId remoteId;

	private boolean isTree = false;

	/**
	 * @return base id
	 */
	public AbbreviatedObjectId getBaseId() {
		return baseId;
	}

	/**
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return {@code true} if entry represents tree, {@code false} otherwise
	 */
	public boolean isTree() {
		return isTree;
	}

	/** @return the type of change this patch makes on {@link #getPath()} */
	public ChangeType getChangeType() {
		return changeType;
	}

	/**
	 * Get the old object id from the <code>index</code>.
	 *
	 * @return the object id; null if there is no index line
	 */
	public AbbreviatedObjectId getLocalId() {
		return localId;
	}

	/**
	 * Get the new object id from the <code>index</code>.
	 *
	 * @return the object id; null if there is no index line
	 */
	public AbbreviatedObjectId getRemoteId() {
		return remoteId;
	}

	/**
	 * @return direction
	 */
	public Direction getDirection() {
		return direction;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("ThreeDiffEntry["); //$NON-NLS-1$
		buf.append(changeType).append(" ").append(path); //$NON-NLS-1$
		buf.append("]"); //$NON-NLS-1$

		return buf.toString();
	}

}
