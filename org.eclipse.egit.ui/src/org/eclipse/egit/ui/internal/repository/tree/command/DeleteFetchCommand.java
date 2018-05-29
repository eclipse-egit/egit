/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.jgit.lib.StoredConfig;

/**
 * Deletes the Fetch
 */
public class DeleteFetchCommand extends
		RepositoriesViewCommandHandler<FetchNode> {

	private static final String REMOTE = "remote"; //$NON-NLS-1$

	private static final String FETCH = "fetch"; //$NON-NLS-1$

	private static final String PUSH = "push"; //$NON-NLS-1$

	private static final String URL = "url"; //$NON-NLS-1$

	private static final String PUSHURL = "pushurl"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FetchNode node = getSelectedNodes(event).get(0);
		RemoteNode remote = (RemoteNode) node.getParent();

		StoredConfig config = node.getRepository().getConfig();
		String fetchUrl = config.getString(REMOTE, remote.getObject(), URL);
		config.unset(REMOTE, remote.getObject(), FETCH);
		config.unset(REMOTE, remote.getObject(), URL);
		// the push URL may still be needed for fetch
		if (fetchUrl != null) {
			boolean hasPush = config.getStringList(REMOTE, remote.getObject(),
					PUSH).length > 0;
			if (hasPush) {
				String[] pushurls = config.getStringList(REMOTE, remote
						.getObject(), PUSHURL);
				// if there are not specific push urls,
				// copy the former fetch url into push url
				if (pushurls.length == 0)
					config.setString(REMOTE, remote.getObject(), PUSHURL,
							fetchUrl);
			}
		}

		try {
			config.save();
		} catch (IOException e1) {
			Activator.handleError(e1.getMessage(), e1, true);
		}

		return null;
	}
}
