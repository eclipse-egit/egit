/*******************************************************************************
 * Copyright (C) 2011, 2022 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * This class is used to load / save the state of a
 * {@link CommitMessageComponentState} in the dialog settings.
 */
public class CommitMessageComponentStateManager {

	private static final String COMMIT_MESSAGE_COMPONENT_SECTION = "GitCommitMessageComponent"; //$NON-NLS-1$

	private static final String EMPTY = "empty"; //$NON-NLS-1$

	// number of members in CommitMessageComponentState, before caret
	// positioning was introduced
	private static final int MEMBER_COUNT_WITHOUT_CARET_POSITION = 5;

	/**
	 * @param repository
	 * @param state
	 */
	public static void persistState(Repository repository,
			CommitMessageComponentState state) {
		IDialogSettings dialogSettings = getDialogSettings();
		char autoCommentChar = state.getAutoCommentChar();
		String[] values = new String[] {
				Boolean.toString(state.getAmend()),
				state.getAuthor(),
				state.getCommitMessage(),
				state.getCommitter(),
				state.getHeadCommit().getName().toString(),
				String.valueOf(state.getCaretPosition()),
				Boolean.toString(state.getSign()),
				autoCommentChar == '\0' ? "" : String.valueOf(autoCommentChar) //$NON-NLS-1$
		};
		dialogSettings.put(repository.getDirectory().getAbsolutePath(), values);
	}

	/**
	 * @param repository
	 * @return state
	 */
	public static CommitMessageComponentState loadState(Repository repository) {
		IDialogSettings dialogSettings = getDialogSettings();
		String[] values = dialogSettings.getArray(repository.getDirectory()
				.getAbsolutePath());
		if (values == null
				|| values.length < MEMBER_COUNT_WITHOUT_CARET_POSITION) {
			return null;
		}

		CommitMessageComponentState state = new CommitMessageComponentState();
		state.setAmend(Boolean.parseBoolean(values[0]));
		state.setAuthor(values[1]);
		state.setCommitMessage(values[2]);
		state.setCommitter(values[3]);
		state.setHeadCommit(ObjectId.fromString(values[4]));
		if (values.length > 5) {
			state.setCaretPosition(Integer.parseInt(values[5]));
		} else {
			state.setCaretPosition(
					CommitMessageComponentState.CARET_DEFAULT_POSITION);
		}
		if (values.length > 6) {
			state.setSign(Boolean.parseBoolean(values[6]));
		}
		if (values.length > 7 && !values[7].isEmpty()) {
			state.setAutoCommentChar(values[7].charAt(0));
		}
		return state;
	}

	private static IDialogSettings getDialogSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings
				.getSection(COMMIT_MESSAGE_COMPONENT_SECTION);
		if (section == null)
			section = settings.addNewSection(COMMIT_MESSAGE_COMPONENT_SECTION);
		return section;
	}

	/**
	 * @param repository
	 */
	public static void deleteState(Repository repository) {
		IDialogSettings dialogSettings = getDialogSettings();
		String key = repository.getDirectory().getAbsolutePath();
		if (dialogSettings != null && dialogSettings.getArray(key) != null)
			dialogSettings.put(key, new String[] { EMPTY });
	}

}
