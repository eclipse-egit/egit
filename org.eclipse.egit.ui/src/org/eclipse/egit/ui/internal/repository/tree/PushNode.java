package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Push" node
 */
public class PushNode extends RepositoryTreeNode<String> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param pushUri
	 *            the push URI (or another suitable representation of the push)
	 */
	public PushNode(RepositoryTreeNode parent, Repository repository,
			String pushUri) {
		super(parent, RepositoryTreeNodeType.PUSH, repository, pushUri);
	}

}
