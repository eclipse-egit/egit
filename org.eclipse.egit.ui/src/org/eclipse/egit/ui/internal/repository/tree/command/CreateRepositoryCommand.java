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
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.NewRepositoryWizard;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Implements "Create Repository"
 */
public class CreateRepositoryCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		WizardDialog dlg = new WizardDialog(getShell(event),
				new NewRepositoryWizard(false)) {
			@Override
			protected Button createButton(Composite parent, int id,
					String label, boolean defaultButton) {
				if (id == IDialogConstants.FINISH_ID) {
					return super.createButton(parent, id,
							UIText.CreateRepositoryCommand_CreateButtonLabel,
							defaultButton);
				}
				return super.createButton(parent, id, label, defaultButton);
			}
		};
		dlg.setHelpAvailable(false);
		dlg.open();
		return null;
	}
}
