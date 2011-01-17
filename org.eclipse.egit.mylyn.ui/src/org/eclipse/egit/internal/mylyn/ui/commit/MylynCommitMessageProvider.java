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
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.mylyn.internal.team.ui.FocusedTeamUiPlugin;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskActivityManager;
import org.eclipse.mylyn.tasks.ui.TasksUi;


/**
 * Gets the active task and combines the description and title with
 * the commit message template defined in the preferences
 */
@SuppressWarnings("restriction")
public class MylynCommitMessageProvider implements ICommitMessageProvider {

	/**
	 * @return the mylyn commit message template defined in the preferences
	 */
	public String getMessage(IResource[] resources) {
		String message = "";
		ITaskActivityManager tam = TasksUi.getTaskActivityManager();
		ITask task = tam.getActiveTask();
		if(task == null)
			return message;
		// generate the comment based on the active mylyn task
		String template = FocusedTeamUiPlugin.getDefault().getPreferenceStore().getString(FocusedTeamUiPlugin.COMMIT_TEMPLATE);
		FocusedTeamUiPlugin.getDefault().getCommitTemplateManager().generateComment(task, template);
		message = FocusedTeamUiPlugin.getDefault().getCommitTemplateManager().generateComment(task, template);
		return message;
	}

}
