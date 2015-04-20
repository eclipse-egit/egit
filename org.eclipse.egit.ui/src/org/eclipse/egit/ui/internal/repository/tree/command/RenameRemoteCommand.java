package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.dialogs.RemoteRenameDialog;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.swt.widgets.Shell;

/**
 * Renames a remote
 */
public class RenameRemoteCommand extends
		RepositoriesViewCommandHandler<RemoteNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RemoteNode node = getSelectedNodes(event).get(0);

		Shell shell = getShell(event);
		new RemoteRenameDialog(shell, node.getRepository(), node).open();
		return null;
	}
}