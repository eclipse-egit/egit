/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * Copyright (c) 2011, Matthias Sohn <matthias.sohn@sap.com>
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
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.fetch.FetchOperationUI;
import org.eclipse.egit.ui.internal.fetch.SimpleConfigureFetchDialog;
import org.eclipse.egit.ui.internal.repository.tree.FetchNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

/**
 * Fetches from the remote
 */
public class FetchConfiguredRemoteCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode>
		implements IElementUpdater {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode node = getSelectedNodes(event).get(0);
		RemoteConfig config = getRemoteConfig(node);
		if (config == null) {
			MessageDialog
					.openInformation(
							getShell(event),
							UIText.SimpleFetchActionHandler_NothingToFetchDialogTitle,
							UIText.SimpleFetchActionHandler_NothingToFetchDialogMessage);
			return null;
		}
		int timeout = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		new FetchOperationUI(node.getRepository(), config, timeout, false)
				.start();
		return null;
	}

	@Override
	public boolean isEnabled() {
		RepositoryTreeNode node = getSelectedNodes().get(0);
		try {
			return getRemoteConfigCached(node) != null;
		} catch (ExecutionException e) {
			return false;
		}
	}

	private RemoteConfig getRemoteConfig(RepositoryTreeNode node)
			throws ExecutionException {
		if (node instanceof RepositoryNode) {
			return SimpleConfigureFetchDialog
					.getConfiguredRemote(node.getRepository());
		}
		if (node instanceof FetchNode) {
			node = node.getParent();
		}
		if (node instanceof RemoteNode) {
			try {
				RemoteNode remote = (RemoteNode) node;
				return new RemoteConfig(node.getRepository().getConfig(),
						remote.getObject());
			} catch (URISyntaxException e) {
				throw new ExecutionException(e.getMessage());
			}
		}
		return null;
	}

	private RemoteConfig getRemoteConfigCached(RepositoryTreeNode node)
			throws ExecutionException {
		if (node instanceof RepositoryNode) {
			return SimpleConfigureFetchDialog
					.getConfiguredRemoteCached(node.getRepository());
		}
		if (node instanceof FetchNode) {
			node = node.getParent();
		}
		if (node instanceof RemoteNode) {
			try {
				RemoteNode remote = (RemoteNode) node;
				return new RemoteConfig(SelectionRepositoryStateCache.INSTANCE
						.getConfig(node.getRepository()), remote.getObject());
			} catch (URISyntaxException e) {
				throw new ExecutionException(e.getMessage());
			}
		}
		return null;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		List<RepositoryTreeNode> nodes = getSelectedNodes();
		if (nodes.size() == 1) {
			RepositoryTreeNode node = nodes.get(0);
			if (node instanceof FetchNode || node instanceof RemoteNode) {
				// do nothing
			} else {
				try {
					RemoteConfig config = getRemoteConfigCached(node);
					if (config != null) {
						element.setText(SimpleConfigureFetchDialog
								.getSimpleFetchCommandLabel(config));
					}
				} catch (ExecutionException e) {
					// ignore - no label update
				}
			}
		}
	}
}
