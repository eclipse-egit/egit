/*******************************************************************************
 * Copyright (c) 2011 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.internal.team.ui.ContextChangeSet;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.TasksUi;

@SuppressWarnings("restriction")
public class CommitDialogExtender implements ICommitMessageProvider {

	/**
	 * @author Markus Alexander Kuppe
	 * @author Manuel Doninger
	 * @author Benjamin Muskalla
	 * @author Thorsten Kamann
	 *
	 *         Gets the active task and combines the description and title with
	 *         the commit message template defined in the preferences
	 */
	public String getMessage(IResource[] resources) {
		if (resources == null) {
			return null;
		}
		ITask task = getCurrentTask();
		if (task == null) {
			return null;
		}
		boolean checkTaskRepository = true;
		String comment = ContextChangeSet.getComment(checkTaskRepository, task,
				resources);
		return comment;
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
