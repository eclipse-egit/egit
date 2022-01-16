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
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler2;
import org.eclipse.jgit.lib.CommitConfig.CleanupMode;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.ui.PlatformUI;

/**
 * Singleton {@link InteractiveHandler2}.
 */
public enum RebaseInteractiveHandler implements InteractiveHandler2 {

	/**
	 * Commonly used {@link InteractiveHandler2} for (interactive) rebase.
	 */
	INSTANCE;

	@Override
	public ModifyResult editCommitMessage(String message, CleanupMode mode,
			char commentChar) {
		String[] result = { message };
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			CommitMessageEditorDialog dialog = new CommitMessageEditorDialog(
					PlatformUI.getWorkbench().getModalDialogShellProvider()
							.getShell(),
					message, mode, commentChar);
			if (dialog.open() == Window.OK) {
				result[0] = dialog.getCommitMessage();
			}
		});
		String msg = result[0];
		return new ModifyResult() {

			@Override
			public String getMessage() {
				return msg == null ? "" : msg; //$NON-NLS-1$
			}

			@Override
			public CleanupMode getCleanupMode() {
				return CleanupMode.VERBATIM;
			}
		};
	}

	@Override
	public void prepareSteps(List<RebaseTodoLine> steps) {
		// Do not change list of steps here. Instead change the list via
		// writeRebaseTodoFile of class Repository
	}
}
