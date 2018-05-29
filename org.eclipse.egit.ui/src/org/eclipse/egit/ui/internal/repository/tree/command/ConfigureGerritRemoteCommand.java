/*******************************************************************************
 * Copyright (C) 2012, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.gerrit.ConfigureGerritWizard;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Starts a UI to configure a remote configuration for Gerrit Code Review
 */
public class ConfigureGerritRemoteCommand extends
		RepositoriesViewCommandHandler<RemoteNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RemoteNode node = getSelectedNodes(event).get(0);
		Repository repository = node.getRepository();
		final String remoteName = node.getObject();

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();
		ConfigureGerritWizard configureGerritWizard = new ConfigureGerritWizard(
				repository, remoteName);
		WizardDialog dlg = new WizardDialog(shell, configureGerritWizard);
		dlg.setHelpAvailable(false);
		dlg.open();

		return null;
	}
}
