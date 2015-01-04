/*******************************************************************************
 * Copyright (C) 2010, 2012 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;
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
	 * @return <li>a {@link ContainerTreeIterator} if the work tree folder of
	 *         the given repository resides in a project shared with Git <li>an
	 *         {@link AdaptableFileTreeIterator} otherwise <li>{@code null} if the
	 *         workspace is closed.
	 */
	public static WorkingTreeIterator createInitialIterator(
			Repository repository) {
		IWorkspaceRoot root;
		try {
			root = ResourcesPlugin.getWorkspace().getRoot();
		} catch (IllegalStateException e) {
			// workspace is closed
			return null;
		}
		File workTree = repository.getWorkTree();
		if (!workTree.exists())
			return null;
		IContainer container = findContainer(root, workTree);
		if (container != null)
			return new ContainerTreeIterator(repository, container);
		return new AdaptableFileTreeIterator(repository, root);
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
