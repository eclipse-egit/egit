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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.ConfigureRemoteWizard;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * Configures the Push
 */
public class ConfigurePushCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode selectedNode = getSelectedNodes(event).get(0);
		final String configName;

		if (selectedNode instanceof RemoteNode)
			configName = ((RemoteNode) selectedNode).getObject();
		else if (selectedNode instanceof PushNode)
			configName = ((RemoteNode) selectedNode.getParent()).getObject();
		else
			return null;

		WizardDialog dlg = new WizardDialog(
				getShell(event), new ConfigureRemoteWizard(
						selectedNode.getRepository(), configName, true));
		dlg.open();

		return null;
	}
}
