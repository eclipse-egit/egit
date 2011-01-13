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
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Pushes to the remote
 */
public class PushConfiguredRemoteCommand extends
		RepositoriesViewCommandHandler<PushNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		PushNode node = getSelectedNodes(event).get(0);
		RemoteNode remote = (RemoteNode) node.getParent();
		RemoteConfig config;
		try {
			config = new RemoteConfig(node.getRepository().getConfig(), remote
					.getObject());
		} catch (URISyntaxException e) {
			throw new ExecutionException(e.getMessage());
		}
		new PushConfiguredRemoteAction(node.getRepository(), config,
				Activator.getDefault().getPreferenceStore().getInt(
						UIPreferences.REMOTE_CONNECTION_TIMEOUT)).start();

		return null;
	}
}
