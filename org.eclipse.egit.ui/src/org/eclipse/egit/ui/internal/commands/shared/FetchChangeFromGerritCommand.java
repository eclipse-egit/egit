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

import java.io.File;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.NonBlockingWizardDialog;
import org.eclipse.egit.ui.internal.fetch.FetchGerritChangeWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Fetch a change from Gerrit
 */
public class FetchChangeFromGerritCommand extends AbstractSharedCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		RepositoryCache repositoryCache = Activator.getDefault()
				.getRepositoryCache();

		if (repository == null) {
			Repository[] repositories = repositoryCache.getAllRepositories();
			if (repositories.length == 0) {
				Shell shell = getShell(event);
				MessageDialog.openInformation(shell,
						UIText.FetchChangeFromGerritCommand_noRepositorySelectedTitle,
						UIText.FetchChangeFromGerritCommand_noRepositorySelectedMessage);
				return null;
			} else {
				File activeFile = getActiveFile();
				if (activeFile != null) {
					repository = repositoryCache.getRepository(activeFile);
				} else {
					repository = repositories[0];
				}
			}
		}

		FetchGerritChangeWizard wiz = new FetchGerritChangeWizard();
		wiz.setRepository(repository);
		NonBlockingWizardDialog dlg = new NonBlockingWizardDialog(
				HandlerUtil.getActiveShellChecked(event), wiz);
		dlg.setHelpAvailable(false);
		dlg.open();
		return null;
	}

	File getActiveFile() {
		IWorkbenchPage workbenchPage = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getService(IWorkbenchPage.class);
		IEditorPart activeEditor = workbenchPage.getActiveEditor();
		if (activeEditor == null) {
			return null;
		}

		IEditorInput editorInput = activeEditor.getEditorInput();
		return Adapters.adapt(editorInput, File.class);
	}
}
