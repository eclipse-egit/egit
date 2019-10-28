/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2013, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;

/** An {@link IFileRevision} for the version in the Git index. */
public class IndexFileRevision extends GitFileRevision implements
		OpenWorkspaceVersionEnabled {

	// This is to maintain compatibility with the old behavior
	private static final int FIRST_AVAILABLE = -1;

	private final Repository db;

	private final String path;

	private final int stage;

	private ObjectId blobId;

	private boolean isGitlink;

	private CheckoutMetadata metadata;

	IndexFileRevision(final Repository repo, final String path) {
		this(repo, path, FIRST_AVAILABLE);
	}

	IndexFileRevision(final Repository repo, final String path, int stage) {
		super(path);
		this.db = repo;
		this.path = path;
		this.stage = stage;
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		if (blobId == null) {
			try {
				DirCache cache = db.readDirCache();
				DirCacheEntry entry = locateEntry(cache);
				if (entry != null) {
					blobId = entry.getObjectId();
					if (FileMode.GITLINK.equals(entry.getFileMode())) {
						isGitlink = true;
					} else {
						if (blobId != null) {
							metadata = getMetadata(cache);
						}
					}
				} else if (!db.isBare()) {
					// Not found in index. Check if the file on disk, if any, is
					// a directory, and assume it's a gitlink if so. This is a
					// safe fallback and results in a read-only editor in the
					// compare editor.
					File onDisk = new File(db.getWorkTree(), path);
					isGitlink = onDisk.isDirectory();
				}
			} catch (IOException e) {
				throw new CoreException(Activator.error(
						NLS.bind(CoreText.IndexFileRevision_errorLookingUpPath,
								path),
						e));
			}
		}
		return new IndexBlobStorage(db, path, isGitlink, blobId, metadata);
	}

	/**
	 * @return whether this represents a gitlink
	 */
	public boolean isGitlink() {
		return isGitlink;
	}

	@Override
	public boolean isPropertyMissing() {
		return false;
	}

	@Override
	public String getAuthor() {
		return "";  //$NON-NLS-1$
	}

	@Override
	public long getTimestamp() {
		return -1;
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public String getContentIdentifier() {
		return INDEX;
	}

	private CheckoutMetadata getMetadata(DirCache cache) throws IOException {
		try (TreeWalk walk = new TreeWalk(db)) {
			walk.addTree(new DirCacheIterator(cache));
			FileTreeIterator files = new FileTreeIterator(db);
			files.setDirCacheIterator(walk, 0);
			walk.addTree(files);
			walk.setFilter(AndTreeFilter.create(
					PathFilterGroup.createFromStrings(path),
					new NotIgnoredFilter(1)));
			walk.setRecursive(true);
			if (walk.next()) {
				return new CheckoutMetadata(
						walk.getEolStreamType(
								TreeWalk.OperationType.CHECKOUT_OP),
						walk
						.getFilterCommand(Constants.ATTR_FILTER_TYPE_SMUDGE));
			}
		}
		return null;
	}

	private DirCacheEntry locateEntry(DirCache cache) {
		int firstIndex = cache.findEntry(path);
		if (firstIndex < 0)
			return null;

		// Try to avoid call to nextEntry if first entry already matches
		DirCacheEntry firstEntry = cache.getEntry(firstIndex);
		if (stage == FIRST_AVAILABLE || firstEntry.getStage() == stage)
			return firstEntry;

		// Ok, we have to search
		int nextIndex = cache.nextEntry(firstIndex);
		for (int i = firstIndex; i < nextIndex; i++) {
			DirCacheEntry entry = cache.getEntry(i);
			if (entry.getStage() == stage)
				return entry;
		}
		return null;
	}

	@Override
	public Repository getRepository() {
		return db;
	}

	@Override
	public String getGitPath() {
		return path;
	}
}
