/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
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

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.dialogs.BranchConfigurationDialog;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.jgit.lib.Repository;

/**
 * "Configures" a branch
 */
public class ConfigureBranchCommand extends
		RepositoriesViewCommandHandler<RefNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final List<RefNode> nodes = getSelectedNodes(event);
		if (nodes.size() == 1) {
			RefNode node = nodes.get(0);
			String branchName = Repository.shortenRefName(node.getObject()
					.getName());
			BranchConfigurationDialog dlg = new BranchConfigurationDialog(
					getShell(event), branchName, node.getRepository());
			dlg.open();
		}
		return null;
	}
}
