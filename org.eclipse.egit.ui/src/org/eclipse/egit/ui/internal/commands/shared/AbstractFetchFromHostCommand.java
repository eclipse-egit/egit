/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitSelectRepositoryPage;
import org.eclipse.egit.ui.internal.dialogs.MinimumSizeWizardDialog;
import org.eclipse.egit.ui.internal.dialogs.NonBlockingWizardDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Base class for fetching a change from an upstream server.
 */
public abstract class AbstractFetchFromHostCommand
		extends AbstractSharedCommandHandler {

	@Override
	public final Object execute(ExecutionEvent event)
			throws ExecutionException {
		Repository repository = getRepository(event);

		Clipboard clipboard = new Clipboard(
				PlatformUI.getWorkbench().getDisplay());
		String clipText;
		try {
			clipText = (String) clipboard
					.getContents(TextTransfer.getInstance());
		} finally {
			clipboard.dispose();
		}

		if (repository == null) {
			Shell shell = getShell(event);
			repository = askForRepository(shell, createSelectionPage());
			if (repository == null) {
				return null;
			}
		}

		Wizard wiz = createFetchWizard(repository, clipText);
		NonBlockingWizardDialog dlg = new NonBlockingWizardDialog(
				HandlerUtil.getActiveShellChecked(event), wiz);
		dlg.setHelpAvailable(false);
		dlg.open();
		return null;
	}

	/**
	 * Creates a {@link GitSelectRepositoryPage} where the user can select a
	 * repository.
	 *
	 * @return the {@link GitSelectRepositoryPage}
	 */
	protected abstract GitSelectRepositoryPage createSelectionPage();

	/**
	 * Creates the fetch wizard.
	 *
	 * @param repository
	 *            to fetch from
	 * @param clipText
	 *            content of the clipboard
	 * @return the {@link Wizard}
	 */
	protected abstract Wizard createFetchWizard(@NonNull Repository repository,
			String clipText);

	/**
	 * Opens a dialog for the user to select a {@link Repository}, and returns
	 * the selected repository, if any.
	 *
	 * @param shell
	 *            {@link Shell} to use as parent for the dialog
	 * @param page
	 *            to show for selecting a {@link Repository}
	 * @return the selected {@link Repository}, or {@code null} if none
	 */
	private Repository askForRepository(Shell shell,
			GitSelectRepositoryPage page) {
		Repository[] result = { null };
		Wizard wizard = new Wizard() {

			@Override
			public boolean performFinish() {
				result[0] = page.getRepository();
				return true;
			}
		};
		wizard.addPage(page);
		wizard.setWindowTitle(page.getTitle());
		WizardDialog wizardDialog = new MinimumSizeWizardDialog(shell,
				wizard) {

			@Override
			protected Button createButton(Composite parent, int id,
					String label, boolean defaultButton) {
				String text = label;
				if (id == IDialogConstants.FINISH_ID) {
					text = UIText.GerritSelectRepositoryPage_FinishButtonLabel;
				}
				return super.createButton(parent, id, text, defaultButton);
			}
		};
		wizardDialog.setHelpAvailable(false);
		if (wizardDialog.open() != Window.OK) {
			return null;
		}
		return result[0];
	}
}
