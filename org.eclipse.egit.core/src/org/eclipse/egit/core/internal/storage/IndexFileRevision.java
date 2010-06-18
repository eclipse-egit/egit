/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
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
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.storage.file.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;

/** An {@link IFileRevision} for the version in the Git index. */
class IndexFileRevision extends GitFileRevision implements IFileRevision {
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

	public IFileRevision withAllProperties(IProgressMonitor monitor)
			throws CoreException {
		return null;
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
			final GitIndex idx = db.getIndex();
			final Entry e = idx.getEntry(path);
			if (e == null)
				throw new CoreException(Activator.error(NLS.bind(
						CoreText.IndexFileRevision_indexEntryNotFound, path),
						null));
			return e.getObjectId();

		} catch (IOException e) {
			throw new CoreException(Activator.error(NLS.bind(
					CoreText.IndexFileRevision_errorLookingUpPath, path), e));
		}
	}
}
