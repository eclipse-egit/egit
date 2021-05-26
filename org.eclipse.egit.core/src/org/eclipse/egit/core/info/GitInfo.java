/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.info;

import org.eclipse.egit.core.internal.info.GitItemStateFactory;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Accessor interface that can be used to obtain git-related information about
 * various objects. EGit registers an adapter factory that provides
 * {@code GitInfo} accessors for {@link org.eclipse.core.resources.IResource}s
 * and {@link org.eclipse.team.core.history.IFileRevision}s.
 *
 * @since 5.12
 */
public interface GitInfo {

	/**
	 * Retrieves the repository. The {@link Repository} returned is managed by
	 * EGit and <em>must not</em> be {@link Repository#close() closed}. If used
	 * in a try-with-resource statement, use {@link Repository#incrementOpen()}
	 * to prevent the repository from being closed.
	 *
	 * @return the {@link Repository} the object is in, if any, or {@code null}
	 *         if the item is not managed by EGit
	 */
	Repository getRepository();

	/**
	 * Retrieves the git path relative to the repository root.
	 *
	 * @return the git path, using forward slashes as file separators, or
	 *         {@code null} if no git path can be determined
	 */
	String getGitPath();

	/**
	 * Describes the origin of the item this {@link GitInfo} object had been
	 * obtained from.
	 */
	enum Source {
		WORKING_TREE, INDEX, COMMIT
	}

	/**
	 * Returns the {@link Source} of the item this {@link GitInfo} object was
	 * obtained from.
	 * <p>
	 * {@link GitInfo} objects can be obtained via adaptation from a variety of
	 * objects. If the adapted object was an
	 * {@link org.eclipse.core.resources.IResource IResource}, then it's in the
	 * git {@link Source#WORKING_TREE}, but the {@link GitInfo} might also have
	 * been obtained from a file version in the {@link Source#INDEX}, or even
	 * from a file revision in a {@link Source#COMMIT}.
	 * </p>
	 *
	 * @return the {@link Source} of this {@link GitInfo} object, or
	 *         {@code null} if the item is not managed by EGit or the source
	 *         cannot be determined (for raw
	 *         {@link org.eclipse.egit.core.storage.GitBlobStorage
	 *         GitBlobStorage} objects).
	 */
	Source getSource();

	/**
	 * Retrieves the commit ID of the commit containing the item, if the
	 * {@link #getSource() source} of this {@link GitInfo} object is
	 * {@link Source#COMMIT}.
	 *
	 * @return the commit ID, or {@code null} if the {@link #getSource() source}
	 *         of this {@link GitInfo} is not from a git commit or is a raw
	 *         {@link org.eclipse.egit.core.storage.GitBlobStorage
	 *         GitBlobStorage} object. These describe raw blobs in the git
	 *         repository and have no information about any commit.
	 */
	AnyObjectId getCommitId();

	/**
	 * Retrieves a {@link GitItemState} providing information about the git
	 * state of the item this {@link GitInfo} is about.
	 *
	 * @return a {@link GitItemState}, or {@code null} if the item isn't in any
	 *         repository
	 */
	default GitItemState getGitState() {
		return GitItemStateFactory.getInstance().get(getRepository(),
				getGitPath());
	}
}
