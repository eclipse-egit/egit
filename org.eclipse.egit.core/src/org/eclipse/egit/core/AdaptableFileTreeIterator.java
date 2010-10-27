/*******************************************************************************
 * Copyright (C) 2009, Tor Arne Vestbø <torarnv@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.FS;

/**
 * Java IO file tree iterator that can adapt to a {@link ContainerTreeIterator}
 * <p>
 * The iterator automatically adapts to a {@link ContainerTreeIterator} when
 * recursing into directories that are accessible from the given workspace root.
 *
 * @see org.eclipse.jgit.treewalk.FileTreeIterator
 * @see org.eclipse.egit.core.ContainerTreeIterator
 */
public class AdaptableFileTreeIterator extends FileTreeIterator {

	IWorkspaceRoot root;

	/**
	 * Create a new iterator to traverse the given directory and its children
	 * <p>
	 * The iterator will automatically adapt to a {@link ContainerTreeIterator}
	 * when encountering directories what can be mapped into the given workspace
	 * root.
	 *
	 * @param path
	 *            the starting directory. This directory should correspond to
	 *            the repository root.
	 * @param workspaceRoot
	 *            the workspace root to check resource mapping against.
	 *
	 */
	public AdaptableFileTreeIterator(final File path,
			final IWorkspaceRoot workspaceRoot) {
		super(path, FS.DETECTED, WorkingTreeOptions.createDefaultInstance());
		root = workspaceRoot;
	}

	/**
	 * Create a new iterator to traverse a subdirectory.
	 * <p>
	 * The iterator will automatically adapt to a {@link ContainerTreeIterator}
	 * when encountering directories what can be mapped into the given workspace
	 * root.
	 *
	 * @param path
	 *            the subdirectory. This should be a directory contained within
	 *            the parent directory.
	 * @param parent
	 *            the parent iterator we were created from.
	 * @param workspaceRoot
	 *            the workspace root to check resource mapping against.
	 */
	protected AdaptableFileTreeIterator(final AdaptableFileTreeIterator parent,
			File path, final IWorkspaceRoot workspaceRoot) {
		super(parent, path, FS.DETECTED);
		root = workspaceRoot;
	}

	@Override
	public AbstractTreeIterator createSubtreeIterator(ObjectReader repo)
			throws IncorrectObjectTypeException, IOException {
		final File currentFile = ((FileEntry) current()).getFile();
		final IContainer[] containers = root
				.findContainersForLocationURI(currentFile.toURI());
		if (containers.length == 0)
			return new AdaptableFileTreeIterator(this, currentFile, root);
		if (containers.length == 1) {
			if (containers[0].isAccessible())
				return new ContainerTreeIterator(this, containers[0]);
		} else {
			// there is more than one container
			IContainer container = findContainer(containers);
			if (container != null)
				return new ContainerTreeIterator(this, container);
		}
		return new AdaptableFileTreeIterator(this, currentFile, root);
	}

	private IContainer findContainer(IContainer[] containers) {
		// filter out non accessible containers
		List<IContainer> validContainers = new ArrayList<IContainer>(containers.length);
		for (IContainer container : containers) {
			if (container.isAccessible())
				validContainers.add(container);
		}
		if (validContainers.size()==0)
			return null;
		if (validContainers.size()==1)
			return validContainers.get(0);
		// there is more than one accessible container
		// sort containers according to project path containment
		Collections.sort(validContainers, new Comparator<IContainer>(){
			public int compare(IContainer o1, IContainer o2) {
				IPath location1 = o1.getProject().getLocation();
				IPath location2 = o2.getProject().getLocation();
				if(location1.equals(location2))
					return 0;
				if(location1.isPrefixOf(location2))
					return -1;
				return 1;
			}});
		// take the container of the innermost project
		return validContainers.get(validContainers.size()-1);
	}
}
