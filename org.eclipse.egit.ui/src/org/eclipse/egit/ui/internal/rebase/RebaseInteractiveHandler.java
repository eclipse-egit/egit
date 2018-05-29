/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.List;

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

	@Override
	public String modifyCommitMessage(final String commitMessage) {
		final String[] result = new String[1];
		result[0] = commitMessage;
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				CommitMessageEditorDialog dialog = new CommitMessageEditorDialog(
						PlatformUI.getWorkbench()
						.getDisplay().getActiveShell(),
						commitMessage);
				if (dialog.open() == Window.OK) {
					result[0] = dialog.getCommitMessage();
				}
			}
		});
		return result[0];
	}

	@Override
	public void prepareSteps(List<RebaseTodoLine> steps) {
		// do not change list of steps here. Instead change the list via
		// writeRebaseTodoFile of class Repository
	}
}
