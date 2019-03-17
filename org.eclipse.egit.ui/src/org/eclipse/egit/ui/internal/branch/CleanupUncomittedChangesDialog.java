/*******************************************************************************
 * Copyright (c) 2010-2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitUI;
import org.eclipse.egit.ui.internal.dialogs.NonDeletedFilesTree;
import org.eclipse.egit.ui.internal.stash.StashCreateUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
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
 * Offer options to cleanup uncommitted changes
 */
public class CleanupUncomittedChangesDialog extends MessageDialog {

	private static final Image INFO = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);

	private final Repository repository;

	private List<String> fileList;

	private boolean shouldContinue = false;

	/**
	 * @param shell
	 * @param dialogTitle
	 * @param dialogMessage
	 * @param repository
	 * @param fileList
	 */
	public CleanupUncomittedChangesDialog(Shell shell, String dialogTitle,
			String dialogMessage, Repository repository, List<String> fileList) {
		super(shell, dialogTitle, INFO, dialogMessage,
				MessageDialog.INFORMATION, new String[] {}, -1);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repository = repository;
		this.fileList = fileList;
	}

	@SuppressWarnings("unused")
	@Override
	protected Control createCustomArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true)
				.applyTo(main);
		new NonDeletedFilesTree(main, repository, fileList);
		applyDialogFont(main);

		return main;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case IDialogConstants.PROCEED_ID:
			CommitUI commitUI = new CommitUI(getShell(), repository,
					new IResource[0], true);
			shouldContinue = commitUI.commit();
			break;
		case IDialogConstants.ABORT_ID:
			DiscardChangesOperation operation = new DiscardChangesOperation(
					repository, fileList);
			JobUtil.scheduleUserWorkspaceJob(operation,
					UIText.DiscardChangesAction_discardChanges,
					JobFamilies.DISCARD_CHANGES);
			shouldContinue = true;
			break;
		case IDialogConstants.SKIP_ID:
			StashCreateUI stashCreateUI = new StashCreateUI(repository);
			shouldContinue = stashCreateUI.createStash(getShell());
			break;
		case IDialogConstants.CANCEL_ID:
		default:
			break;
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		createButton(parent, IDialogConstants.PROCEED_ID,
				UIText.BranchResultDialog_buttonCommit, false);
		createButton(parent, IDialogConstants.SKIP_ID,
				UIText.BranchResultDialog_buttonStash, false);
		createButton(parent, IDialogConstants.ABORT_ID,
				UIText.BranchResultDialog_buttonDiscardChanges, false);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, true);
	}

	/**
	 * @return if the initial operation should continue when dialog is closed
	 */
	public boolean shouldContinue() {
		return shouldContinue;
	}
}
