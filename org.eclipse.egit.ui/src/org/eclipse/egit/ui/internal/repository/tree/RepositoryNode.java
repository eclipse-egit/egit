package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Repository" node
 */
public class RepositoryNode extends RepositoryTreeNode<Repository> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 */
	public RepositoryNode(RepositoryTreeNode parent, Repository repository) {
		super(parent, RepositoryTreeNodeType.REPO, repository, repository);
	}

}
