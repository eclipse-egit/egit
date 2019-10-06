/*******************************************************************************
 * Copyright (c) 2010, 2019 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Alexander Nittka <alex@nittka.de> - Bug 545123
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * A node in the Git Repositories view tree
 *
 * @param <T>
 *            the type
 */
public abstract class RepositoryTreeNode<T> extends PlatformObject implements Comparable<RepositoryTreeNode> {

	private Repository myRepository;

	private T myObject;

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
		Repository repository = getRepository();
		if (repository == null) {
			return null;
		}
		return new Path(getRepository().getWorkTree().getAbsolutePath());
	}

	/**
	 * Removes the references to the repository and to the object. <strong>Call
	 * only after this node has been removed from the view!</strong>
	 * <p>
	 * The CommonViewer framework keeps on to a hard reference to the last
	 * selection, even if that no longer will appear in the view. Moreover, the
	 * WorkbenchSourceProvider may also hold such a reference to the
	 * RepositoryNode(s). This will preclude for some time the garbage
	 * collection and eventual removal of the Repository instance
	 * (RepositoryCache relies on WeakReference semantics). Therefore, this
	 * operation provides a means to clear the reference to the Repository in a
	 * now otherwise unreferenced RepositoryNode.
	 */
	public void clear() {
		myRepository = null;
		myObject = null;
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
	 * <td>{@link RepositoryTreeNodeType#BRANCHHIERARCHY}</td>
	 * <td>{@link IPath}</td>
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
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#REPOGROUP}</td>
	 * <td>{@link RepositoryGroup}</td>
	 * </tr>
	 * </table>
	 *
	 * @return the type-specific object
	 */
	public T getObject() {
		return myObject;
	}

	// should the node parent considered when calculating equals/hashcode
	private boolean considerParent(RepositoryTreeNodeType nodeType) {
		return nodeType != RepositoryTreeNodeType.REPO;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		switch (myType) {
		case REPO:
		case REMOTES:
		case LOCAL:
		case REMOTETRACKING:
		case BRANCHES:
		case ADDITIONALREFS:
		case SUBMODULES:
		case STASH:
		case WORKINGDIR:
			result = prime
					* result
					+ ((myObject == null) ? 0 : ((Repository) myObject)
							.getDirectory().hashCode());
			break;
		case REF:
		case TAG:
		case ADDITIONALREF:
			result = prime
					* result
					+ ((myObject == null) ? 0 : ((Ref) myObject).getName()
							.hashCode());
			break;
		case FILE:
		case FOLDER:
			result = prime
					* result
					+ ((myObject == null) ? 0 : ((File) myObject).getPath()
							.hashCode());
			break;
		case TAGS:
		case REMOTE:
		case PUSH:
		case FETCH:
		case BRANCHHIERARCHY:
		case STASHED_COMMIT:
		case REPOGROUP:
		case ERROR:
			result = prime * result
					+ ((myObject == null) ? 0 : myObject.hashCode());

		}

		if (considerParent(myType)) {
			result = prime * result
					+ ((myParent == null) ? 0 : myParent.hashCode());
		}
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
		if (considerParent(myType)) {
			if (myParent == null) {
				if (other.myParent != null) {
					return false;
				}
			} else if (!myParent.equals(other.myParent)) {
				return false;
			}
		}
		if (myRepository == null) {
			if (other.myRepository != null) {
				return false;
			}
		} else {
			if (other.myRepository == null) {
				return false;
			}
			if (!myRepository.getDirectory()
					.equals(other.myRepository.getDirectory())) {
				return false;
			}
		}
		if (myObject == null) {
			if (other.myObject != null) {
				return false;
			}
		} else {
			if (other.myObject == null) {
				return false;
			}
			if (!checkObjectsEqual(other.myObject)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int compareTo(RepositoryTreeNode otherNode) {
		int typeDiff = this.myType.ordinal() - otherNode.getType().ordinal();
		if (typeDiff != 0) {
			return typeDiff;
		}
		// we only implement this for sorting, so we only have to
		// implement this for nodes that can be on the same level
		// i.e. siblings to each other

		switch (myType) {
		case REPOGROUP:
			return CommonUtils.STRING_ASCENDING_COMPARATOR.compare(
					((RepositoryGroup) myObject).getName(),
					((RepositoryGroup) otherNode.getObject()).getName());
		case BRANCHES:
		case LOCAL:
		case REMOTETRACKING:
		case BRANCHHIERARCHY:
			return CommonUtils.STRING_ASCENDING_COMPARATOR.compare(
					myObject.toString(), otherNode.getObject().toString());
		case REMOTES:
		case ADDITIONALREFS:
		case TAGS:
		case ERROR:
		case SUBMODULES:
		case STASH:
		case WORKINGDIR:
			return 0;

		case FETCH:
		case PUSH:
		case REMOTE:
			return CommonUtils.STRING_ASCENDING_COMPARATOR
					.compare((String) myObject, (String) otherNode.getObject());
		case FILE:
		case FOLDER:
			return CommonUtils.STRING_ASCENDING_COMPARATOR
					.compare(((File) myObject).getName(),
					((File) otherNode.getObject()).getName());
		case STASHED_COMMIT:
			// ok for positive indexes < ~2 billion
			return ((StashedCommitNode) this).getIndex()
					- ((StashedCommitNode) otherNode).getIndex();
		case TAG:
		case ADDITIONALREF:
		case REF:
			return CommonUtils.REF_ASCENDING_COMPARATOR.compare((Ref) myObject,
					(Ref) otherNode.getObject());
		case REPO:
			int nameCompare = CommonUtils.STRING_ASCENDING_COMPARATOR.compare(
					getDirectoryContainingRepo((Repository) myObject).getName(),
					getDirectoryContainingRepo(
							(Repository) otherNode.getObject())
									.getName());
			if (nameCompare != 0)
				return nameCompare;
			// if the name is not unique, let's look at the whole path
			return CommonUtils.STRING_ASCENDING_COMPARATOR.compare(
					getDirectoryContainingRepo((Repository) myObject)
							.getParentFile().getPath(),
					getDirectoryContainingRepo(
							(Repository) otherNode.getObject()).getParentFile()
									.getPath());
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
		case REMOTES:
		case BRANCHES:
		case LOCAL:
		case REMOTETRACKING:
		case ADDITIONALREFS:
		case SUBMODULES:
		case STASH:
		case WORKINGDIR:
			return ((Repository) myObject).getDirectory().equals(
					((Repository) otherObject).getDirectory());
		case REF:
		case TAG:
		case ADDITIONALREF:
			return ((Ref) myObject).getName().equals(
					((Ref) otherObject).getName());
		case FOLDER:
		case FILE:
			return ((File) myObject).getPath().equals(
					((File) otherObject).getPath());
		case ERROR:
		case REMOTE:
		case FETCH:
		case PUSH:
		case BRANCHHIERARCHY:
		case STASHED_COMMIT:
		case REPOGROUP:
		case TAGS:
			return myObject.equals(otherObject);
		}
		return false;
	}

	@Override
	public <X> X getAdapter(Class<X> adapter) {
		if (Repository.class == adapter && myRepository != null) {
			return adapter.cast(myRepository);
		}
		if (File.class == adapter) {
			return adapter.cast(adaptFile());
		}
		if (myObject != null) {
			if (adapter.isInstance(myObject)) {
				return adapter.cast(myObject);
			}
		}
		return super.getAdapter(adapter);
	}

	private File adaptFile() {
		switch (getType()) {
		case REPO:
		case WORKINGDIR:
		case FILE:
		case FOLDER:
			IPath path = getPath();
			return path != null ? path.toFile() : null;
		default:
			return null;
		}
	}

	@Override
	public String toString() {
		return "RepositoryNode[" + myType + ", " + myObject + ']'; //$NON-NLS-1$//$NON-NLS-2$
	}
}
