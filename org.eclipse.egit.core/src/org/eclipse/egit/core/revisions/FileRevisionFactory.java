/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.revisions;

import java.util.Objects;

import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.history.IFileRevision;

/**
 * Factory methods to create {@link IFileRevision}s.
 *
 * @since 5.12
 */
public final class FileRevisionFactory {

	private FileRevisionFactory() {
		// No instantiation
	}

	/**
	 * Obtains a file revision for a specific blob of an existing commit.
	 *
	 * @param repository
	 *            the repository this commit was loaded out of, and that this
	 *            file's blob should also be reachable through.
	 * @param commit
	 *            the commit the blob was identified to be within.
	 * @param gitPath
	 *            path within the commit's tree of the file.
	 * @return revision implementation for this file in the given commit.
	 */
	public static IFileRevision inCommit(Repository repository,
			RevCommit commit, String gitPath) {
		return GitFileRevision.inCommit(repository, commit, gitPath, null,
				null);
	}

	/**
	 * Obtains a file revision for a specific blob of an existing commit. Use
	 * this variant if you already know the blob ID and the
	 * {@link CheckoutMetadata} for a JGit tree walk.
	 *
	 * @param repository
	 *            the repository this commit was loaded out of, and that this
	 *            file's blob should also be reachable through.
	 * @param commit
	 *            the commit the blob was identified to be within.
	 * @param gitPath
	 *            path within the commit's tree of the file.
	 * @param blobId
	 *            ID of the blob within the git repository
	 * @param metadata
	 *            Smudge filters and EOL stream type to apply when the content
	 *            is to be gotten.
	 * @return revision implementation for this file in the given commit.
	 */
	public static IFileRevision inCommit(Repository repository,
			RevCommit commit, String gitPath, ObjectId blobId,
			CheckoutMetadata metadata) {
		return GitFileRevision.inCommit(repository, commit, gitPath,
				Objects.requireNonNull(blobId),
				Objects.requireNonNull(metadata));
	}

	/**
	 * Obtains a file revision for an item in the git index.
	 *
	 * @param repository
	 *            the repository which contains the index to use.
	 * @param gitPath
	 *            path of the resource in the index
	 * @return revision implementation for the given path in the index
	 */
	public static IFileRevision inIndex(Repository repository,
			String gitPath) {
		return GitFileRevision.inIndex(repository, gitPath);
	}

	/**
	 * Obtains a file revision for a particular stage of an item in the git
	 * index.
	 *
	 * @param repository
	 *            the repository which contains the index to use.
	 * @param gitPath
	 *            path of the resource in the index
	 * @param stage
	 *            stage of the index entry to get; use one of the
	 *            {@link DirCacheEntry} constants (e.g.
	 *            {@link DirCacheEntry#STAGE_2})
	 * @return revision implementation for the given path in the index
	 */
	public static IFileRevision inIndex(Repository repository,
			String gitPath, int stage) {
		return GitFileRevision.inIndex(repository, gitPath, stage);
	}

}
