/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
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

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * A node in the Git Repositories view tree
 *
 * @param <T>
 *            the type
 */
public abstract class RepositoryTreeNode<T> implements Comparable<RepositoryTreeNode> {

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

	@SuppressWarnings("unchecked")
	private RepositoryTreeNode<Repository> getRepositoryNode() {
		if (myType == RepositoryTreeNodeType.REPO) {
			return (RepositoryTreeNode<Repository>) this;
		} else {
			return getParent().getRepositoryNode();
		}
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
	 * <td>{@link RepositoryTreeNodeType#LOCALBRANCHES}</td>
	 * <td>{@link String}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#REMOTEBRANCHES}</td>
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
		case LOCALBRANCHES:
			// fall through
		case REMOTEBRANCHES:
			// fall through
		case BRANCHES:
			// fall through
		case SYMBOLICREFS:
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
		case SYMBOLICREF:
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
		case LOCALBRANCHES:
			// fall through
		case REMOTEBRANCHES:
			// fall through
		case REMOTES:
			// fall through
		case SYMBOLICREFS:
			// fall through
		case TAGS:
			// fall through
		case ERROR:
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
		case TAG:
			// fall through
		case SYMBOLICREF:
			// fall through
		case REF:
			return ((Ref) myObject).getName().compareTo(
					((Ref) otherNode.getObject()).getName());
		case REPO:
			int nameCompare = ((Repository) myObject).getDirectory()
					.getParentFile().getName().compareTo(
							(((Repository) otherNode.getObject())
									.getDirectory().getParentFile().getName()));
			if (nameCompare != 0)
				return nameCompare;
			// if the name is not unique, let's look at the whole path
			return ((Repository) myObject).getDirectory().getParentFile()
					.getParentFile().getPath().compareTo(
							(((Repository) otherNode.getObject())
									.getDirectory().getParentFile()
									.getParentFile().getPath()));

		}
		return 0;
	}

	private boolean checkObjectsEqual(Object otherObject) {
		switch (myType) {
		case REPO:
			// fall through
		case REMOTES:
			// fall through
		case BRANCHES:
			// fall through
		case LOCALBRANCHES:
			// fall through
		case REMOTEBRANCHES:
			// fall through
		case SYMBOLICREFS:
			// fall through
		case ERROR:
			// fall through
		case WORKINGDIR:
			return ((Repository) myObject).getDirectory().equals(
					((Repository) otherObject).getDirectory());
		case REF:
			// fall through
		case TAG:
			// fall through
		case SYMBOLICREF:
			return ((Ref) myObject).getName().equals(
					((Ref) otherObject).getName());
		case FOLDER:
			// fall through
		case FILE:
			return ((File) myObject).getPath().equals(
					((File) otherObject).getPath());
		case REMOTE:
			// fall through
		case FETCH:
			// fall through
		case PUSH:
			// fall through
		case TAGS:
			return myObject.equals(otherObject);
		}
		return false;
	}

}
