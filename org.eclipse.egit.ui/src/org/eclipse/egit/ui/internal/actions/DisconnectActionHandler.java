/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.Arrays;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.job.JobUtil;
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
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject[] projects = getProjectsForSelectedResources();
		if (projects.length == 0)
			return null;
		JobUtil.scheduleUserJob(new DisconnectProviderOperation(Arrays
				.asList(projects)), UIText.Disconnect_disconnect,
				JobFamilies.DISCONNECT, new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent actEvent) {
						GitLightweightDecorator.refresh();
					}
				});
		return null;
	}
}
