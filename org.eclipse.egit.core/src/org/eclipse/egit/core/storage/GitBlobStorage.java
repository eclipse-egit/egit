/*******************************************************************************
 * Copyright (C) 2006, 2012 Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2014, Obeo
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.attributes.Filtering;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.AutoCRLF;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.io.EolStreamTypeUtil;
import org.eclipse.osgi.util.NLS;

/**
 * Provides access to a git blob.
 *
 * @since 4.0
 */
public class GitBlobStorage implements IEncodedStorage {


	/** Repository containing the object this storage provides access to. */
	protected final Repository db;

	/** Repository-relative path of the underlying object. */
	protected final String path;

	/** Id of this object in its repository. */
	protected final ObjectId blobId;

	/** Checkout metadata: smudge filters, EOL stream type. */
	private final CheckoutMetadata metadata;

	private String charset;

	/**
	 * @param repository
	 *            The repository containing this object.
	 * @param path
	 *            Repository-relative path of the underlying object. This path
	 *            is not validated by this class, i.e. it's returned as is by
	 *            {@code #getAbsolutePath()} and {@code #getFullPath()} without
	 *            validating if the blob is reachable using this path.
	 * @param blob
	 *            Id of this object in its repository.
	 * @deprecated Use
	 *             {@link #GitBlobStorage(Repository, String, ObjectId, CheckoutMetadata)}
	 *             instead.
	 */
	@Deprecated
	public GitBlobStorage(final Repository repository, final String path,
			final ObjectId blob) {
		this(repository, path, blob, null);
	}

	/**
	 * @param repository
	 *                       The repository containing this object.
	 * @param path
	 *                       Repository-relative path of the underlying object.
	 *                       This path is not validated by this class, i.e. it's
	 *                       returned as is by {@code #getAbsolutePath()} and
	 *                       {@code #getFullPath()} without validating if the
	 *                       blob is reachable using this path.
	 * @param blob
	 *                       Id of this object in its repository.
	 * @param metadata
	 *                       Smudge filters and EOL stream type to apply when
	 *                       the content is to be gotten.
	 * @since 5.0
	 */
	public GitBlobStorage(final Repository repository, final String path,
			final ObjectId blob, final CheckoutMetadata metadata) {
		this.db = repository;
		this.path = path;
		this.blobId = blob;
		this.metadata = metadata;
	}

	@Override
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
		if (blobId == null) {
			return new ByteArrayInputStream(new byte[0]);
		}
		try {
			WorkingTreeOptions workingTreeOptions = db.getConfig().get(WorkingTreeOptions.KEY);
			InputStream objectInputStream = db.open(blobId,
					Constants.OBJ_BLOB).openStream();
			InputStream filteredInputStream = objectInputStream;
			if (metadata != null) {
				filteredInputStream = Filtering.filter(db, path,
						objectInputStream, metadata.smudgeFilterCommand);
			}
			EolStreamType streamType;
			if (metadata != null && metadata.eolStreamType != null) {
				streamType = metadata.eolStreamType;
			} else if (workingTreeOptions.getAutoCRLF() == AutoCRLF.TRUE) {
				streamType = EolStreamType.AUTO_CRLF;
			} else {
				streamType = EolStreamType.DIRECT;
			}
			return EolStreamTypeUtil.wrapInputStream(filteredInputStream,
					streamType);
		} catch (MissingObjectException notFound) {
			throw new CoreException(Activator.error(NLS.bind(
					CoreText.BlobStorage_blobNotFound, blobId.name(), path),
					notFound));
		}
	}

	@Override
	public IPath getFullPath() {
		return Path.fromPortableString(path);
	}

	@Override
	public String getName() {
		final int last = path.lastIndexOf('/');
		return last >= 0 ? path.substring(last + 1) : path;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public <T> T getAdapter(final Class<T> adapter) {
		return null;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { blobId, db, path });
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GitBlobStorage other = (GitBlobStorage) obj;
		if (blobId == null) {
			if (other.blobId != null)
				return false;
		} else if (!blobId.equals(other.blobId))
			return false;
		if (db == null) {
			if (other.db != null)
				return false;
		} else if (!db.equals(other.db))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	/**
	 * Returns the absolute path on disk of the underlying object.
	 * <p>
	 * The returned path may not point to an existing file if the object does
	 * not exist locally.
	 * </p>
	 *
	 * @return The absolute path on disk of the underlying object.
	 */
	public IPath getAbsolutePath() {
		if (db.isBare()) {
			return null;
		}
		return new Path(db.getWorkTree().getAbsolutePath() + File.separatorChar
				+ path);
	}

	/**
	 * @since 4.1
	 */
	@Override
	public String getCharset() throws CoreException {
		if (charset == null) {
			charset = CompareCoreUtils.getResourceEncoding(db, path);
			if (charset == null) {
				charset = ResourcesPlugin.getEncoding();
			}
		}
		return charset;
	}
}
