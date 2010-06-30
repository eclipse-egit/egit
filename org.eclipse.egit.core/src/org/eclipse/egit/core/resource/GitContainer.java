/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.resource;

import java.io.IOException;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.lib.FileTreeEntry;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;

/**
 *
 */
public abstract class GitContainer extends GitResource implements IContainer {

	private final Tree containerTree;

	private GitResource[] members;

	GitContainer(IContainer parent, Tree containerTree) {
		super(parent, containerTree);

		this.containerTree = containerTree;
	}

	public boolean exists(IPath path) {
		String pathStr = path.toString();

		try {
			return containerTree.existsBlob(pathStr)
					|| containerTree.existsTree(pathStr);
		} catch (IOException e) {
			// TODO log exception
			return false;
		}
	}

	public IResource findMember(String name) {
		TreeEntry blobMember = findBlobMember(containerTree, name);
		if (blobMember != null && blobMember instanceof FileTreeEntry)
			return new GitFile(this, (FileTreeEntry) blobMember);

		TreeEntry treeMember = findTreeMember(containerTree, name);
		if (treeMember != null && treeMember instanceof Tree)
			return new GitFolder(this, (Tree) treeMember);

		return null;
	}

	public IResource findMember(String name, boolean includePhantoms) {
		return findMember(name);
	}

	public IResource findMember(IPath path) {
		return findMember(path.toString());
	}

	public IResource findMember(IPath path, boolean includePhantoms) {
		return findMember(path.toString());
	}

	public String getDefaultCharset() throws CoreException {
		// unused
		return null;
	}

	public String getDefaultCharset(boolean checkImplicit) throws CoreException {
		// unused
		return null;
	}

	public IFile getFile(IPath path) {
		TreeEntry member = findBlobMember(containerTree, path.toString());

		if (member != null && member instanceof FileTreeEntry)
			return new GitFile(this, (FileTreeEntry) member);

		return null;
	}

	public IFolder getFolder(IPath path) {
		return getFolder(path.toString());
	}

	public IResource[] members() throws CoreException {
		return members != null ? members : new IResource[0];
	}

	public IResource[] members(boolean includePhantoms) throws CoreException {
		return members();
	}

	public IResource[] members(int memberFlags) throws CoreException {
		return members();
	}

	public IFile[] findDeletedMembersWithHistory(int depth,
			IProgressMonitor monitor) throws CoreException {
		// unused
		return null;
	}

	public void setDefaultCharset(String charset) throws CoreException {
		// unused
	}

	public void setDefaultCharset(String charset, IProgressMonitor monitor)
			throws CoreException {
		// unused
	}

	public IResourceFilterDescription createFilter(int type,
			FileInfoMatcherDescription matcherDescription, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// unused
		return null;
	}

	public IResourceFilterDescription[] getFilters() throws CoreException {
		// unused
		return null;
	}

	public int getType() {
		return IResource.FOLDER;
	}

	/**
	 *
	 * @param path
	 * @return folder
	 */
	protected IFolder getFolder(String path) {
		for (GitResource resource : members) {
			if (resource.getType() == IResource.FOLDER
					&& resource.getName().equals(path))
				return (IFolder) resource;
		}

		return null;
	}
}
