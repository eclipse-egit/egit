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
import org.eclipse.egit.core.storage.GitBlobStorage;
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
	 *         if the item is not git managed
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
	 * Retrieves the commit ID of the commit containing the item, if any.
	 *
	 * @return the commit ID, {@code null} for working tree files,
	 *         {@link org.eclipse.jgit.lib.ObjectId#zeroId()} for index items
	 * @throws UnsupportedOperationException
	 *             if the item this {@link GitInfo} was obtained for was a raw
	 *             {@link GitBlobStorage}. A {@link GitBlobStorage} described a
	 *             raw blob in the git repository and has no information about
	 *             any commit.
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
