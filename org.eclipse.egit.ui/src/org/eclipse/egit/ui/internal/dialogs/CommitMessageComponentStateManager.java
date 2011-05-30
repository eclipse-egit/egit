/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * This class is used to load / save the state of a
 * {@link CommitMessageComponentState} in the dialog settings.
 *
 */
public class CommitMessageComponentStateManager {

	private static final String COMMIT_MESSAGE_COMPONENT_SECTION = "GitCommitMessageComponent"; //$NON-NLS-1$

	private static final String EMPTY = "empty"; //$NON-NLS-1$

	private static final int MEMBER_COUNT = 5; // number of members in
												// CommitMessageComponentState

	/**
	 * @param repository
	 * @param state
	 */
	public static void persistState(Repository repository,
			CommitMessageComponentState state) {
		IDialogSettings dialogSettings = getDialogSettings();
		String[] values = new String[] { Boolean.toString(state.getAmend()),
				state.getAuthor(), state.getCommitMessage(),
				state.getCommitter(), state.getHeadCommit().getName().toString() };
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
		if (values == null || values.length < MEMBER_COUNT)
			return null;
		CommitMessageComponentState state = new CommitMessageComponentState();
		state.setAmend(Boolean.parseBoolean(values[0]));
		state.setAuthor(values[1]);
		state.setCommitMessage(values[2]);
		state.setCommitter(values[3]);
		state.setHeadCommit(ObjectId.fromString(values[4]));
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
