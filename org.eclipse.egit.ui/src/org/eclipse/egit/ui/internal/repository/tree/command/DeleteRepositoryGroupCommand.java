package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;

/**
 * Deletes a repository group, the repositories themselves are not affeced
 */
public class DeleteRepositoryGroupCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<String> groupsToDelete = new ArrayList<>();
		List<RepositoryTreeNode> elements = getSelectedNodes();
		for (Object element : elements) {
			if (element instanceof RepositoryGroupNode) {
				groupsToDelete.add(((RepositoryGroupNode) element).getObject());
			}
		}
		if (!groupsToDelete.isEmpty()) {
			RepositoryGroups groups = new RepositoryGroups();
			groups.delete(groupsToDelete);
			getView(event).refresh();
		}
		return null;
	}
}
