package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Fetch" node
 */
public class FetchNode extends RepositoryTreeNode<String> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param fetchUri
	 *            the push URI (or another suitable representation of the fetch)
	 */
	public FetchNode(RepositoryTreeNode parent, Repository repository,
			String fetchUri) {
		super(parent, RepositoryTreeNodeType.FETCH, repository, fetchUri);
	}

}
