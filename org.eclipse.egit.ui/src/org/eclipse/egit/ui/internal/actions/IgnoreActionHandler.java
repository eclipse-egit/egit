/*******************************************************************************
 * Copyright (C) 2009, Alex Blewitt <alex.blewitt@gmail.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.IgnoreOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.team.core.Team;

/** Action for ignoring files via .gitignore. */
public class IgnoreActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IResource[] resources = getSelectedResources(event);
		if (resources.length == 0)
			return null;
		final IgnoreOperation operation = new IgnoreOperation(resources);
		String jobname = UIText.IgnoreActionHandler_addToGitignore;
		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					operation.execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getStatus()
							.getMessage(), e);
				}
				if (operation.isGitignoreOutsideWSChanged())
					GitLightweightDecorator.refresh();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setRule(operation.getSchedulingRule());
		job.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		if (getProjectsInRepositoryOfSelectedResources().length == 0)
			return false;

		IResource[] resources = getSelectedResources();
		for (IResource resource : resources) {
			// NB This does the same thing in DecoratableResourceAdapter,
			// but
			// neither currently consult .gitignore
			if (!Team.isIgnoredHint(resource))
				return true;
		}
		return false;
	}
}
