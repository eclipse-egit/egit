/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2013, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import java.io.IOException;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
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
		if (blobId == null)
			blobId = locateBlobObjectId();
		return new IndexBlobStorage(db, path, blobId);
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

	private ObjectId locateBlobObjectId() throws CoreException {
		try {
			DirCache dc = db.readDirCache();
			int firstIndex = dc.findEntry(path);
			if (firstIndex < 0)
				return null;

			// Try to avoid call to nextEntry if first entry already matches
			DirCacheEntry firstEntry = dc.getEntry(firstIndex);
			if (stage == FIRST_AVAILABLE || firstEntry.getStage() == stage)
				return firstEntry.getObjectId();

			// Ok, we have to search
			int nextIndex = dc.nextEntry(firstIndex);
			for (int i = firstIndex; i < nextIndex; i++) {
				DirCacheEntry entry = dc.getEntry(i);
				if (entry.getStage() == stage)
					return entry.getObjectId();
			}
			return null;
		} catch (IOException e) {
			throw new CoreException(Activator.error(NLS.bind(
					CoreText.IndexFileRevision_errorLookingUpPath, path), e));
		}
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
