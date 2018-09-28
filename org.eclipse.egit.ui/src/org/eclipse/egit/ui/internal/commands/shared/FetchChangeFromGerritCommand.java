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
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.dialogs.NonBlockingWizardDialog;
import org.eclipse.egit.ui.internal.fetch.FetchGerritChangeWizard;
import org.eclipse.egit.ui.internal.gerrit.GerritSelectRepositoryPage;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Fetch a change from Gerrit
 */
public class FetchChangeFromGerritCommand extends AbstractSharedCommandHandler {

	private Repository repository;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		repository = getRepository(event);

		if (repository == null) {
			Shell shell = getShell(event);
			GerritSelectRepositoryPage page = new GerritSelectRepositoryPage();

			Wizard wizard = new Wizard() {

				@Override
				public boolean performFinish() {
					FetchChangeFromGerritCommand.this.repository = page
							.getRepository();
					return true;
				}
			};
			wizard.addPage(page);
			WizardDialog wizardDialog = new WizardDialog(shell, wizard);
			int result = wizardDialog.open();
			if (result != Window.OK) {
				return null;
			}
		}

		FetchGerritChangeWizard wiz = new FetchGerritChangeWizard(repository);
		NonBlockingWizardDialog dlg = new NonBlockingWizardDialog(
				HandlerUtil.getActiveShellChecked(event), wiz);
		dlg.setHelpAvailable(false);
		dlg.open();
		return null;
	}

}
