package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Branches" node
 */
public class BranchesNode extends RepositoryTreeNode<Repository> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 */
	public BranchesNode(RepositoryTreeNode parent, Repository repository) {
		super(parent, RepositoryTreeNodeType.BRANCHES, repository, repository);
	}

}
