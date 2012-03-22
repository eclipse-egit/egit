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

import org.eclipse.egit.ui.internal.dialogs.CheckoutConflictDialog;
import org.eclipse.egit.ui.internal.dialogs.NonDeletedFilesDialog;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Display the result of a Branch operation.
 */
public class BranchResultDialog {

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
					new CheckoutConflictDialog(shell, repository, result.getConflictList())
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
		}
	}
}
