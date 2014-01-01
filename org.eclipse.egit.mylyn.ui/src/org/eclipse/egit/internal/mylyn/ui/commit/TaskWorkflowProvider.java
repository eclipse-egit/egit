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
 *     Steffen Pingel <steffen.pingel@tasktop.com>
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.IBranchNameProvider;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.team.ui.TeamUiUtil;

/**
 * Gets the active task and combines the description and title with the commit
 * message template defined in the preferences
 */
@SuppressWarnings("restriction")
public class TaskWorkflowProvider implements IBranchNameProvider {

	/**
	 * @return the mylyn commit message template defined in the preferences
	 */
	public String getMessage(IResource[] resources) {
		String message = ""; //$NON-NLS-1$
		if (resources == null)
			return message;
		ITask task = getCurrentTask();
		if (task == null)
			return message;
		boolean checkTaskRepository = true;
		message = TeamUiUtil.getComment(checkTaskRepository, task,
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

	public String getBranchNameSuggestion() {
		ITask task = getCurrentTask();
		if (task == null)
			return null;

		String taskKey = task.getTaskKey();
		if (taskKey == null)
			taskKey = task.getTaskId();

		StringBuilder sb = new StringBuilder();
		sb.append(TasksUiInternal.getTaskPrefix(task.getConnectorKind()));
		sb.append(task.getTaskKey());
		sb.append("-"); //$NON-NLS-1$
		sb.append(task.getSummary());
		return normalizeBranchName(sb.toString());
	}

	private String normalizeBranchName(String name) {
		String normalized = name.replaceAll("\\s+", "_").replaceAll("[^\\w-]", ""); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		if (normalized.length() > 30)
			normalized = normalized.substring(0, 30);
		return normalized;
	}

}

