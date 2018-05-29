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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.dialogs.NewRemoteDialog;
import org.eclipse.egit.ui.internal.fetch.SimpleConfigureFetchDialog;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.egit.ui.internal.repository.tree.RemotesNode;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;

/**
 * Configures the Remote
 */
public class ConfigureRemoteCommand extends
		RepositoriesViewCommandHandler<RemotesNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RemotesNode node = getSelectedNodes(event).get(0);
		Repository repository = node.getRepository();

		NewRemoteDialog nrd = new NewRemoteDialog(getShell(event), repository);
		if (nrd.open() != Window.OK)
			return null;

		if (nrd.getPushMode())
			SimpleConfigurePushDialog.getDialog(getShell(event), repository,
					nrd.getName()).open();
		else
			SimpleConfigureFetchDialog.getDialog(getShell(event), repository,
					nrd.getName()).open();
		return null;
	}
}
