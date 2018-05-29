/*******************************************************************************
 * Copyright (C) 2010, 2012 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;

/**
 * IteratorService is a utility class for providing the right
 * {@link WorkingTreeIterator} iterator for a given folder.
 *
 */
public class IteratorService {

	/**
	 * Creates a {@link WorkingTreeIterator} for a tree walk starting on the
	 * repository work tree folder.
	 *
	 * @param repository
	 * @return a {@link FileTreeIterator} or {@code null} if repository is bare
	 */
	public static WorkingTreeIterator createInitialIterator(
			Repository repository) {
		if (repository.isBare()) {
			return null;
		}
		return new FileTreeIterator(repository);
	}

	/**
	 * The method searches a container resource related to the given file. The
	 * container must reside in a project that is shared with Git. Linked folders
	 * are ignored.
	 *
	 * @param root
	 *            the workspace root
	 * @param file
	 * @return a container that matches the description above or null if such a
	 *         container does not exist
	 */
	public static IContainer findContainer(IWorkspaceRoot root, File file) {
		if (!file.exists())
			return null;
		if (!file.isDirectory())
			throw new IllegalArgumentException(
					"file " + file.getAbsolutePath() + " is no directory"); //$NON-NLS-1$//$NON-NLS-2$

		// fast path to get desired container
		IContainer resource = root.getContainerForLocation(new Path(file
				.getAbsolutePath()));

		// Eclipse has no mappings for this path
		if (resource == null)
			return null;

		if (isValid(resource))
			return resource;

		// the case where the project is either closed or resource is linked:
		// try to find *all* possible candidates (hello overlapping projects!)
		// The code below performs exceptionally slow because it tries to find
		// all linked resources too
		final IContainer[] containers = root.findContainersForLocationURI(file
				.toURI());
		for (IContainer container : containers)
			if (isValid(container))
				return container;
		return null;
	}

	/**
	 * sort out closed, linked or not shared directories
	 *
	 * @param container
	 * @return true if the container is shared with git, not a link and
	 *         accessible in Eclipse
	 */
	private static boolean isValid(IContainer container) {
		return container.isAccessible()
				&& !container.isLinked(IResource.CHECK_ANCESTORS)
				&& isProjectSharedWithGit(container);
	}

	private static boolean isProjectSharedWithGit(IContainer container) {
		return RepositoryMapping.getMapping(container) != null;
	}

}
