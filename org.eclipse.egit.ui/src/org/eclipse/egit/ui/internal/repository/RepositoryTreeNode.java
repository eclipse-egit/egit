package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

/**
 * A node in the Git Repositories view tree
 *
 * @param <T>
 *            the type
 */
class RepositoryTreeNode<T> {

	private final Repository myRepository;

	private final T myObject;

	private final RepositoryTreeNodeType myType;

	private final RepositoryTreeNode myParent;

	private String branch;

	RepositoryTreeNode(RepositoryTreeNode parent, RepositoryTreeNodeType type,
			Repository repository, T treeObject) {
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
	 * We keep this cached in the repository node to avoid repeated lookups
	 *
	 * @return the full branch
	 * @throws IOException
	 */
	public String getBranch() throws IOException {
		if (myType != RepositoryTreeNodeType.REPO) {
			return getRepositoryNode().getBranch();
		}
		if (branch == null) {
			branch = getRepository().getBranch();
		}
		return branch;
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
	 * <td>{@link RepositoryTreeNodeType#PROJ}</td>
	 * <td>{@link File}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#PROJECTS}</td>
	 * <td>{@link String}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link RepositoryTreeNodeType#REF}</td>
	 * <td>{@link Ref}</td>
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
		case PROJECTS:
		case BRANCHES:
			result = prime
					* result
					+ ((myObject == null) ? 0 : ((Repository) myObject)
							.getDirectory().hashCode());
			break;
		case REF:
			result = prime
					* result
					+ ((myObject == null) ? 0 : ((Ref) myObject).getName()
							.hashCode());
			break;
		case PROJ:
			result = prime
					* result
					+ ((myObject == null) ? 0 : ((File) myObject).getPath()
							.hashCode());
			break;

		default:
			break;
		}

		result = prime * result
				+ ((myParent == null) ? 0 : myParent.hashCode());
		result = prime
				* result
				+ ((myRepository == null) ? 0 : myRepository.getDirectory()
						.hashCode());
		result = prime * result + ((myType == null) ? 0 : myType.hashCode());
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

	private boolean checkObjectsEqual(Object otherObject) {
		switch (myType) {
		case REPO:
		case PROJECTS:
		case REMOTES:
		case BRANCHES:
			return ((Repository) myObject).getDirectory().equals(
					((Repository) otherObject).getDirectory());
		case REF:
			return ((Ref) myObject).getName().equals(
					((Ref) otherObject).getName());
		case PROJ:
			return ((File) myObject).getPath().equals(
					((File) otherObject).getPath());
		case REMOTE:
				return myObject.equals(otherObject);
		}
		return false;
	}

	enum RepositoryTreeNodeType {

		REPO(Activator.ICON_REPOSITORY), //
		REF(PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_FOLDER)), //
		PROJ(PlatformUI.getWorkbench().getSharedImages().getImage(
				SharedImages.IMG_OBJ_PROJECT_CLOSED)), //
		BRANCHES(Activator.ICON_BRANCHES), //
		PROJECTS(PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_FOLDER)), //
		REMOTES(Activator.ICON_REMOTE), //
		REMOTE(PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_FOLDER))

		;

		private final Image myImage;

		private RepositoryTreeNodeType(String iconName) {

			if (iconName != null) {
				myImage = Activator.getDefault().getImageRegistry().get(
						iconName);
			} else {
				myImage = null;
			}

		}

		private RepositoryTreeNodeType(Image icon) {
			myImage = icon;

		}

		public Image getIcon() {
			return myImage;
		}

	}

}
