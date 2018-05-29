/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * Copyright (c) 2011, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (c) 2011, Dariusz Luksza <dariusz@luksa.org>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org - set action disabled when there is
 *    										no configuration for remotes
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Pushes to the remote
 */
public class PushConfiguredRemoteCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode<?>> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode node = getSelectedNodes(event).get(0);
		RemoteConfig config = getRemoteConfig(node);
		if (config == null) {
			MessageDialog.openInformation(getShell(event),
					UIText.SimplePushActionHandler_NothingToPushDialogTitle,
					UIText.SimplePushActionHandler_NothingToPushDialogMessage);
			return null;
		}
		new PushOperationUI(node.getRepository(), config.getName(), false)
				.start();
		return null;
	}

	@Override
	public boolean isEnabled() {
		RepositoryTreeNode<?> node = getSelectedNodes().get(0);
		return getRemoteConfig(node) != null;
	}

	private RemoteConfig getRemoteConfig(RepositoryTreeNode node) {
		if (node instanceof RepositoryNode)
			return SimpleConfigurePushDialog.getConfiguredRemote(node
					.getRepository());

		if (node instanceof RemoteNode || node instanceof PushNode) {
			RemoteNode remoteNode;
			if (node instanceof PushNode)
				remoteNode = (RemoteNode) node.getParent();
			else
				remoteNode = (RemoteNode) node;

			try {
				RemoteConfig config = new RemoteConfig(remoteNode
						.getRepository().getConfig(), remoteNode.getObject());
				boolean fetchConfigured = !config.getFetchRefSpecs().isEmpty();
				boolean pushConfigured = !config.getPushRefSpecs().isEmpty();
				if (fetchConfigured || pushConfigured)
					return config;
				else
					return null;
			} catch (URISyntaxException e) {
				return null;
			}
		}

		return null;
	}

}
