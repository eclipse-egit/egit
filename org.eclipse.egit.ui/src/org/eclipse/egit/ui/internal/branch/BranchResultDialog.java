/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
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
import java.util.List;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.NonDeletedFilesDialog;
import org.eclipse.egit.ui.internal.dialogs.NonDeletedFilesTree;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.api.CheckoutResult;
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

	/**
	 * @param result
	 *            the result to show
	 * @param repository
	 * @param target
	 *            the target (branch name or commit id)
	 */
	public static void show(final CheckoutResult result,
			final Repository repository, final String target) {
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
			for (String path : pathList) {
				if (new File(repository.getWorkTree(), path).exists()) {
					show = true;
					break;
				}
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
						target), MessageDialog.INFORMATION,
				new String[] { IDialogConstants.OK_LABEL }, 0);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repository = repository;
		this.result = result;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true).applyTo(
				main);
		new NonDeletedFilesTree(main, repository, this.result.getConflictList());
		applyDialogFont(main);

		return main;
	}
}