/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RenameCallback;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * Class to track all renames encountered during a {@link RevWalk}
 *
 * Note that rename tracking with this class only works for a single file,
 * because it is based on the git log --follow facility.
 *
 * @see FollowFilter
 */
class RenameTracker {

	private final RevFilter filter = new RevFilter() {

		@Override
		public boolean include(final RevWalk walker, final RevCommit commit)
				throws IOException {
			if (currentPath != null)
				renames.put(commit, currentPath);
			else if (currentDiff != null) {
				renames.put(commit, currentDiff.getNewPath());
				currentPath = currentDiff.getOldPath();
				currentDiff = null;
			}
			return true;
		}

		@Override
		public RevFilter clone() {
			return null;
		}
	};

	private final RenameCallback callback = new RenameCallback() {

		@Override
		public void renamed(final DiffEntry entry) {
			currentDiff = entry;
			currentPath = null;
		}
	};

	private String initialPath;

	private DiffEntry currentDiff;

	private String currentPath;

	private Map<RevCommit, String> renames = new LinkedHashMap<>();

	/**
	 * @return filter
	 */
	public RevFilter getFilter() {
		return filter;
	}

	/**
	 * @return callback
	 */
	public RenameCallback getCallback() {
		return callback;
	}

	/**
	 * Get renamed path in target commit
	 *
	 * @param target
	 * @param startingPath
	 * @return path
	 */
	public String getPath(final ObjectId target, final String startingPath) {
		if (!startingPath.equals(initialPath))
			return startingPath;
		String renamed = renames.get(target);
		return renamed != null ? renamed : startingPath;
	}

	/**
	 * Reset the tracker
	 *
	 * @param path
	 * @return this tracker
	 */
	public RenameTracker reset(final String path) {
		renames.clear();
		initialPath = path;
		currentPath = path;
		return this;
	}
}
