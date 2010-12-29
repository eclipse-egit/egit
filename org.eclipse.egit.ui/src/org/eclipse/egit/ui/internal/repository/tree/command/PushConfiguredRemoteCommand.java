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
import org.eclipse.egit.ui.internal.push.PushConfiguredRemoteAction;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushWizard;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Pushes to the remote
 */
public class PushConfiguredRemoteCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode treeNode = getSelectedNodes(event).get(0);
		if (treeNode instanceof PushNode) {
			PushNode node = (PushNode) treeNode;
			RemoteNode remote = (RemoteNode) node.getParent();

			Repository repository = node.getRepository();
			RemoteConfig config;
			try {
				config = new RemoteConfig(repository.getConfig(), remote
						.getObject());
			} catch (URISyntaxException e) {
				throw new ExecutionException(e.getMessage(), e);
			}
			PushConfiguredRemoteAction op = new PushConfiguredRemoteAction(
					repository, config, Activator.getDefault()
							.getPreferenceStore().getInt(
									UIPreferences.REMOTE_CONNECTION_TIMEOUT));
			op.start();
		} else if (treeNode instanceof RepositoryNode) {
			Repository repository = treeNode.getRepository();
			RemoteConfig config = SimpleConfigurePushWizard
					.getConfiguredRemote(repository);
			SimpleConfigurePushWizard wiz = SimpleConfigurePushWizard
					.getWizard(repository, config);
			if (config == null || wiz != null) {
				new WizardDialog(getShell(event), wiz).open();
			} else {
				PushConfiguredRemoteAction op = new PushConfiguredRemoteAction(
						repository, config,
						Activator.getDefault().getPreferenceStore().getInt(
								UIPreferences.REMOTE_CONNECTION_TIMEOUT));
				op.start();
			}
		}
		return null;
	}
}
