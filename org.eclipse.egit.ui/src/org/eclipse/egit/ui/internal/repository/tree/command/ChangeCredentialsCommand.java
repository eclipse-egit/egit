/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.credentials.LoginDialog;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jgit.transport.URIish;

/**
 * Change credentials command
 */
public class ChangeCredentialsCommand extends
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
			Activator.error(e.getMessage(), e);
			return null;
		}
		UserPasswordCredentials credentials = LoginDialog.changeCredentials(getShell(event), uri);
		if (credentials == null)
			return null;
		try {
			Activator.getDefault().getSecureStore().putCredentials(uri, credentials);
		} catch (StorageException e) {
			Activator.error(UIText.ChangeCredentialsCommand_writingToSecureStoreFailed, e);
		}
		return null;
	}
}
