/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.ResetActionHandler;
import org.eclipse.egit.ui.internal.actions.ResetMenu;
import org.eclipse.egit.ui.internal.repository.SelectResetTypePage;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Ref;

/**
 * "Resets" a repository
 */
public class ResetCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode<?>> {

	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.team.Reset"; //$NON-NLS-1$

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		final RepositoryTreeNode<?> node = getSelectedNodes(event).get(0);
		final String currentBranch;
		try {
			currentBranch = node.getRepository().getFullBranch();
		} catch (IOException e1) {
			throw new ExecutionException(e1.getMessage(), e1);
		}
		if (!(node.getObject() instanceof Ref)) {
			// Use same dialog as for project when a repository is selected
			// allowing reset to any commit
			return new ResetActionHandler().execute(event);
		}

		// If a ref is selected in the repository view, only reset to
		// that ref will be possible.
		final Ref targetBranch = (Ref) node.getObject();

		final String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(node.getRepository());

		Wizard wiz = new Wizard() {

			@Override
			public void addPages() {
				addPage(new SelectResetTypePage(repoName, node.getRepository(),
						currentBranch, targetBranch.getName()));
				setWindowTitle(UIText.ResetCommand_WizardTitle);
			}

			@Override
			public boolean performFinish() {
				final ResetType resetType = ((SelectResetTypePage) getPages()[0])
						.getResetType();
				ResetMenu.performReset(getShell(), node.getRepository(),
						targetBranch.getObjectId(), resetType);
				return true;
			}
		};
		WizardDialog dlg = new WizardDialog(getShell(event), wiz);
		dlg.setHelpAvailable(false);
		dlg.open();

		return null;
	}
}
