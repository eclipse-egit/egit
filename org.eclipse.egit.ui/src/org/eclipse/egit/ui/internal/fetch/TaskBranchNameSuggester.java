/*******************************************************************************
 * Copyright (c) 2004, 2013 Tasktop Technologies.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sam Davis (Tasktop) - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.fetch;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.mylyn.tasks.core.ITask;

/**
 * @author Sam Davis
 */
public class TaskBranchNameSuggester {
	private static final String SEPARATOR = "_"; //$NON-NLS-1$

	private static final int BRANCH_MAX_NAME_LENGTH = 40;

	private static Set<String> wordsToIgnore = new HashSet<String>();

	static {
		wordsToIgnore.add("a"); //$NON-NLS-1$
		wordsToIgnore.add("an"); //$NON-NLS-1$
		wordsToIgnore.add("the"); //$NON-NLS-1$
		wordsToIgnore.add("this"); //$NON-NLS-1$
		wordsToIgnore.add("that"); //$NON-NLS-1$
		wordsToIgnore.add("so"); //$NON-NLS-1$
		wordsToIgnore.add("are"); //$NON-NLS-1$
		wordsToIgnore.add("and"); //$NON-NLS-1$
		wordsToIgnore.add("them"); //$NON-NLS-1$
		wordsToIgnore.add("with"); //$NON-NLS-1$
		wordsToIgnore.add("when"); //$NON-NLS-1$
		wordsToIgnore.add("is"); //$NON-NLS-1$
		wordsToIgnore.add("supporting"); //$NON-NLS-1$
		wordsToIgnore.add("consider"); //$NON-NLS-1$
	}

	/**
	 * @param task
	 * @param prefix
	 * @return branch name
	 */
	public static String suggestBranchName(ITask task, String prefix) {
		StringBuilder name = new StringBuilder(prefix);
		name.append(SEPARATOR);
		name.append(task.getTaskId());
		name.append(SEPARATOR);
		String[] summary = task.getSummary().replaceAll("\\[.*\\]", "") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\"", "") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\'", "") //$NON-NLS-1$ //$NON-NLS-2$
				.split("\\s"); //$NON-NLS-1$
		for (String word : summary) {
			if (name.length() >= BRANCH_MAX_NAME_LENGTH) {
				break;
			}
			if (word.length() > 0
					&& !wordsToIgnore.contains(word.toLowerCase())) {
				name.append(word);
				name.append(SEPARATOR);
			}
		}
		return name.substring(0, name.length() - 1).replace(":", SEPARATOR); //$NON-NLS-1$
	}

}
