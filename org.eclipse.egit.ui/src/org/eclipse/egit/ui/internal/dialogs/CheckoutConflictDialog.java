/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Duft - Refactoring from RebaseResultDialog
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.List;

import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.ActionCommands;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.command.ResetCommand;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Display a checkout conflict
 */
public class CheckoutConflictDialog extends MessageDialog {
	private static final Image INFO = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);
	private List<String> conflicts;
	private Repository repository;

	/**
	 * @param shell
	 * @param repository
	 * @param conflicts
	 */
	public CheckoutConflictDialog(Shell shell, Repository repository, List<String> conflicts) {
		super(shell, UIText.BranchResultDialog_CheckoutConflictsTitle, INFO,
				UIText.CheckoutConflictDialog_conflictMessage,
				MessageDialog.INFORMATION,
				new String[] { IDialogConstants.OK_LABEL }, 0);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repository = repository;
		this.conflicts = conflicts;
	}

	@SuppressWarnings("unused")
	@Override
	protected Control createCustomArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true)
				.applyTo(main);
		new NonDeletedFilesTree(main, repository, this.conflicts);
		applyDialogFont(main);

		return main;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case IDialogConstants.PROCEED_ID:
			CommonUtils.runCommand(ActionCommands.COMMIT_ACTION,
					new StructuredSelection(repository));
			break;
		case IDialogConstants.ABORT_ID:
			CommonUtils.runCommand(ResetCommand.ID, new StructuredSelection(
					new RepositoryNode(null, repository)));
			break;
		case IDialogConstants.SKIP_ID:
			CommonUtils.runCommand(ActionCommands.STASH_CREATE,
					new StructuredSelection(
							new RepositoryNode(null, repository)));
			break;
		default:
			break;
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		createButton(parent, IDialogConstants.ABORT_ID,
				UIText.BranchResultDialog_buttonReset, false);
		createButton(parent, IDialogConstants.PROCEED_ID,
				UIText.BranchResultDialog_buttonCommit, false);
		createButton(parent, IDialogConstants.SKIP_ID,
				UIText.BranchResultDialog_buttonStash, false);
	}
}
