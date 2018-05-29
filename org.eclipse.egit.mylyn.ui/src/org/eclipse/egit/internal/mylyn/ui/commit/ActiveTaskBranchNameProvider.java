/*******************************************************************************
 * Copyright (c) 2011, 2014 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Manuel Doninger <manuel.doninger@googlemail.com>
 *     Benjamin Muskalla <benjamin.muskalla@tasktop.com>
 *     Thorsten Kamann <thorsten@kamann.info>
 *     Steffen Pingel <steffen.pingel@tasktop.com>
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import org.eclipse.egit.ui.IBranchNameProvider;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * A BranchNameProvider using description and title of the currently active task
 * to suggest a branch name.
 */
public class ActiveTaskBranchNameProvider implements IBranchNameProvider {

	/**
	 * @return the currently activated task or <code>null</code> if no task is
	 *         activated
	 */
	protected ITask getCurrentTask() {
		return TasksUi.getTaskActivityManager().getActiveTask();
	}

	@Override
	public String getBranchNameSuggestion() {
		ITask task = getCurrentTask();
		if (task == null)
			return null;

		String taskKey = task.getTaskKey();
		if (taskKey == null)
			taskKey = task.getTaskId();

		StringBuilder sb = new StringBuilder();
		sb.append(TasksUiInternal.getTaskPrefix(task.getConnectorKind()));
		sb.append(taskKey);
		sb.append('-');
		sb.append(task.getSummary());
		return normalizeBranchName(sb.toString());
	}

	private String normalizeBranchName(String name) {
		String normalized = Repository
				.normalizeBranchName(name.replaceAll("[#$!]", " ")); //$NON-NLS-1$ //$NON-NLS-2$
		if (normalized.length() > 30) {
			normalized = Repository
					.normalizeBranchName(normalized.substring(0, 30));
		}
		return normalized;
	}
}

