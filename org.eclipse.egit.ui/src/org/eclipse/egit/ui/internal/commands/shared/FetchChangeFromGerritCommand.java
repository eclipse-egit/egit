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
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.NonBlockingWizardDialog;
import org.eclipse.egit.ui.internal.fetch.FetchGerritChangeWizard;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Fetch a change from Gerrit
 */
public class FetchChangeFromGerritCommand extends AbstractSharedCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		if (repository == null) {
			Shell shell = getShell(event);
			RepositoryCache repositoryCache = Activator.getDefault()
					.getRepositoryCache();
			Repository[] repositories = repositoryCache.getAllRepositories();
			// This should only show gerrit configured repositories.
			// But there is no way to retrieve all gerrit configured
			// repositories (AFAICS)

			ElementListSelectionDialog repositorySelectionDialog = new ElementListSelectionDialog(
					shell, new NoRepositorySelectedLabelProvider());
			repositorySelectionDialog.setElements(repositories);

			repositorySelectionDialog.setTitle(
					UIText.FetchChangeFromGerritCommand_noRepositorySelectedTitle);

			if (repositorySelectionDialog.open() != Window.OK) {
				return null;
			} else {
				repository = (Repository) repositorySelectionDialog
						.getResult()[0]; // Not the best solution I suppose. Any
											// thoughts on this?
			}
		}

		FetchGerritChangeWizard wiz = new FetchGerritChangeWizard(repository);
		NonBlockingWizardDialog dlg = new NonBlockingWizardDialog(
				HandlerUtil.getActiveShellChecked(event), wiz);
		dlg.setHelpAvailable(false);
		dlg.open();
		return null;
	}

	class NoRepositorySelectedLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Repository) {

				Repository repository = (Repository) element;
				// I couldn't figure out how to get the correct name of a
				// repository.
				// Like "egit" or "jgit".
				return repository.toString();
			}
			return super.getText(element);
		}
	}
}
