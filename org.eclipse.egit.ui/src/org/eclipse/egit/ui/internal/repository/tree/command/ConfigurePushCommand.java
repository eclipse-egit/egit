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
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.Dialog;

/**
 * Configures the Push
 */
public class ConfigurePushCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode selectedNode = getFirstOrNull(getSelectedNodes(event));

		final String configName;
		if (selectedNode instanceof RemoteNode)
			configName = ((RemoteNode) selectedNode).getObject();
		else if (selectedNode instanceof PushNode)
			configName = ((RemoteNode) selectedNode.getParent()).getObject();
		else
			return null;

		Dialog dlg = SimpleConfigurePushDialog.getDialog(getShell(event),
				selectedNode.getRepository(), configName);
		dlg.open();
		return null;
	}
}
