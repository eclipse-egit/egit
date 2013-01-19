/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
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
import org.eclipse.egit.core.CoreText;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;

/** An {@link IFileRevision} for the version in the Git index. */
public class IndexFileRevision extends GitFileRevision {
	private final Repository db;

	private final String path;

	private ObjectId blobId;

	IndexFileRevision(final Repository repo, final String fileName) {
		super(fileName);
		db = repo;
		path = fileName;
	}

	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		if (blobId == null)
			blobId = locateBlobObjectId();
		return new IndexBlobStorage(db, path, blobId);
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public String getAuthor() {
		return "";  //$NON-NLS-1$
	}

	public long getTimestamp() {
		return -1;
	}

	public String getComment() {
		return null;
	}

	public String getContentIdentifier() {
		return INDEX;
	}

	private ObjectId locateBlobObjectId() throws CoreException {
		try {
			DirCacheEntry entry = db.readDirCache().getEntry(path);
			if (entry == null)
				return null;

			return entry.getObjectId();
		} catch (IOException e) {
			throw new CoreException(Activator.error(NLS.bind(
					CoreText.IndexFileRevision_errorLookingUpPath, path), e));
		}
	}
}
