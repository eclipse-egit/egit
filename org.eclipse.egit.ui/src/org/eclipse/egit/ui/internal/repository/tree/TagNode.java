package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Tag" node
 */
public class TagNode extends RepositoryTreeNode<Ref> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param ref
	 *            the tag reference
	 */
	public TagNode(RepositoryTreeNode parent, Repository repository, Ref ref) {
		super(parent, RepositoryTreeNodeType.TAG, repository, ref);
	}

}
