package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Symbolic Reference" node
 */
public class SymbolicRefNode extends RepositoryTreeNode<Ref> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param ref
	 *            the symbolic reference
	 */
	public SymbolicRefNode(RepositoryTreeNode parent, Repository repository,
			Ref ref) {
		super(parent, RepositoryTreeNodeType.SYMBOLICREF, repository, ref);
	}

}
