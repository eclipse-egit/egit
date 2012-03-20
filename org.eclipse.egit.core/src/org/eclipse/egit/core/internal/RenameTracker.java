/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.core.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RenameCallback;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * Class to track all renames encountered during a {@link RevWalk}
 */
public class RenameTracker {

	private static class RenamedPath {

		private final Map<ObjectId, String> paths = new LinkedHashMap<ObjectId, String>();

		RenamedPath add(final String path, final ObjectId commit) {
			paths.put(commit, path);
			return this;
		}

		public String getPath(final ObjectId commit) {
			return paths.get(commit);
		}
	}

	private final RevFilter filter = new RevFilter() {

		public boolean include(final RevWalk walker, final RevCommit commit) {
			for (DiffEntry entry : currentRenames) {
				String newPath = entry.getNewPath();
				String oldPath = entry.getOldPath();
				RenamedPath rename = currentPaths.get(newPath);
				if (rename == null) {
					rename = new RenamedPath();
					originalPaths.put(newPath, rename);
				}
				rename.add(oldPath, commit);
				currentPaths.remove(newPath);
				currentPaths.put(oldPath, rename);
			}
			currentRenames.clear();
			return true;
		}

		public RevFilter clone() {
			return null;
		}
	};

	private final RenameCallback callback = new RenameCallback() {

		public void renamed(DiffEntry entry) {
			currentRenames.add(entry);
		}
	};

	private List<DiffEntry> currentRenames = new ArrayList<DiffEntry>();

	private final Map<String, RenamedPath> currentPaths = new HashMap<String, RenamedPath>();

	private final Map<String, RenamedPath> originalPaths = new HashMap<String, RenamedPath>();

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
	 * @param commits
	 * @param target
	 * @param startingPath
	 * @return path
	 */
	public String getPath(final ObjectId[] commits, final ObjectId target,
			final String startingPath) {
		RenamedPath renames = originalPaths.get(startingPath);
		if (renames == null)
			return startingPath;
		String workingPath = startingPath;
		for (ObjectId candidate : commits) {
			if (AnyObjectId.equals(candidate, target))
				break;
			String renamedPath = renames.getPath(candidate);
			if (renamedPath != null)
				workingPath = renamedPath;
		}
		return workingPath;
	}

	/**
	 * Reset the tracker
	 *
	 * @return this tracker
	 */
	public RenameTracker reset() {
		originalPaths.clear();
		currentPaths.clear();
		currentRenames.clear();
		return this;
	}
}
