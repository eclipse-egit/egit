package org.eclipse.egit.ui.internal.repository.tree;

/***/
public class RepositoryGroupNode extends RepositoryTreeNode<String> {

	private boolean hasChildren;

	/**
	 * @param parent
	 * @param groupName
	 * @param hasChildren
	 */
	public RepositoryGroupNode(RepositoryTreeNode parent, String groupName,
			boolean hasChildren) {
		super(parent, RepositoryTreeNodeType.REPOGROUP, null, groupName);
		this.hasChildren = hasChildren;
	}

	/**
	 * @return whether there are repositories in this group
	 */
	public boolean hasChildren() {
		return hasChildren;
	}

}
