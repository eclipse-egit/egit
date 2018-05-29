/*******************************************************************************
 * Copyright (C) 2012, Kevin Sawicki <kevin@github.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

/**
 * Provides access to read and add new saved commit messages.
 */
public class CommitMessageHistory {

	private static final String KEY_MESSAGE = "message"; //$NON-NLS-1$

	private static final String KEY_MESSAGES = "messages"; //$NON-NLS-1$

	/**
	 * @return saved commit messages
	 */
	public static Set<String> getCommitHistory() {
		String all = getPreferenceStore().getString(
				UIPreferences.COMMIT_DIALOG_HISTORY_MESSAGES);
		if (all.length() == 0)
			return Collections.emptySet();
		int max = getCommitHistorySize();
		if (max < 1)
			return Collections.emptySet();
		XMLMemento memento;
		try {
			memento = XMLMemento.createReadRoot(new StringReader(all));
		} catch (WorkbenchException e) {
			org.eclipse.egit.ui.Activator.logError(
					"Error reading commit message history", e); //$NON-NLS-1$
			return Collections.emptySet();
		}
		Set<String> messages = new LinkedHashSet<>();
		for (IMemento child : memento.getChildren(KEY_MESSAGE)) {
			messages.add(child.getTextData());
			if (messages.size() == max)
				break;
		}
		return messages;
	}

	/**
	 * Save a new commit message.
	 *
	 * @param message
	 */
	public static void saveCommitHistory(String message) {
		if (message == null || message.length() == 0)
			return;
		int size = getCommitHistorySize();
		if (size < 1)
			return;

		XMLMemento memento = XMLMemento.createWriteRoot(KEY_MESSAGES);
		memento.createChild(KEY_MESSAGE).putTextData(message);

		int count = 1;
		if (count < size) {
			Set<String> history = getCommitHistory();
			history.remove(message);
			for (String previous : history) {
				memento.createChild(KEY_MESSAGE).putTextData(previous);
				count++;
				if (count == size)
					break;
			}
		}
		StringWriter writer = new StringWriter();
		try {
			memento.save(writer);
			getPreferenceStore().setValue(
					UIPreferences.COMMIT_DIALOG_HISTORY_MESSAGES,
					writer.toString());
		} catch (IOException e) {
			org.eclipse.egit.ui.Activator.logError(
					"Error writing commit message history", e); //$NON-NLS-1$
		}
	}

	private static IPreferenceStore getPreferenceStore() {
		return org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore();
	}

	private static int getCommitHistorySize() {
		return getPreferenceStore().getInt(
				UIPreferences.COMMIT_DIALOG_HISTORY_SIZE);
	}
}
