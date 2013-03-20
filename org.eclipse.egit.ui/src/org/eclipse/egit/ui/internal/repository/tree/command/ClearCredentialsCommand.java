/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.transport.URIish;

/**
 * Clear credentials command
 */
public class ClearCredentialsCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode<String>> {

	/**
	 * Execute the command
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode<String> node = getSelectedNodes(event).get(0);
		URIish uri;
		try {
			uri = new URIish(node.getObject());
		} catch (URISyntaxException e) {
			Activator.handleError(e.getMessage(), e, true);
			return null;
		}
		try {
			org.eclipse.egit.core.internal.Activator.getDefault().getSecureStore().clearCredentials(uri);
		} catch (IOException e) {
			Activator.handleError(UIText.ClearCredentialsCommand_clearingCredentialsFailed, e, true);
		}
		return null;
	}
}
