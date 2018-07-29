/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (c) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.DecorationOverlayDescriptor;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.CancelledException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilterMarker;
import org.eclipse.jgit.util.LfsFactory;
import org.eclipse.ui.model.WorkbenchAdapter;

/**
 * A class with information about the changes to a file introduced in a
 * commit.
 */
public class FileDiff extends WorkbenchAdapter {

	/**
	 * Comparator for sorting FileDiffs based on getPath(). Compares first the
	 * directory part, if those are equal, the filename part.
	 */
	public static final Comparator<FileDiff> PATH_COMPARATOR = new Comparator<FileDiff>() {

		@Override
		public int compare(FileDiff left, FileDiff right) {
			String leftPath = left.getPath();
			String rightPath = right.getPath();
			int i = leftPath.lastIndexOf('/');
			int j = rightPath.lastIndexOf('/');
			int p = leftPath.substring(0, i + 1).replace('/', '\001').compareTo(
					rightPath.substring(0, j + 1).replace('/', '\001'));
			if (p != 0) {
				return p;
			}
			return leftPath.substring(i + 1)
					.compareTo(rightPath.substring(j + 1));
		}
	};

	private final RevCommit commit;

	private final DiffEntry diffEntry;

	private final Repository repository;

	static ObjectId[] trees(final RevCommit commit, final RevCommit[] parents) {
		final ObjectId[] r = new ObjectId[parents.length + 1];
		for (int i = 0; i < r.length - 1; i++)
			r[i] = parents[i].getTree().getId();
		r[r.length - 1] = commit.getTree().getId();
		return r;
	}

	/**
	 * Computer file diffs for specified tree walk and commit
	 *
	 * @param repository
	 * @param walk
	 * @param commit
	 * @param monitor
	 *            for progress reporting an cancellation; may be {@code null}
	 * @param markTreeFilters
	 *            optional filters for marking entries, see
	 *            {@link #isMarked(int)}
	 * @return non-null but possibly empty array of file diffs
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	public static FileDiff[] compute(Repository repository, TreeWalk walk,
			RevCommit commit, @Nullable IProgressMonitor monitor,
			TreeFilter... markTreeFilters) throws MissingObjectException,
			IncorrectObjectTypeException, CorruptObjectException, IOException {
		return compute(repository, walk, commit, commit.getParents(), monitor,
				markTreeFilters);
	}

	/**
	 * Computer file diffs for specified tree walk and commit
	 *
	 * @param repository
	 * @param walk
	 * @param commit
	 * @param parents
	 * @param monitor
	 *            for progress reporting an cancellation; may be {@code null}
	 * @param markTreeFilters
	 *            optional filters for marking entries, see
	 *            {@link #isMarked(int)}
	 * @return non-null but possibly empty array of file diffs
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	public static FileDiff[] compute(Repository repository, TreeWalk walk,
			RevCommit commit, RevCommit[] parents,
			@Nullable IProgressMonitor monitor, TreeFilter... markTreeFilters)
			throws MissingObjectException, IncorrectObjectTypeException,
			CorruptObjectException, IOException {

		final ArrayList<FileDiff> r = new ArrayList<>();

		if (parents.length > 0) {
			walk.reset(trees(commit, parents));
		} else {
			walk.reset();
			walk.addTree(new EmptyTreeIterator());
			walk.addTree(commit.getTree());
		}

		if (walk.getTreeCount() <= 2) {
			// TODO: make JGit DiffEntry.scan and RenameDetector.compute
			// cancelable
			SubMonitor progress = SubMonitor.convert(monitor, 3);
			List<DiffEntry> entries = DiffEntry.scan(walk, false,
					markTreeFilters);
			if (progress.isCanceled()) {
				return new FileDiff[0];
			}
			progress.worked(1);
			List<DiffEntry> xentries = new LinkedList<>(entries);
			RenameDetector detector = new RenameDetector(repository);
			detector.addAll(entries);
			boolean cancelled = false;
			List<DiffEntry> renames = Collections.emptyList();
			try {
				renames = detector.compute(walk.getObjectReader(),
						new EclipseGitProgressTransformer(
								progress.newChild(1)));
			} catch (CancelledException e) {
				cancelled = true;
			}
			if (!cancelled) {
				progress.setWorkRemaining(renames.size());
				for (DiffEntry m : renames) {
					final FileDiff d = new FileDiff(repository, commit, m);
					r.add(d);
					for (Iterator<DiffEntry> i = xentries.iterator(); i
							.hasNext();) {
						DiffEntry n = i.next();
						if (m.getOldPath().equals(n.getOldPath()))
							i.remove();
						else if (m.getNewPath().equals(n.getNewPath()))
							i.remove();
					}
					progress.worked(1);
				}
			}
			for (DiffEntry m : xentries) {
				final FileDiff d = new FileDiff(repository, commit, m);
				r.add(d);
			}
		}
		else { // DiffEntry does not support walks with more than two trees
			SubMonitor progress = SubMonitor.convert(monitor, 1);
			final int nTree = walk.getTreeCount();
			final int myTree = nTree - 1;

			TreeFilterMarker treeFilterMarker = new TreeFilterMarker(
					markTreeFilters);

			while (walk.next()) {
				if (progress.isCanceled()) {
					break;
				}
				progress.setWorkRemaining(100).worked(1);
				if (matchAnyParent(walk, myTree)) {
					continue;
				}
				int treeFilterMarks = treeFilterMarker.getMarks(walk);

				final FileDiffForMerges d = new FileDiffForMerges(repository,
						commit,
						treeFilterMarks);
				d.path = walk.getPathString();
				int m0 = 0;
				for (int i = 0; i < myTree; i++)
					m0 |= walk.getRawMode(i);
				final int m1 = walk.getRawMode(myTree);
				d.change = ChangeType.MODIFY;
				if (m0 == 0 && m1 != 0)
					d.change = ChangeType.ADD;
				else if (m0 != 0 && m1 == 0)
					d.change = ChangeType.DELETE;
				else if (m0 != m1 && walk.idEqual(0, myTree))
					d.change = ChangeType.MODIFY; // there is no ChangeType.TypeChanged
				d.blobs = new ObjectId[nTree];
				d.modes = new FileMode[nTree];
				for (int i = 0; i < nTree; i++) {
					d.blobs[i] = walk.getObjectId(i);
					d.modes[i] = walk.getFileMode(i);
				}

				r.add(d);
			}

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
	 * @param gitFormat
	 *            if false, do not show any source or destination prefix,
	 *            and the paths are calculated relative to the eclipse
	 *            project, otherwise relative to the git repository
	 * @throws IOException
	 */
	public void outputDiff(final StringBuilder d, final Repository db,
			final DiffFormatter diffFmt, boolean gitFormat) throws IOException {
		if (gitFormat) {
			diffFmt.setRepository(db);
			diffFmt.format(diffEntry);
			return;
		}

		try (ObjectReader reader = db.newObjectReader()) {
			outputEclipseDiff(d, db, reader, diffFmt);
		}
	}

	private void outputEclipseDiff(final StringBuilder d, final Repository db,
			final ObjectReader reader, final DiffFormatter diffFmt)
			throws IOException {
		if (!(getBlobs().length == 2))
			throw new UnsupportedOperationException(
					"Not supported yet if the number of parents is different from one"); //$NON-NLS-1$

		String projectRelativeNewPath = getProjectRelativePath(db, getNewPath());
		String projectRelativeOldPath = getProjectRelativePath(db, getOldPath());
		d.append("diff --git ").append(projectRelativeOldPath).append(" ") //$NON-NLS-1$ //$NON-NLS-2$
				.append(projectRelativeNewPath).append("\n"); //$NON-NLS-1$
		final ObjectId id1 = getBlobs()[0];
		final ObjectId id2 = getBlobs()[1];
		final FileMode mode1 = getModes()[0];
		final FileMode mode2 = getModes()[1];

		if (id1.equals(ObjectId.zeroId())) {
			d.append("new file mode " + mode2).append("\n"); //$NON-NLS-1$//$NON-NLS-2$
		} else if (id2.equals(ObjectId.zeroId())) {
			d.append("deleted file mode " + mode1).append("\n"); //$NON-NLS-1$//$NON-NLS-2$
		} else if (!mode1.equals(mode2)) {
			d.append("old mode " + mode1); //$NON-NLS-1$
			d.append("new mode " + mode2).append("\n"); //$NON-NLS-1$//$NON-NLS-2$
		}
		d.append("index ").append(reader.abbreviate(id1).name()). //$NON-NLS-1$
				append("..").append(reader.abbreviate(id2).name()). //$NON-NLS-1$
				append(mode1.equals(mode2) ? " " + mode1 : "").append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (id1.equals(ObjectId.zeroId()))
			d.append("--- /dev/null\n"); //$NON-NLS-1$
		else {
			d.append("--- "); //$NON-NLS-1$
			d.append(getProjectRelativePath(db, getOldPath()));
			d.append("\n"); //$NON-NLS-1$
		}

		if (id2.equals(ObjectId.zeroId()))
			d.append("+++ /dev/null\n"); //$NON-NLS-1$
		else {
			d.append("+++ "); //$NON-NLS-1$
			d.append(getProjectRelativePath(db, getNewPath()));
			d.append("\n"); //$NON-NLS-1$
		}

		final RawText a = getRawText(id1, reader);
		final RawText b = getRawText(id2, reader);
		EditList editList = MyersDiff.INSTANCE
				.diff(RawTextComparator.DEFAULT, a, b);
		diffFmt.format(editList, a, b);
	}

	private String getProjectRelativePath(Repository db, String repoPath) {
		IResource resource = ResourceUtil.getFileForLocation(db, repoPath, false);
		if (resource == null)
			return null;
		return resource.getProjectRelativePath().toString();
	}

	private RawText getRawText(ObjectId id, ObjectReader reader)
			throws IOException {
		if (id.equals(ObjectId.zeroId()))
			return new RawText(new byte[] {});
		ObjectLoader ldr = LfsFactory.getInstance().applySmudgeFilter(repository,
				reader.open(id, Constants.OBJ_BLOB),
				LfsFactory.getAttributesForPath(repository, getPath())
						.get(Constants.ATTR_DIFF));
		return new RawText(ldr.getCachedBytes(Integer.MAX_VALUE));
	}

	/**
	 * Get commit
	 *
	 * @return commit
	 */
	public RevCommit getCommit() {
		return commit;
	}

	/**
	 * Retrieves the repository the {@link FileDiff} and its commit belong to.
	 *
	 * @return the {@link Repository}
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @return the old path in case of a delete, the new path otherwise, but
	 *         never null or <code>/dev/null</code>
	 * @see #getNewPath()
	 * @see #getOldPath()
	 */
	public String getPath() {
		if (ChangeType.DELETE.equals(diffEntry.getChangeType()))
			return diffEntry.getOldPath();
		return diffEntry.getNewPath();
	}

	/**
	 * @return the old path or <code>/dev/null</code> for a completely new file
	 * @see #getPath() for getting the new or old path depending on change type
	 */
	public String getOldPath() {
		return diffEntry.getOldPath();
	}

	/**
	 * @return the new path or <code>/dev/null</code> for a deleted file
	 * @see #getPath() for getting the new or old path depending on change type
	 */
	public String getNewPath() {
		return diffEntry.getNewPath();
	}

	/**
	 * Get change type
	 *
	 * @return type
	 */
	public ChangeType getChange() {
		return diffEntry.getChangeType();
	}

	/**
	 * Get blob object ids
	 *
	 * @return non-null but possibly empty array of object ids
	 */
	public ObjectId[] getBlobs() {
		List<ObjectId> objectIds = new ArrayList<>();
		if (diffEntry.getOldId() != null)
			objectIds.add(diffEntry.getOldId().toObjectId());
		if (diffEntry.getNewId() != null)
			objectIds.add(diffEntry.getNewId().toObjectId());
		return objectIds.toArray(new ObjectId[]{});
	}

	/**
	 * Get file modes
	 *
	 * @return non-null but possibly empty array of file modes
	 */
	public FileMode[] getModes() {
		List<FileMode> modes = new ArrayList<>();
		if (diffEntry.getOldMode() != null)
			modes.add(diffEntry.getOldMode());
		if (diffEntry.getOldMode() != null)
			modes.add(diffEntry.getOldMode());
		return modes.toArray(new FileMode[]{});
	}

	/**
	 * Whether the mark tree filter with the specified index matched during scan
	 * or not, see
	 * {@link #compute(Repository, TreeWalk, RevCommit, RevCommit[], IProgressMonitor, TreeFilter...)}
	 * .
	 *
	 * @param index
	 *            the tree filter index to check
	 * @return true if it was marked, false otherwise
	 */
	public boolean isMarked(int index) {
		return diffEntry != null && diffEntry.isMarked(index);
	}

	/**
	 * Create a file diff for a specified {@link RevCommit} and
	 * {@link DiffEntry}
	 *
	 * @param repo
	 *
	 * @param c
	 * @param entry
	 */
	public FileDiff(final Repository repo, final RevCommit c,
			final DiffEntry entry) {
		repository = repo;
		diffEntry = entry;
		commit = c;
	}

	/**
	 * Is this diff a submodule?
	 *
	 * @return true if submodule, false otherwise
	 */
	public boolean isSubmodule() {
		if (diffEntry == null)
			return false;
		return diffEntry.getOldMode() == FileMode.GITLINK
				|| diffEntry.getNewMode() == FileMode.GITLINK;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		final ImageDescriptor base;
		if (!isSubmodule())
			base = UIUtils.getEditorImage(getPath());
		else
			base = UIIcons.REPOSITORY;
		switch (getChange()) {
		case ADD:
			return new DecorationOverlayDescriptor(base,
					UIIcons.OVR_STAGED_ADD, IDecoration.BOTTOM_RIGHT);
		case DELETE:
			return new DecorationOverlayDescriptor(base,
					UIIcons.OVR_STAGED_REMOVE, IDecoration.BOTTOM_RIGHT);
		case RENAME:
			return new DecorationOverlayDescriptor(base,
					UIIcons.OVR_STAGED_RENAME, IDecoration.BOTTOM_RIGHT);
		default:
			return base;
		}
	}

	@Override
	public String getLabel(Object object) {
		return getPath();
	}

	private static class FileDiffForMerges extends FileDiff {
		private String path;

		private ChangeType change;

		private ObjectId[] blobs;

		private FileMode[] modes;

		private final int treeFilterMarks;

		private FileDiffForMerges(final Repository repo, final RevCommit c,
				int treeFilterMarks) {
			super(repo, c, null);
			this.treeFilterMarks = treeFilterMarks;
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public String getNewPath() {
			return path;
		}

		@Override
		public ChangeType getChange() {
			return change;
		}

		@Override
		public ObjectId[] getBlobs() {
			return blobs;
		}

		@Override
		public FileMode[] getModes() {
			return modes;
		}

		@Override
		public boolean isMarked(int index) {
			return (treeFilterMarks & (1L << index)) != 0;
		}
	}
}
