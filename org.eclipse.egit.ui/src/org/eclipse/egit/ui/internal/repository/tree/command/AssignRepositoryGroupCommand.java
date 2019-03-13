package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;

/**
 * Manage which repositories should belong to the selected group
 */
public class AssignRepositoryGroupCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryGroups groups = new RepositoryGroups();
		List<String> repositories = util.getConfiguredRepositories();
		List<RepositoryTreeNode> selection = getSelectedNodes(event);
		if (!selection.isEmpty()
				&& selection.get(0) instanceof RepositoryGroupNode) {
			String group = ((RepositoryGroupNode) selection.get(0)).getObject();
			System.out.println(repositories);
			System.out.println(group + " " + groups.getRepositories(group)); //$NON-NLS-1$
			// TODO dialog for selecting repositories for this group
		}
		return null;
	}
}
