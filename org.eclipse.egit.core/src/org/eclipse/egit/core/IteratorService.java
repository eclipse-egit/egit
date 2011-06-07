/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
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
	 *         {@link AdaptableFileTreeIterator} otherwise
	 */
	public static WorkingTreeIterator createInitialIterator(
			Repository repository) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer container = findContainer(root, repository.getWorkTree());
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
		if (!file.isDirectory())
			throw new IllegalArgumentException(
					"file " + file.getAbsolutePath() + " is no directory"); //$NON-NLS-1$//$NON-NLS-2$
		final IContainer[] containers = root.findContainersForLocationURI(file
				.toURI());
		for (IContainer container : containers)
			if (container.isAccessible() && !container.isLinked() && isProjectSharedWithGit(container))
				return container;
		return null;
	}

	private static boolean isProjectSharedWithGit(IContainer container) {
		return RepositoryMapping.getMapping(container) != null;
	}

}
