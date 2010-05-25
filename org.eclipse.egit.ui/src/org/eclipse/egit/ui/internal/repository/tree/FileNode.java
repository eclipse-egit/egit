package org.eclipse.egit.ui.internal.repository.tree;

import java.io.File;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents a File in the working directory tree
 */
public class FileNode extends RepositoryTreeNode<File> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param file
	 *            the file
	 */
	public FileNode(RepositoryTreeNode parent, Repository repository, File file) {
		super(parent, RepositoryTreeNodeType.FILE, repository, file);
	}

}
