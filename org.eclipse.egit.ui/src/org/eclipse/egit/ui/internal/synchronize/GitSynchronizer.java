/*******************************************************************************
 * Copyright (C) 2016 Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkbenchPage;

/**
 * A synchronizer is able to launch a synchronization.
 */
public interface GitSynchronizer {

	/**
	 * Decide how to display the result of a synchronization operation to the
	 * user, generally by opening the synchronize view for the given set of
	 * resources, comparing the given revisions together, or directly opening
	 * the comparison editor if only one file is involved.
	 * <p>
	 * Note that this may also open the git tree compare view or any other
	 * alternative depending on implementation-specific criteria.
	 * </p>
	 *
	 * @param resources
	 *            The resources to compare, can be empty in which case the whole
	 *            repositpry will be synchronized.
	 * @param repository
	 *            The repository to use.
	 * @param leftRev
	 *            The left revision.
	 * @param rightRev
	 *            The right revision.
	 * @param includeLocal
	 *            Whether the local version of the file should be used, in which
	 *            case leftRev is ignored.
	 *
	 * @param somePage
	 *            If not {@code null} try to re-use a compare editor on this
	 *            page if any is available. Otherwise open a new one.
	 *
	 * @throws IOException
	 */
	void compare(IResource[] resources, @NonNull Repository repository,
			String leftRev, String rightRev, boolean includeLocal,
			IWorkbenchPage somePage) throws IOException;

	/**
	 * Decide how to display the result of a synchronization operation to the
	 * user, generally by opening a comparison editor on the file involved.
	 * <p>
	 * Note that this may however open the synchronize view, the git tree
	 * compare view, or any other alternative depending on
	 * implementation-specific criteria.
	 * </p>
	 *
	 * @param file
	 *            The file to compare.
	 * @param repository
	 *            The repository to use.
	 * @param leftPath
	 *            The path of the left revision, <code>null</code> for file
	 *            creation.
	 * @param rightPath
	 *            The path of the right revision, <code>null</code> for file
	 *            deletion.
	 * @param leftRev
	 *            The left revision.
	 * @param rightRev
	 *            The right revision.
	 * @param includeLocal
	 *            Whether the local version of the file should be used, in which
	 *            case leftRev is ignored.
	 *
	 * @param somePage
	 *            If not {@code null} try to re-use a compare editor on this
	 *            page if any is available. Otherwise open a new one.
	 *
	 * @throws IOException
	 */
	void compare(@NonNull IFile file, @NonNull Repository repository,
			String leftPath, String rightPath, String leftRev, String rightRev,
			boolean includeLocal, IWorkbenchPage somePage) throws IOException;
}
