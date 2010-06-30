/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.resource;

import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.lib.FileTreeEntry;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;

/**
 * Representation of folder stored in Git repository
 */
public class GitFolder extends GitContainer implements IFolder {

	private final Tree tree;

	/**
	 * Creates GitFolder instance.
	 *
	 * @param parent
	 *            of this folder
	 * @param tree
	 *            Git {@link Tree} that is connected with this project
	 */
	public GitFolder(IContainer parent, Tree tree) {
		super(parent, tree);

		this.tree = tree;
	}

	public void create(boolean force, boolean local, IProgressMonitor monitor)
			throws CoreException {
		// unused
	}

	public void create(int updateFlags, boolean local, IProgressMonitor monitor)
			throws CoreException {
		// unused
	}

	public void createLink(IPath localLocation, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// unused
	}

	public void createLink(URI location, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// unused
	}

	public void delete(boolean force, boolean keepHistory,
			IProgressMonitor monitor) throws CoreException {
		// unused
	}

	public IFile getFile(String name) {
		TreeEntry member = findBlobMember(tree, name);
		if (member != null && member instanceof FileTreeEntry)
			return new GitFile(parent, (FileTreeEntry) member);

		return null;
	}

	public IFolder getFolder(String name) {
		return super.getFolder(name);
	}

	public void move(IPath destination, boolean force, boolean keepHistory,
			IProgressMonitor monitor) throws CoreException {
		// unused
	}

	@Override
	public int getType() {
		return IResource.FOLDER;
	}

}
