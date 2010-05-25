package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.jgit.lib.Repository;

/**
 * Represents the "Working Directory" node
 */
public class WorkingDirNode extends RepositoryTreeNode<Repository> {

	/**
	 * Constructs the node.
	 * 
	 * @param parent
	 *            the parent node (may be null)
	 * @param repository
	 *            the {@link Repository}
	 */
	public WorkingDirNode(RepositoryTreeNode parent, Repository repository) {
		super(parent, RepositoryTreeNodeType.WORKINGDIR, repository, repository);
	}

}
