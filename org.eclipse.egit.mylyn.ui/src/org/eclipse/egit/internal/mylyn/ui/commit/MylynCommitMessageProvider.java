/*******************************************************************************
 * Copyright (c) 2011 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Manuel Doninger <manuel@doninger.net>
 *     Benjamin Muskalla <benjamin.muskalla@tasktop.com>
 *     Thorsten Kamann <thorsten@kamann.info>
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.team.ui.TeamUiUtil;


/**
 * Gets the active task and combines the description and title with
 * the commit message template defined in the preferences
 */
public class MylynCommitMessageProvider implements ICommitMessageProvider {

	/**
	 * @return the mylyn commit message template defined in the preferences
	 */
	@Override
	public String getMessage(IResource[] resources) {
		String message = ""; //$NON-NLS-1$
		if (resources == null)
			return message;
		ITask task = getCurrentTask();
		if (task == null)
			return message;
		boolean checkTaskRepository = true;
		message = TeamUiUtil.getComment(checkTaskRepository, task, resources);
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
}
