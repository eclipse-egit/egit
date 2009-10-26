/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

class FileDiff {
	private static ObjectId[] trees(final RevCommit commit) {
		final ObjectId[] r = new ObjectId[commit.getParentCount() + 1];
		for (int i = 0; i < r.length - 1; i++)
			r[i] = commit.getParent(i).getTree().getId();
		r[r.length - 1] = commit.getTree().getId();
		return r;
	}

	static FileDiff[] compute(final TreeWalk walk, final RevCommit commit)
			throws MissingObjectException, IncorrectObjectTypeException,
			CorruptObjectException, IOException {
		final ArrayList<FileDiff> r = new ArrayList<FileDiff>();

		walk.reset(trees(commit));
		final int nTree = walk.getTreeCount();
		final int myTree = nTree - 1;

		switch (nTree) {
		case 1:
			while (walk.next()) {
				final FileDiff d = new FileDiff(commit, walk.getPathString());
				d.change = "A"; //$NON-NLS-1$
				d.blobs = new ObjectId[] { walk.getObjectId(0) };
				d.modes = new FileMode[] { walk.getFileMode(0) };
				r.add(d);
			}
			break;
		case 2:
			while (walk.next()) {
				final FileDiff d = new FileDiff(commit, walk.getPathString());
				final ObjectId id0 = walk.getObjectId(0);
				final ObjectId id1 = walk.getObjectId(1);
				final FileMode fm0 = walk.getFileMode(0);
				final FileMode fm1 = walk.getFileMode(1);
				d.change = "M"; //$NON-NLS-1$
				d.blobs = new ObjectId[] { id0, id1 };
				d.modes = new FileMode[] { fm0, fm1 };

				final int m0 = walk.getRawMode(0);
				final int m1 = walk.getRawMode(1);
				if (m0 == 0 && m1 != 0)
					d.change = "A"; //$NON-NLS-1$
				else if (m0 != 0 && m1 == 0)
					d.change = "D"; //$NON-NLS-1$
				else if (m0 != m1 && walk.idEqual(0, 1))
					d.change = "T"; //$NON-NLS-1$
				r.add(d);
			}
			break;
		default:
			while (walk.next()) {
				if (matchAnyParent(walk, myTree))
					continue;

				final FileDiff d = new FileDiff(commit, walk.getPathString());
				int m0 = 0;
				for (int i = 0; i < myTree; i++)
					m0 |= walk.getRawMode(i);
				final int m1 = walk.getRawMode(myTree);
				d.change = "M"; //$NON-NLS-1$
				if (m0 == 0 && m1 != 0)
					d.change = "A"; //$NON-NLS-1$
				else if (m0 != 0 && m1 == 0)
					d.change = "D"; //$NON-NLS-1$
				else if (m0 != m1 && walk.idEqual(0, myTree))
					d.change = "T"; //$NON-NLS-1$
				d.blobs = new ObjectId[nTree];
				d.modes = new FileMode[nTree];
				for (int i = 0; i < nTree; i++) {
					d.blobs[i] = walk.getObjectId(i);
					d.modes[i] = walk.getFileMode(i);
				}
				r.add(d);
			}
			break;
		}

		final FileDiff[] tmp = new FileDiff[r.size()];
		r.toArray(tmp);
		return tmp;
	}

	private static boolean matchAnyParent(final TreeWalk walk, final int myTree) {
		final int m = walk.getRawMode(myTree);
		for (int i = 0; i < myTree; i++)
			if (walk.getRawMode(i) == m && walk.idEqual(i, myTree))
				return true;
		return false;
	}

	final RevCommit commit;

	final String path;

	String change;

	ObjectId[] blobs;

	FileMode[] modes;

	FileDiff(final RevCommit c, final String p) {
		commit = c;
		path = p;
	}
}
