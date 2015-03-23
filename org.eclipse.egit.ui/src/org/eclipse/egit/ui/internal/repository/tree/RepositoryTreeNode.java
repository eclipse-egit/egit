/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * A node in the Git Repositories view tree
 *
 * @param <T>
 *            the type
 */
public abstract class RepositoryTreeNode<T> extends PlatformObject implements Comparable<RepositoryTreeNode> {

	private final Repository myRepository;

	private final T myObject;

	private final RepositoryTreeNodeType myType;

	private final RepositoryTreeNode myParent;

	/**
	 * Constructs a node
	 *
	 * @param parent
	 *            the parent (may be null)
	 * @param type
	 *            the type
	 * @param repository
	 *            the {@link Repository}
	 * @param treeObject
	 *            an object (depending on the type)
	 */
	public RepositoryTreeNode(RepositoryTreeNode parent,
			RepositoryTreeNodeType type, Repository repository, T treeObject) {
		myParent = parent;
		myRepository = repository;
		myType = type;
		myObject = treeObject;
	}

	/**
	 * @return the parent, or null
	 */
	public RepositoryTreeNode getParent() {
		return myParent;
	}

	/**
	 * @return the type
	 */
	public RepositoryTreeNodeType getType() {
		return myType;
	}

	/**
	 * @return the repository
	 */
	public Repository getRepository() {
		return myRepository;
	}

	/**
	 * @return the path of the file, folder or repository
	 */
	public IPath getPath() {
		return new Path(getRepository().getWorkTree().getAbsolutePath());
	}

	/**
	 * Depending on the node type, the returned type is:
	 *
	 * <table border=1>
	 * <th>Type</th>
	 * <th>Object type</th>
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#BRANCHES}</td>
	 * <td>{@link String}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#LOCAL}</td>
	 * <td>{@link String}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#REMOTETRACKING}</td>
	 * <td>{@link String}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#TAGS}</td>
	 * <td>{@link String}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#REMOTE}</td>
	 * <td>{@link String}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#REMOTES}</td>
	 * <td>{@link String}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#REPO}</td>
	 * <td>{@link Repository}</td>
	 * </tr>
	 * </table>
	 *
	 * @return the type-specific object
	 */
	public T getObject() {
		return myObject;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		switch (myType) {
		case REPO:
			// fall through
		case REMOTES:
			// fall through
		case LOCAL:
			// fall through
		case REMOTETRACKING:
			// fall through
		case BRANCHES:
			// fall through
		case ADDITIONALREFS:
			// fall through
		case SUBMODULES:
			// fall through
		case STASH:
			// fall through
		case WORKINGDIR:
			result = prime
					* result
					+ ((myObject == null) ? 0 : ((Repository) myObject)
							.getDirectory().hashCode());
			break;
		case REF:
			// fall through
		case TAG:
			// fall through
		case ADDITIONALREF:
			result = prime
					* result
					+ ((myObject == null) ? 0 : ((Ref) myObject).getName()
							.hashCode());
			break;
		case FILE:
			// fall through
		case FOLDER:
			result = prime
					* result
					+ ((myObject == null) ? 0 : ((File) myObject).getPath()
							.hashCode());
			break;
		case TAGS:
			// fall through
		case REMOTE:
			// fall through
		case PUSH:
			// fall through
		case FETCH:
			// fall through
		case BRANCHHIERARCHY:
			// fall through
		case STASHED_COMMIT:
			// fall through
		case ERROR:
			result = prime * result
					+ ((myObject == null) ? 0 : myObject.hashCode());

		}

		result = prime * result
				+ ((myParent == null) ? 0 : myParent.hashCode());
		result = prime
				* result
				+ ((myRepository == null) ? 0 : myRepository.getDirectory()
						.hashCode());
		result = prime * result + myType.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		RepositoryTreeNode other = (RepositoryTreeNode) obj;

		if (myType == null) {
			if (other.myType != null)
				return false;
		} else if (!myType.equals(other.myType))
			return false;
		if (myParent == null) {
			if (other.myParent != null)
				return false;
		} else if (!myParent.equals(other.myParent))
			return false;
		if (myRepository == null) {
			if (other.myRepository != null)
				return false;
		} else if (!myRepository.getDirectory().equals(
				other.myRepository.getDirectory()))
			return false;
		if (myObject == null) {
			if (other.myObject != null)
				return false;
		} else if (!checkObjectsEqual(other.myObject))
			return false;

		return true;
	}

	@Override
	public int compareTo(RepositoryTreeNode otherNode) {
		int typeDiff = this.myType.ordinal() - otherNode.getType().ordinal();
		if (typeDiff != 0)
			return typeDiff;

		// we only implement this for sorting, so we only have to
		// implement this for nodes that can be on the same level
		// i.e. siblings to each other

		switch (myType) {

		case BRANCHES:
			// fall through
		case LOCAL:
			// fall through
		case REMOTETRACKING:
			// fall through
		case BRANCHHIERARCHY:
			return myObject.toString().compareTo(
					otherNode.getObject().toString());
		case REMOTES:
			// fall through
		case ADDITIONALREFS:
			// fall through
		case TAGS:
			// fall through
		case ERROR:
			// fall through
		case SUBMODULES:
			// fall through
		case STASH:
			// fall through
		case WORKINGDIR:
			return 0;

		case FETCH:
			// fall through
		case PUSH:
			// fall through
		case REMOTE:
			return ((String) myObject)
					.compareTo((String) otherNode.getObject());
		case FILE:
			// fall through
		case FOLDER:
			return ((File) myObject).getName().compareTo(
					((File) otherNode.getObject()).getName());
		case STASHED_COMMIT:
			return ((RevCommit) myObject).compareTo(((RevCommit) otherNode
					.getObject()));
		case TAG:
			// fall through
		case ADDITIONALREF:
			// fall through
		case REF:
			return ((Ref) myObject).getName().compareTo(
					((Ref) otherNode.getObject()).getName());
		case REPO:
			int nameCompare = getDirectoryContainingRepo((Repository) myObject)
					.getName()
					.compareTo(
							getDirectoryContainingRepo((Repository) otherNode.getObject())
									.getName());
			if (nameCompare != 0)
				return nameCompare;
			// if the name is not unique, let's look at the whole path
			return getDirectoryContainingRepo((Repository) myObject)
					.getParentFile()
					.getPath()
					.compareTo(
							getDirectoryContainingRepo((Repository) otherNode.getObject())
									.getParentFile().getPath());
		}
		return 0;
	}

	private File getDirectoryContainingRepo(Repository repo) {
		if (!repo.isBare())
			return repo.getDirectory().getParentFile();
		else
			return repo.getDirectory();
	}

	private boolean checkObjectsEqual(Object otherObject) {
		switch (myType) {
		case REPO:
			// fall through
		case REMOTES:
			// fall through
		case BRANCHES:
			// fall through
		case LOCAL:
			// fall through
		case REMOTETRACKING:
			// fall through
		case ADDITIONALREFS:
			// fall through
		case SUBMODULES:
			// fall through
		case STASH:
			// fall through
		case WORKINGDIR:
			return ((Repository) myObject).getDirectory().equals(
					((Repository) otherObject).getDirectory());
		case REF:
			// fall through
		case TAG:
			// fall through
		case ADDITIONALREF:
			return ((Ref) myObject).getName().equals(
					((Ref) otherObject).getName());
		case FOLDER:
			// fall through
		case FILE:
			return ((File) myObject).getPath().equals(
					((File) otherObject).getPath());
		case ERROR:
			// fall through
		case REMOTE:
			// fall through
		case FETCH:
			// fall through
		case PUSH:
			// fall through
		case BRANCHHIERARCHY:
			// fall through
		case STASHED_COMMIT:
			// fall through
		case TAGS:
			return myObject.equals(otherObject);
		}
		return false;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (Repository.class == adapter && myRepository != null)
			return myRepository;
		return super.getAdapter(adapter);
	}

	@Override
	public String toString() {
		return "RepositoryNode[" + myType + ", " + myObject.toString() + "]";   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}
}
