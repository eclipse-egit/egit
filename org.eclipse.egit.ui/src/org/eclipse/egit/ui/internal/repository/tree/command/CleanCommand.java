package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.clean.CleanWizardDialog;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jgit.lib.Repository;

/**
 * Performs a clean operation on a repository.
 */
public class CleanCommand extends RepositoriesViewCommandHandler<RepositoryNode> {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryNode node = getSelectedNodes(event).get(0);
		Repository repository = node.getRepository();

		CleanWizardDialog dlg = new CleanWizardDialog(getShell(event), repository);
		dlg.setBlockOnOpen(true);
		dlg.open();

		return null;
	}

}
