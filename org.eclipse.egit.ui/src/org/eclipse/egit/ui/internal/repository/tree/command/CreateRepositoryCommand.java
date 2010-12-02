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
import org.eclipse.core.commands.IHandler;
import org.eclipse.egit.ui.internal.repository.NewRepositoryWizard;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * Implements "Create Repository"
 */
public class CreateRepositoryCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> implements IHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		WizardDialog dlg = new WizardDialog(getShell(event),
				new NewRepositoryWizard());
		dlg.setHelpAvailable(false);
		dlg.open();
		return null;
	}
}
