package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Reference" node
 */
public class RefNode extends RepositoryTreeNode<Ref> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param ref
	 *            the reference
	 */
	public RefNode(RepositoryTreeNode parent, Repository repository, Ref ref) {
		super(parent, RepositoryTreeNodeType.REF, repository, ref);
	}

}
