/*******************************************************************************
 * Copyright (C) 2011, 2017 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
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
	private static final AbbreviatedObjectId A_ZERO = AbbreviatedObjectId
			.fromObjectId(ObjectId.zeroId());

	/** General type of change a single file-level patch describes. */
	public static enum ChangeType {
		/** Add a new file to the project */
		ADD,

		/** Modify an existing file in the project (content and/or mode) */
		MODIFY,

		/** Delete an existing file from the project */
		DELETE,

		/** Resource is in sync */
		IN_SYNC;
	}

	/** Change direction */
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

	ThreeWayDiffEntry() {
		// reduce the visibility of the default constructor
	}

	/**
	 * Convert the TreeWalk into {@link ThreeWayDiffEntry} instances.
	 *
	 * @param walk
	 *            the TreeWalk to walk through. Must have 3 or 4 trees in this
	 *            order: local, base, remote, optionally a DirCacheIterator, and
	 *            can't be recursive.
	 * @return A list, never null but possibly empty, of
	 *         {@link ThreeWayDiffEntry} describing the changed file.
	 * @throws IOException
	 *             the repository cannot be accessed.
	 * @throws IllegalArgumentException
	 *             when {@code walk} doen't have 3 or 4 trees, or when
	 *             {@code walk} is recursive
	 */
	public static @NonNull List<ThreeWayDiffEntry> scan(TreeWalk walk)
			throws IOException {
		return scan(walk, null);
	}

	/**
	 * Convert the TreeWalk into {@link ThreeWayDiffEntry} instances.
	 *
	 * @param walk
	 *            the TreeWalk to walk through. Must have 3 or 4 trees in this
	 *            order: local, base, remote, optionally a DirCacheIterator, and
	 *            can't be recursive.
	 * @param gsd
	 *            The {@link GitSynchronizeData} that contains info about the
	 *            synchronization configuration and scope.
	 * @return A list, never null but possibly empty, of
	 *         {@link ThreeWayDiffEntry} describing the changed file.
	 * @throws IOException
	 *             the repository cannot be accessed.
	 * @throws IllegalArgumentException
	 *             when {@code walk} doen't have 3 or 4 trees, or when
	 *             {@code walk} is recursive
	 */
	public static @NonNull List<ThreeWayDiffEntry> scan(TreeWalk walk,
			GitSynchronizeData gsd) throws IOException {
		if (walk.getTreeCount() != 3 && walk.getTreeCount() != 4)
			throw new IllegalArgumentException(
					"TreeWalk need to have three or four trees"); //$NON-NLS-1$
		if (walk.isRecursive())
			throw new IllegalArgumentException(
					"TreeWalk shouldn't be recursive."); //$NON-NLS-1$

		List<ThreeWayDiffEntry> r = new ArrayList<>();
		MutableObjectId idBuf = new MutableObjectId();
		NeedEntry needEntry = new NeedEntry(gsd);
		while (walk.next()) {
			ThreeWayDiffEntry e = new ThreeWayDiffEntry();

			walk.getObjectId(idBuf, 0);
			e.localId = AbbreviatedObjectId.fromObjectId(idBuf);

			walk.getObjectId(idBuf, 1);
			e.baseId = AbbreviatedObjectId.fromObjectId(idBuf);

			walk.getObjectId(idBuf, 2);
			e.remoteId = AbbreviatedObjectId.fromObjectId(idBuf);

			if (!walk.isSubtree()) {
				e.metadata = new CheckoutMetadata(
						walk.getEolStreamType(
								TreeWalk.OperationType.CHECKOUT_OP),
						walk.getFilterCommand(
								Constants.ATTR_FILTER_TYPE_SMUDGE));
			}

			boolean localSameAsBase = e.localId.equals(e.baseId);
			if (!A_ZERO.equals(e.localId) && localSameAsBase
					&& e.baseId.equals(e.remoteId)) {
				if (needEntry.apply(walk.getPathString())) {
					e.direction = Direction.INCOMING;
					e.changeType = ChangeType.IN_SYNC;
					e.path = walk.getPathString();
					r.add(e);
				}
				continue;
			}

			e.path = walk.getPathString();
			boolean localIsMissing = walk.getFileMode(0) == FileMode.MISSING;
			boolean baseIsMissing = walk.getFileMode(1) == FileMode.MISSING;
			boolean remoteIsMissing = walk.getFileMode(2) == FileMode.MISSING;

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

	ChangeType changeType;

	AbbreviatedObjectId baseId;

	AbbreviatedObjectId remoteId;

	private String path;

	private Direction direction;

	private AbbreviatedObjectId localId;

	private CheckoutMetadata metadata;

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

	/**
	 * @return the {@link CheckoutMetadata}, or {@code null}.
	 */
	public CheckoutMetadata getMetadata() {
		return metadata;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("ThreeDiffEntry["); //$NON-NLS-1$
		buf.append(changeType).append(" ").append(path); //$NON-NLS-1$
		buf.append("]"); //$NON-NLS-1$

		return buf.toString();
	}

	private static class NeedEntry {
		private final GitSynchronizeData gsd;

		private Set<String> paths;

		public NeedEntry(GitSynchronizeData gsd) {
			this.gsd = gsd;
		}

		boolean apply(String pathString) {
			if (gsd == null) {
				// This means that all paths must be included
				return true;
			}
			if (paths == null) {
				initPaths();
			}
			return paths.contains(pathString);
		}

		private void initPaths() {
			Set<IResource> resources = gsd.getIncludedResources();
			if (resources != null && !resources.isEmpty()) {
				paths = new HashSet<>(resources.size());
				final Path repositoryPath = new Path(gsd.getRepository()
						.getWorkTree().getAbsolutePath());
				for (IResource resource : gsd.getIncludedResources()) {
					IPath resourceLocation = resource.getLocation();
					if (resourceLocation != null) {
						paths.add(resourceLocation.makeRelativeTo(
								repositoryPath).toString());
					}
				}
			} else {
				paths = Collections.emptySet();
			}
		}
	}

}
