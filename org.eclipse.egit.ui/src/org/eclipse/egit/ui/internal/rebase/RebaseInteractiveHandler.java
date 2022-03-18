/*******************************************************************************
 * Copyright (c) 2013, 2022 SAP AG and others.
 *
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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;

/**
 * Default implementation of an {@link InteractiveHandler2}.
 */
public class RebaseInteractiveHandler implements InteractiveHandler2 {

	private final Repository repository;

	/**
	 * Creates a new {@link RebaseInteractiveHandler}.
	 *
	 * @param repository
	 *            to work in
	 */
	public RebaseInteractiveHandler(Repository repository) {
		this.repository = repository;
	}

	@Override
	public ModifyResult editCommitMessage(String message, CleanupMode mode,
			char commentChar) {
		String[] msg = { message };
		boolean[] doChangeId = { false };
		CleanupMode[] finalMode = { mode };
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			CommitMessageEditorDialog dialog = new CommitMessageEditorDialog(
					PlatformUI.getWorkbench().getModalDialogShellProvider()
							.getShell(),
					repository, message, mode, commentChar);
			if (dialog.open() == Window.OK) {
				msg[0] = dialog.getCommitMessage();
				doChangeId[0] = dialog.isWithChangeId();
				finalMode[0] = CleanupMode.VERBATIM;
			}
		});
		String edited = msg[0];
		return new ModifyResult() {

			@Override
			public String getMessage() {
				return edited == null ? "" : edited; //$NON-NLS-1$
			}

			@Override
			public CleanupMode getCleanupMode() {
				CleanupMode result = finalMode[0];
				assert result != null;
				return result;
			}

			@Override
			public boolean shouldAddChangeId() {
				return doChangeId[0];
			}
		};
	}

	@Override
	public void prepareSteps(List<RebaseTodoLine> steps) {
		// Do not change list of steps here. Instead change the list via
		// writeRebaseTodoFile of class Repository
	}
}
