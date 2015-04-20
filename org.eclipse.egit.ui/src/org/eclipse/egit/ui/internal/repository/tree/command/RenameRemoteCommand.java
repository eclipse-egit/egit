/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.dialogs.RemoteRenameDialog;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.swt.widgets.Shell;

/**
 * Renames a remote
 */
public class RenameRemoteCommand extends
		RepositoriesViewCommandHandler<RemoteNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RemoteNode node = getSelectedNodes(event).get(0);

		Shell shell = getShell(event);
		new RemoteRenameDialog(shell, node.getRepository(), node).open();
		return null;
	}
}