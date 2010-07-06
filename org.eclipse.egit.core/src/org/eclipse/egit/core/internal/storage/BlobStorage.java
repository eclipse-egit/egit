/*******************************************************************************
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
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/** Accesses a blob from Git. */
class BlobStorage implements IStorage {
	protected final Repository db;

	private final String path;

	private final ObjectId blobId;

	BlobStorage(final Repository repository, final String fileName,
			final ObjectId blob) {
		db = repository;
		path = fileName;
		blobId = blob;
	}

	public InputStream getContents() throws CoreException {
		try {
			return open();
		} catch (IOException e) {
			throw new CoreException(Activator.error(
					NLS.bind(CoreText.BlobStorage_errorReadingBlob, blobId
							.name(), path), e));
		}
	}

	private InputStream open() throws IOException, CoreException,
			IncorrectObjectTypeException {
		try {
			return db.open(blobId, Constants.OBJ_BLOB).openStream();
		} catch (MissingObjectException notFound) {
			throw new CoreException(Activator.error(NLS.bind(
					CoreText.BlobStorage_blobNotFound, blobId.name(), path),
					null));
		}
	}

	public IPath getFullPath() {
		return Path.fromPortableString(path);
	}

	public String getName() {
		final int last = path.lastIndexOf('/');
		return last >= 0 ? path.substring(last + 1) : path;
	}

	public boolean isReadOnly() {
		return true;
	}

	public Object getAdapter(final Class adapter) {
		return null;
	}
}
