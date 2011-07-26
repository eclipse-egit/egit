/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.RemoveFromIndexOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.lib.Repository;

/**
 * Action for removing resource from the git index
 *
 * @see AddToIndexOperation
 */
public class RemoveFromIndexActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IResource[] sel = getSelectedResources(event);
		if (sel.length == 0)
			return null;

		Repository repo = getRepository();
		List<String> paths = new ArrayList<String>();
		RepositoryMapping mapping = RepositoryMapping.findRepositoryMapping(repo);
		for (IResource res : sel)
			paths.add(mapping.getRepoRelativePath(res));

		final RemoveFromIndexOperation removeOperation = new RemoveFromIndexOperation(
				repo, paths);
		Job job = new Job(UIText.RemoveFromIndexAction_removingFiles) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					removeOperation.execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getStatus()
							.getMessage(), e);
				}

				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setRule(removeOperation.getSchedulingRule());
		job.schedule();

		return null;
	}

	@Override
	public boolean isEnabled() {
		return getProjectsInRepositoryOfSelectedResources().length > 0;
	}

}
