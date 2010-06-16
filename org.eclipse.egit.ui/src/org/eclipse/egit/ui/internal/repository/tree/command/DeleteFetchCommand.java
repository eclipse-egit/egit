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
import org.eclipse.egit.ui.internal.repository.tree.FetchNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.jgit.lib.RepositoryConfig;

/**
 * Deletes the Fetch
 */
public class DeleteFetchCommand extends
		RepositoriesViewCommandHandler<FetchNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FetchNode node = getSelectedNodes(event).get(0);
		RemoteNode remote = (RemoteNode) node.getParent();
		RepositoryConfig config = node.getRepository().getConfig();
		config.unset("remote", remote.getObject(), "url"); //$NON-NLS-1$ //$NON-NLS-2$
		config.unset("remote", remote.getObject(), "fetch"); //$NON-NLS-1$//$NON-NLS-2$
		try {
			config.save();
		} catch (IOException e1) {
			Activator.handleError(e1.getMessage(), e1, true);
		}

		return null;
	}
}
