/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * Copyright (c) 2011, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (c) 2011, Dariusz Luksza <dariusz@luksa.org>
 * Copyright (c) 2022, Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushConfig;
import org.eclipse.jgit.transport.PushConfig.PushDefault;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

/**
 * Pushes to the remote.
 */
public class PushConfiguredRemoteCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode<?>>
		implements IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode node = getSelectedNodes(event).get(0);
		Repository repository = node != null ? node.getRepository() : null;
		if (repository == null) {
			return null;
		}
		if (node instanceof RepositoryNode) {
			try {
				PushOperationUI.pushToUpstream(getShell(event),
						repository);
			} catch (IOException e) {
				throw new ExecutionException(e.getLocalizedMessage(), e);
			}
		} else {
			RemoteConfig config = getRemoteConfig(node);
			if (config == null) {
				MessageDialog.openInformation(getShell(event),
						UIText.SimplePushActionHandler_NothingToPushDialogTitle,
						UIText.SimplePushActionHandler_NothingToPushDialogMessage);
				return null;
			}
			new PushOperationUI(repository, config.getName(), false).start();
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		RepositoryTreeNode<?> node = getSelectedNodes().get(0);
		if (node instanceof RepositoryNode) {
			RemoteConfig config = getRemoteConfigCached(node);
			if (config == null) {
				return false;
			}
			List<RefSpec> refSpecs = config.getPushRefSpecs();
			if (!refSpecs.isEmpty()) {
				// It's too expensive to determine if anything would match
				return true;
			}
			PushDefault pushDefault = SelectionRepositoryStateCache.INSTANCE
					.getConfig(node.getRepository()).get(PushConfig::new)
					.getPushDefault();
			return !PushDefault.NOTHING.equals(pushDefault);

		}
		return getRemoteConfigCached(node) != null;
	}

	private RemoteConfig getRemoteConfig(RepositoryTreeNode node) {
		if (node instanceof RepositoryNode) {
			return SimpleConfigurePushDialog.getConfiguredRemote(node
					.getRepository());
		}
		if (node instanceof PushNode) {
			node = node.getParent();
		}
		if (node instanceof RemoteNode) {
			try {
				RemoteNode remoteNode = (RemoteNode) node;
				RemoteConfig config = new RemoteConfig(
						remoteNode.getRepository().getConfig(),
						remoteNode.getObject());
				return withRefSpecs(config);
			} catch (URISyntaxException e) {
				return null;
			}
		}
		return null;
	}

	private RemoteConfig getRemoteConfigCached(RepositoryTreeNode node) {
		if (node instanceof RepositoryNode) {
			return SimpleConfigurePushDialog
					.getConfiguredRemoteCached(node.getRepository());
		}
		if (node instanceof PushNode) {
			node = node.getParent();
		}
		if (node instanceof RemoteNode) {
			try {
				RemoteNode remoteNode = (RemoteNode) node;
				RemoteConfig config = new RemoteConfig(
						SelectionRepositoryStateCache.INSTANCE
								.getConfig(node.getRepository()),
						remoteNode.getObject());
				return withRefSpecs(config);
			} catch (URISyntaxException e) {
				return null;
			}
		}
		return null;
	}

	private RemoteConfig withRefSpecs(RemoteConfig config) {
		boolean fetchConfigured = !config.getFetchRefSpecs().isEmpty();
		boolean pushConfigured = !config.getPushRefSpecs().isEmpty();
		if (fetchConfigured || pushConfigured) {
			return config;
		}
		return null;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		List<RepositoryTreeNode<?>> nodes = getSelectedNodes();
		if (nodes.size() == 1) {
			RepositoryTreeNode<?> node = nodes.get(0);
			if (node instanceof PushNode || node instanceof RemoteNode) {
				// do nothing
			} else {
				RemoteConfig config = getRemoteConfigCached(node);
				if (config != null) {
					element.setText(SimpleConfigurePushDialog
							.getSimplePushCommandLabel(config));
				}
			}
		}
	}
}
