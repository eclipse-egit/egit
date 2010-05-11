/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (c) 2010, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
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

	/**
	 * Creates a textual diff together with meta information.
	 * TODO So far this works only in case of one parent commit.
	 *
	 * @param d
	 *            the StringBuilder where the textual diff is added to
	 * @param db
	 *            the Repo
	 * @param diffFmt
	 *            the DiffFormatter used to create the textual diff
	 * @param noPrefix
	 *            if true, do not show any source or destination prefix.
	 * @param pathRelativeToProject
	 *            if true, the paths are calculated relative to the eclipse
	 *            project. otherwise relative to the git repository
	 * @throws IOException
	 */
	public void outputDiff(final StringBuilder d, final Repository db,
			final DiffFormatter diffFmt, boolean noPrefix,
			boolean pathRelativeToProject) throws IOException {
		if (!(blobs.length == 2))
			throw new UnsupportedOperationException(
					"Not supported yet if the number of parents is different from one"); //$NON-NLS-1$

		final ObjectId id1 = blobs[0];
		final ObjectId id2 = blobs[1];
		final FileMode mode1 = modes[0];
		final FileMode mode2 = modes[1];

		if (id1.equals(ObjectId.zeroId())) {
			d.append("new file mode " + mode2).append("\n"); //$NON-NLS-1$//$NON-NLS-2$
		} else if (id2.equals(ObjectId.zeroId())) {
			d.append("deleted file mode " + mode1).append("\n"); //$NON-NLS-1$//$NON-NLS-2$
		} else if (!mode1.equals(mode2)) {
			d.append("old mode " + mode1); //$NON-NLS-1$
			d.append("new mode " + mode2).append("\n"); //$NON-NLS-1$//$NON-NLS-2$
		}
		d.append("index ").append(id1.abbreviate(db, 7).name()). //$NON-NLS-1$
				append("..").append(id2.abbreviate(db, 7).name()). //$NON-NLS-1$
				append(mode1.equals(mode2) ? " " + mode1 : "").append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (id1.equals(ObjectId.zeroId()))
			d.append("--- /dev/null\n"); //$NON-NLS-1$
		else {
			d.append("--- "); //$NON-NLS-1$
			if (!noPrefix)
				d.append("a").append(IPath.SEPARATOR); //$NON-NLS-1$
			if (pathRelativeToProject)
				d.append(getProjectRelaticePath(db, path));
			else
				d.append(path);
			d.append("\n"); //$NON-NLS-1$
		}

		if (id2.equals(ObjectId.zeroId()))
			d.append("+++ /dev/null\n"); //$NON-NLS-1$
		else {
			d.append("+++ "); //$NON-NLS-1$
			if (!noPrefix)
				d.append("b").append(IPath.SEPARATOR); //$NON-NLS-1$
			if (pathRelativeToProject)
				d.append(getProjectRelaticePath(db, path));
			else
				d.append(path);
			d.append("\n"); //$NON-NLS-1$
		}

		final RawText a = getRawText(id1, db);
		final RawText b = getRawText(id2, db);
		final MyersDiff diff = new MyersDiff(a, b);
		diffFmt.formatEdits(new OutputStream() {

			@Override
			public void write(int c) throws IOException {
				d.append((char) c);

			}
		}, a, b, diff.getEdits());
	}

	private String getProjectRelaticePath(Repository db, String repoPath) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IPath absolutePath = new Path(db.getWorkDir().getAbsolutePath()).append(repoPath);
		IResource resource = root.getFileForLocation(absolutePath);
		return resource.getProjectRelativePath().toString();
	}

	private RawText getRawText(ObjectId id, Repository db) throws IOException {
		if (id.equals(ObjectId.zeroId()))
			return new RawText(new byte[] { });
		return new RawText(db.openBlob(id).getCachedBytes());
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
