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
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.repository.NewRepositoryWizard;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
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
		NewRepositoryWizard wizard = new NewRepositoryWizard(false);
		RepositoryGroup group = getSelectedRepositoryGroup(event);
		wizard.setRepositoryGroup(group);
		WizardDialog dlg = new WizardDialog(getShell(event), wizard) {
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
		if (dlg.open() == Window.OK) {
			expandRepositoryGroup(event, group);
		}
		return null;
	}
}
