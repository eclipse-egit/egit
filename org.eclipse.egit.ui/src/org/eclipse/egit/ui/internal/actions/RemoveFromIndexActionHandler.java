/*******************************************************************************
 * Copyright (C) 2011, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.RemoveFromIndexOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Action for removing resource from the git index
 *
 * @see AddToIndexOperation
 */
public class RemoveFromIndexActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Collection<IPath> locations = getSelectedAndRelatedLocations(event);
		if (locations.isEmpty())
			return null;

		final RemoveFromIndexOperation removeOperation = new RemoveFromIndexOperation(locations);
		Job job = new Job(UIText.RemoveFromIndexAction_removingFiles) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					removeOperation.execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getStatus()
							.getMessage(), e);
				} finally {
					monitor.done();
				}

				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.REMOVE_FROM_INDEX.equals(family))
					return true;

				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(removeOperation.getSchedulingRule());
		job.schedule();

		return null;
	}

	private Collection<IPath> getSelectedAndRelatedLocations(ExecutionEvent event)
			throws ExecutionException {
		final IPath[] selectedLocations = getSelectedLocations(event);
		if (selectedLocations.length == 0)
			return Collections.emptyList();

		final LinkedHashSet<IPath> locations = new LinkedHashSet<IPath>(
				Arrays.asList(selectedLocations));

		final IResource[] selectedResources = getSelectedResources(event);
		if (selectedResources.length != 0) {
			final IWorkbenchPart part = getPart(event);
			try {
				final IResource[] resourcesInScope = GitScopeUtil
						.getRelatedChanges(part, selectedResources);
				for (IResource resource : resourcesInScope) {
					IPath l = resource.getLocation();
					if (l != null)
						locations.add(l);
				}
			} catch (InterruptedException e) {
				// ignore, we will not execute the operation in case the user
				// cancels the scope operation
				return Collections.emptyList();
			}
		}
		return locations;
	}

	@Override
	public boolean isEnabled() {
		return getProjectsInRepositoryOfSelectedResources().length > 0;
	}

}
