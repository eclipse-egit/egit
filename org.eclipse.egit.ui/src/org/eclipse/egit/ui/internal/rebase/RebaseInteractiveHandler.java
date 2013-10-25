/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.List;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.ui.PlatformUI;

/**
 * Singleton {@link InteractiveHandler}
 */
public enum RebaseInteractiveHandler implements InteractiveHandler {
	/**
	 * Commonly used {@link InteractiveHandler} for (interactive) rebase
	 */
	INSTANCE;

	public String modifyCommitMessage(final String commitMessage) {
		final String[] result = new String[1];
		result[0] = commitMessage;
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			public void run() {
				InputDialog dialog = new InputDialog(PlatformUI.getWorkbench()
						.getDisplay().getActiveShell(),
						UIText.RebaseInteractiveHandler_EditMessageDialogTitle,
						UIText.RebaseInteractiveHandler_EditMessageDialogText,
						commitMessage, null);
				if (dialog.open() == Window.OK) {
					result[0] = dialog.getValue();
				}
			}
		});
		return result[0];
	}

	public void prepareSteps(List<RebaseTodoLine> steps) {
		// do not change list of steps here. Instead change the list via
		// writeRebaseTodoFile of class Repository
	}
}