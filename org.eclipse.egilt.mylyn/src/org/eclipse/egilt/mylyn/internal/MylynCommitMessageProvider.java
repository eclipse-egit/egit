/*******************************************************************************
 * Copyright (C) 2010, Thorsten Kamann <thorsten@kamann.info>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egilt.mylyn.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.internal.team.ui.ContextChangeSet;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * Implementation of the {@link ICommitMessageProvider} for the integartion of
 * Mylyn's commit message templates.
 */
@SuppressWarnings("restriction")
public class MylynCommitMessageProvider implements ICommitMessageProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.egit.ui.ICommitMessageProvider#getMessage(IResource[])
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
		return (comment == null) ? "" : comment;
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
