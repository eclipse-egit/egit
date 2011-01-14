/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.fetch.FetchConfiguredRemoteAction;
import org.eclipse.egit.ui.internal.fetch.SimpleConfigureFetchDialog;
import org.eclipse.egit.ui.internal.repository.tree.FetchNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Fetches from the remote
 */
public class FetchConfiguredRemoteCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode treeNode = getSelectedNodes(event).get(0);
		if (treeNode instanceof FetchNode) {
			FetchNode node = (FetchNode) treeNode;
			RemoteNode remote = (RemoteNode) node.getParent();

			try {
				RemoteConfig config = new RemoteConfig(node.getRepository()
						.getConfig(), remote.getObject());
				new FetchConfiguredRemoteAction(node.getRepository(), config,
						Activator.getDefault().getPreferenceStore().getInt(
								UIPreferences.REMOTE_CONNECTION_TIMEOUT))
						.start();
			} catch (URISyntaxException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		} else if (treeNode instanceof RepositoryNode) {
			Repository repository = treeNode.getRepository();
			if (SimpleConfigureFetchDialog.shouldConfigure(repository)) {
				Dialog dlg = SimpleConfigureFetchDialog.getDialog(
						getShell(event), repository);

				dlg.open();
			} else {
				FetchConfiguredRemoteAction op = new FetchConfiguredRemoteAction(
						repository, SimpleConfigureFetchDialog
								.getConfiguredRemote(repository),
						Activator.getDefault().getPreferenceStore().getInt(
								UIPreferences.REMOTE_CONNECTION_TIMEOUT));
				op.start();
			}
		}
		return null;
	}
}
