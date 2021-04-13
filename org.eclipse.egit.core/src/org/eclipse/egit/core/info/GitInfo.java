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
	 * Retrieves the repository.
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

}
