/*******************************************************************************
 * Copyright (c) 2010-2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitUI;
import org.eclipse.egit.ui.internal.dialogs.NonDeletedFilesDialog;
import org.eclipse.egit.ui.internal.dialogs.NonDeletedFilesTree;
import org.eclipse.egit.ui.internal.stash.StashCreateUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Display the result of a Branch operation.
 */
public class BranchResultDialog extends MessageDialog {
	private static final Image INFO = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);

	private final CheckoutResult result;

	private final Repository repository;

	private static String target;

	/**
	 * @param result
	 *            the result to show
	 * @param repository
	 * @param target
	 *            the target (branch name or commit id)
	 */
	public static void show(final CheckoutResult result,
			final Repository repository, final String target) {
		BranchResultDialog.target = target;
		if (result.getStatus() == CheckoutResult.Status.CONFLICTS)
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					Shell shell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					new BranchResultDialog(shell, repository, result, target)
							.open();
				}
			});
		else if (result.getStatus() == CheckoutResult.Status.NONDELETED) {
			// double-check if the files are still there
			boolean show = false;
			List<String> pathList = result.getUndeletedList();
			for (String path : pathList)
				if (new File(repository.getWorkTree(), path).exists()) {
					show = true;
					break;
				}
			if (!show)
				return;
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					Shell shell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					new NonDeletedFilesDialog(shell, repository, result
							.getUndeletedList()).open();
				}
			});
		} else if (result.getStatus() == CheckoutResult.Status.OK) {
			try {
				if (ObjectId.isId(repository.getFullBranch()))
					showDetachedHeadWarning();
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
	}

	/**
	 * @param shell
	 * @param repository
	 * @param result
	 * @param target
	 */
	private BranchResultDialog(Shell shell, Repository repository,
			CheckoutResult result, String target) {
		super(shell, UIText.BranchResultDialog_CheckoutConflictsTitle, INFO,
				NLS.bind(UIText.BranchResultDialog_CheckoutConflictsMessage,
						Repository.shortenRefName(target)),
				MessageDialog.INFORMATION,
				new String[] { }, -1);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repository = repository;
		this.result = result;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true)
				.applyTo(main);
		new NonDeletedFilesTree(main, repository, this.result.getConflictList());
		applyDialogFont(main);

		return main;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		boolean shouldCheckout = false;
		switch (buttonId) {
		case IDialogConstants.PROCEED_ID:
			CommitUI commitUI = new CommitUI(getShell(), repository, new IResource[0], true);
			shouldCheckout = commitUI.commit();
			break;
		case IDialogConstants.ABORT_ID:
			final ResetOperation operation = new ResetOperation(repository,
					Constants.HEAD, ResetType.HARD);
			String jobname = NLS.bind(UIText.ResetAction_reset, Constants.HEAD);
			JobUtil.scheduleUserJob(operation, jobname, JobFamilies.RESET);
			shouldCheckout = true;
			break;
		case IDialogConstants.SKIP_ID:
			StashCreateUI stashCreateUI = new StashCreateUI(getShell(), repository);
			shouldCheckout = stashCreateUI.createStash();
			break;
		case IDialogConstants.CANCEL_ID:
			super.buttonPressed(buttonId);
			return;
		}
		if (shouldCheckout) {
			super.buttonPressed(buttonId);
			BranchOperationUI op = BranchOperationUI.checkout(repository, target);
			op.start();
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		createButton(parent, IDialogConstants.PROCEED_ID,
				UIText.BranchResultDialog_buttonCommit, false);
		createButton(parent, IDialogConstants.SKIP_ID,
				UIText.BranchResultDialog_buttonStash, false);
		createButton(parent, IDialogConstants.ABORT_ID,
				UIText.BranchResultDialog_buttonReset, false);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, true);
	}

	private static void showDetachedHeadWarning() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IPreferenceStore store = Activator.getDefault()
						.getPreferenceStore();

				if (store.getBoolean(UIPreferences.SHOW_DETACHED_HEAD_WARNING)) {
					String toggleMessage = UIText.BranchResultDialog_dontShowAgain;
					MessageDialogWithToggle.openInformation(PlatformUI
							.getWorkbench().getActiveWorkbenchWindow()
							.getShell(),
							UIText.BranchOperationUI_DetachedHeadTitle,
							UIText.BranchOperationUI_DetachedHeadMessage,
							toggleMessage, false, store,
							UIPreferences.SHOW_DETACHED_HEAD_WARNING);
				}
			}
		});
	}
}
