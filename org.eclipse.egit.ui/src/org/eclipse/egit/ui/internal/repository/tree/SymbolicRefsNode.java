package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Symbolic References" node
 */
public class SymbolicRefsNode extends RepositoryTreeNode<Repository> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 */
	public SymbolicRefsNode(RepositoryTreeNode parent, Repository repository) {
		super(parent, RepositoryTreeNodeType.SYMBOLICREFS, repository,
				repository);
	}

}
