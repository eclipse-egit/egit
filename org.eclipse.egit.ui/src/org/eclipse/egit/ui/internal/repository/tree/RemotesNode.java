package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Remotes" node
 */
public class RemotesNode extends RepositoryTreeNode<Repository> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 */
	public RemotesNode(RepositoryTreeNode parent, Repository repository) {
		super(parent, RepositoryTreeNodeType.REMOTES, repository, repository);
	}

}
