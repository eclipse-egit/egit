package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Error" node
 */
public class ErrorNode extends RepositoryTreeNode<String> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param error
	 *            the error message
	 */
	public ErrorNode(RepositoryTreeNode parent, Repository repository,
			String error) {
		super(parent, RepositoryTreeNodeType.ERROR, repository, error);
	}

}
