/*******************************************************************************
 * Copyright (c) 2011 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Manuel Doninger <manuel@doninger.net>
 *     Benjamin Muskalla <benjamin.muskalla@tasktop.com>
 *     Thorsten Kamann <thorsten@kamann.info>
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.IWorkflowProvider;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.internal.team.ui.ContextChangeSet;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * Gets the active task and combines the description and title with the commit
 * message template defined in the preferences
 */
@SuppressWarnings("restriction")
public class MylynWorkflowProvider implements IWorkflowProvider {

	/**
	 * @return the mylyn commit message template defined in the preferences
	 */
	public String getCommitMessage(IResource[] resources) {
		String message = ""; //$NON-NLS-1$
		if (resources == null)
			return message;
		ITask task = getCurrentTask();
		if (task == null)
			return message;
		boolean checkTaskRepository = true;
		message = ContextChangeSet.getComment(checkTaskRepository, task,
				resources);

		return message;
	}

	/**
	 * @return the currently activated task or <code>null</code> if no task is
	 *         activated
	 */
	protected ITask getCurrentTask() {
		return TasksUi.getTaskActivityManager().getActiveTask();
	}

	/**
	 * @return the activecontext or <code>null</code> if no activecontext exists
	 */
	protected IInteractionContext getActiveContext() {
		return ContextCore.getContextManager().getActiveContext();
	}

	public String getBranchNameSuggestion() {

		if (getCurrentTask() == null)
			return null;

		String taskKey = getCurrentTask().getTaskKey();
		if (taskKey == null)
			taskKey = getCurrentTask().getTaskId();
		String summary = normalizeSummary(getCurrentTask().getSummary());

		return taskKey + "_" + summary; //$NON-NLS-1$
	}

	private String normalizeSummary(String summary) {
		String normalized;

		normalized = summary
				.replace(" ", "_").replaceAll("\\W", ""); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		if (normalized.length() > 30)
			normalized = normalized.substring(0, 30);

		return normalized;
	}

	public String getDefaultSourceReference() {
		// TODO Auto-generated method stub
		return null;
	}
}
