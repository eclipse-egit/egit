/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Benjamin Muskalla (Tasktop Technologies) - moved into Core for reusability
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Utility class for compare-related functionality
 */
public class CompareCoreUtils {

	/**
	 * Determine the encoding used by Eclipse for the resource which belongs to
	 * repoPath in the eclipse workspace or null if no resource is found
	 *
	 * @param db
	 *            the repository
	 * @param repoPath
	 *            the path in the git repository
	 * @return the encoding used in eclipse for the resource or null if
	 *
	 */
	public static String getResourceEncoding(Repository db, String repoPath) {
		if (db.isBare())
			return null;
		IFile resource = ResourceUtil.getFileForLocation(db, repoPath);
		if (resource == null)
			return null;

		return getResourceEncoding(resource);
	}

	/**
	 * Determine the encoding used by eclipse for the resource.
	 *
	 * @param resource
	 *            must be an instance of IEncodedStorage
	 * @return the encoding used in Eclipse for the resource if found or null
	 */
	public static String getResourceEncoding(IResource resource) {
		// Get the encoding for the current version. As a matter of
		// principle one might want to use the eclipse settings for the
		// version we are retrieving as that may be defined by the
		// project settings, but there is no historic API for this.
		String charset;
		IEncodedStorage encodedStorage = ((IEncodedStorage) resource);
		try {
			charset = encodedStorage.getCharset();
			if (charset == null)
				charset = resource.getParent().getDefaultCharset();
		} catch (CoreException e) {
			charset = Constants.CHARACTER_ENCODING;
		}
		return charset;
	}

	/**
	 * Get the {@link DiffEntry} corresponding to a change in file path. If the
	 * file was renamed, the resulting {@link DiffEntry} will contain the old
	 * path and blob ID. If the file was only added, null will be returned.
	 *
	 * @param repository
	 * @param newPath
	 *            path of the file in new commit
	 * @param newCommit
	 *            new commit
	 * @param oldCommit
	 *            old commit, e.g. parent commit of newCommit
	 * @param objectReader
	 *            reader for the repository
	 * @return the diff entry corresponding to the change for path, or null if
	 *         none could be found
	 * @throws IOException
	 */
	public static DiffEntry getChangeDiffEntry(Repository repository, String newPath,
			RevCommit newCommit, RevCommit oldCommit, ObjectReader objectReader)
			throws IOException {
		TreeWalk walk = new TreeWalk(objectReader);
		walk.setRecursive(true);
		walk.addTree(oldCommit.getTree());
		walk.addTree(newCommit.getTree());

		List<DiffEntry> entries = DiffEntry.scan(walk);

		for (DiffEntry diff : entries)
			if (diff.getChangeType() == ChangeType.MODIFY
					&& newPath.equals(diff.getNewPath()))
				return diff;

		if (entries.size() < 2)
			return null;

		RenameDetector detector = new RenameDetector(repository);
		detector.addAll(entries);
		List<DiffEntry> renames = detector.compute(walk.getObjectReader(),
				NullProgressMonitor.INSTANCE);
		for (DiffEntry diff : renames)
			if (diff.getChangeType() == ChangeType.RENAME
					&& newPath.equals(diff.getNewPath()))
				return diff;

		return null;
	}
}
