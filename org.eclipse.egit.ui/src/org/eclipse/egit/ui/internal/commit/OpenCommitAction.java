/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.actions.ActionDelegate;

/**
 * Open commit action
 */
public class OpenCommitAction extends ActionDelegate implements
		IWorkbenchWindowActionDelegate {

	private Shell shell;

	@Override
	public void run(IAction action) {
		CommitSelectionDialog dialog = new CommitSelectionDialog(shell, true);
		if (Window.OK != dialog.open())
			return;
		Object[] results = dialog.getResult();
		if (results == null || results.length == 0)
			return;
		for (Object result : results)
			CommitEditor.openQuiet((RepositoryCommit) result);
	}

	@Override
	public void init(IWorkbenchWindow window) {
		shell = window.getShell();
	}

}
