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
import org.eclipse.egit.ui.internal.clone.GitCreateProjectViaWizardWizard;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * Implements "Add Projects" for Repository, Working Directory, and Folder
 */
public class ImportProjectsCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode node = getSelectedNodes(event).get(0);
		String path;

		switch (node.getType()) {
		case REPO:
			// fall through
		case WORKINGDIR:
			path = node.getRepository().getWorkTree().toString();
			break;
		case FOLDER:
			path = ((FolderNode) node).getObject().getPath().toString();
			break;
		default:
			return null;
		}

		WizardDialog dlg = new WizardDialog(
				getView(event).getSite().getShell(),
				new GitCreateProjectViaWizardWizard(node.getRepository(), path));
		dlg.open();

		return null;
	}
}
