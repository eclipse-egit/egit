package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Tags" node
 */
public class TagsNode extends RepositoryTreeNode<Repository> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 */
	public TagsNode(RepositoryTreeNode parent, Repository repository) {
		super(parent, RepositoryTreeNodeType.TAGS, repository, repository);
	}

}
