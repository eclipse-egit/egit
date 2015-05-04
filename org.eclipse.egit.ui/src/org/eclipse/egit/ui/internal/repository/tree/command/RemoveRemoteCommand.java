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

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.osgi.util.NLS;

/**
 * Removes a remote
 */
public class RemoveRemoteCommand extends
		RepositoriesViewCommandHandler<RemoteNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RemoteNode node = getSelectedNodes(event).get(0);
		final String configName = node.getObject();

		boolean ok = MessageDialog.openConfirm(getShell(event),
				UIText.RepositoriesView_ConfirmDeleteRemoteHeader, NLS.bind(
						UIText.RepositoriesView_ConfirmDeleteRemoteMessage,
						configName));
		if (ok) {
			StoredConfig config = node.getRepository().getConfig();
			config.unsetSection(RepositoriesView.REMOTE, configName);
			try {
				config.save();
			} catch (IOException e1) {
				Activator.handleError(UIText.RepositoriesView_ErrorHeader, e1,
						true);
			}
		}

		return null;
	}
}
