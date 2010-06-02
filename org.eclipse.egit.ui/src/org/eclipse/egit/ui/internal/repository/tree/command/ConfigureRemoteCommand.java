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
import org.eclipse.egit.ui.internal.repository.NewRemoteWizard;
import org.eclipse.egit.ui.internal.repository.tree.RemotesNode;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * Configures the Remote
 *
 */
public class ConfigureRemoteCommand extends
		RepositoriesViewCommandHandler<RemotesNode> {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		RemotesNode node = getSelectedNodes(event).get(0);

		WizardDialog dlg = new WizardDialog(
				getView(event).getSite().getShell(), new NewRemoteWizard(node
						.getRepository()));
		dlg.open();

		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

}
