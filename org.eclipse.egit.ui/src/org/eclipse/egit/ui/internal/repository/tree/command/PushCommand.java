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

import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.PushBranchWizard;
import org.eclipse.egit.ui.internal.push.PushWizard;
import org.eclipse.egit.ui.internal.push.SimplePushRefWizard;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Ref;

/**
 * Implements "Push" from a Repository
 */
public class PushCommand extends RepositoriesViewCommandHandler<RepositoryNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode node = getSelectedNodes(event).get(0);

		IWizard pushWiz = null;

		try {
			switch (node.getType()) {
			case REF:
				pushWiz = new PushBranchWizard(node.getRepository(),
						(Ref) node.getObject());
				break;
			case TAG:
				pushWiz = new SimplePushRefWizard(node.getRepository(),
						(Ref) node.getObject(), UIText.PushCommand_pushTagTitle);
				break;
			case REPO:
				pushWiz = new PushWizard(node.getRepository());
				break;
			default:
				throw new UnsupportedOperationException("type not supported!"); //$NON-NLS-1$
			}
		} catch (URISyntaxException e1) {
			Activator.handleError(e1.getMessage(), e1, true);
		}

		WizardDialog dlg = new WizardDialog(getShell(event), pushWiz);
		dlg.setHelpAvailable(true);
		dlg.open();

		return null;
	}

	public void setEnabled(Object evaluationContext) {
		enableWhenRepositoryHaveHead(evaluationContext);
	}
}
