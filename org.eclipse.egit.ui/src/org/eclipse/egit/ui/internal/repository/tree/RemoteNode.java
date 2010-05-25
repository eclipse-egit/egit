package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Remote" node
 */
public class RemoteNode extends RepositoryTreeNode<String> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 * @param remoteName
	 *            the name of the remote specification
	 */
	public RemoteNode(RepositoryTreeNode parent, Repository repository,
			String remoteName) {
		super(parent, RepositoryTreeNodeType.REMOTE, repository, remoteName);
	}

}
