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
import org.eclipse.egit.ui.internal.repository.tree.FetchNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * Configures the Fetch
 */
public class ConfigureFetchCommand extends
		RepositoriesViewCommandHandler<RemoteNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode selectedNode = getSelectedNodes(event).get(0);
		final String configName;

		if (selectedNode instanceof RemoteNode)
			configName = ((RemoteNode) selectedNode).getObject();
		else if (selectedNode instanceof FetchNode)
			configName = ((RemoteNode) selectedNode.getParent()).getObject();
		else
			return null;

		WizardDialog dlg = new WizardDialog(
				getView(event).getSite().getShell(), new ConfigureRemoteWizard(
						selectedNode.getRepository(), configName, false));
		dlg.open();

		return null;
	}
}
