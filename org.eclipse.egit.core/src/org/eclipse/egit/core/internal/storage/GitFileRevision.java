/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
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

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;

/**
 * A Git related {@link IFileRevision}. It references a version and a resource,
 * i.e. the version we think corresponds to the resource in specific version.
 */
public abstract class GitFileRevision extends FileRevision {
	/** Content identifier for the working copy. */
	public static final String WORKSPACE = "Workspace";  //$NON-NLS-1$

	/** Content identifier for the working tree version.
	/* Used to access non workspace files */
	public static final String WORKING_TREE = "Working Tree";  //$NON-NLS-1$

	/** Content identifier for the content staged in the index. */
	public static final String INDEX = "Index";  //$NON-NLS-1$

	/**
	 * Obtain a file revision for a specific blob of an existing commit.
	 *
	 * @param db
	 *            the repository this commit was loaded out of, and that this
	 *            file's blob should also be reachable through.
	 * @param commit
	 *            the commit the blob was identified to be within.
	 * @param path
	 *            path within the commit's tree of the file.
	 * @param blobId
	 *            unique name of the content.
	 * @param metadata
	 *            Smudge filters and EOL stream type to apply when the content
	 *            is to be gotten.
	 * @return revision implementation for this file in the given commit.
	 */
	public static GitFileRevision inCommit(final Repository db,
			final RevCommit commit, final String path, final ObjectId blobId,
			CheckoutMetadata metadata) {
		return new CommitFileRevision(db, commit, path, blobId, metadata);
	}

	/**
	 * @param db
	 *            the repository which contains the index to use.
	 * @param path
	 *            path of the resource in the index
	 * @return revision implementation for the given path in the index
	 */
	public static GitFileRevision inIndex(final Repository db, final String path) {
		return new IndexFileRevision(db, path);
	}

	/**
	 * @param db
	 *            the repository which contains the index to use.
	 * @param path
	 *            path of the resource in the index
	 * @param stage
	 *            stage of the index entry to get, use one of the
	 *            {@link DirCacheEntry} constants (e.g.
	 *            {@link DirCacheEntry#STAGE_2})
	 * @return revision implementation for the given path in the index
	 */
	public static GitFileRevision inIndex(final Repository db,
			final String path, int stage) {
		return new IndexFileRevision(db, path, stage);
	}

	private final String path;

	GitFileRevision(final String path) {
		this.path = path;
	}

	@Override
	public String getName() {
		final int last = path.lastIndexOf('/');
		return last >= 0 ? path.substring(last + 1) : path;
	}

	@Override
	public boolean isPropertyMissing() {
		return false;
	}

	@Override
	public IFileRevision withAllProperties(final IProgressMonitor monitor)
			throws CoreException {
		return this;
	}

	/**
	 * Retrieves the {@link Repository} this file revision comes from.
	 *
	 * @return the {@link Repository}
	 */
	public abstract Repository getRepository();

	@Override
	public URI getURI() {
		try {
			return new URI(null, null, path, null);
		} catch (URISyntaxException e) {
			return null;
		}
	}
}
