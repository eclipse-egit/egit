/*******************************************************************************
 * Copyright (C) 2007, 2014 Shawn O. Pearce <spearce@spearce.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.DisconnectProviderOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;

/**
 * Action to disassociate a project from its Git repository.
 *
 * @see DisconnectProviderOperation
 */
public class DisconnectActionHandler extends RepositoryActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject[] selectedProjects = getProjectsForSelectedResources();
		List<IProject> projects = new ArrayList<>(selectedProjects.length);
		for (IProject project : selectedProjects) {
			if (project.isOpen() && ResourceUtil.isSharedWithGit(project)) {
				projects.add(project);
			}
		}
		if (projects.isEmpty()) {
			return null;
		}
		JobUtil.scheduleUserJob(new DisconnectProviderOperation(projects),
				UIText.Disconnect_disconnect,
				JobFamilies.DISCONNECT, new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent actEvent) {
						GitLightweightDecorator.refresh();
					}
				});
		return null;
	}
}
