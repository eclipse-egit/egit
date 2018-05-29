/*******************************************************************************
 * Copyright (C) 2011, 2012 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.Arrays;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
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

/**
 * Action for removing resource from the git index
 *
 * @see AddToIndexOperation
 */
public class RemoveFromIndexActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IPath[] sel = getSelectedLocations(event);
		if (sel.length == 0)
			return null;

		final RemoveFromIndexOperation removeOperation = new RemoveFromIndexOperation(Arrays.asList(sel));
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

	@Override
	public boolean isEnabled() {
		return haveSelectedResourcesWithRepository();
	}

}
