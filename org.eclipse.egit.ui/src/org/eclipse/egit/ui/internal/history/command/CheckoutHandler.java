/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.osgi.util.NLS;

/**
 * Implements "Checkout" from history view
 */
public class CheckoutHandler extends AbstractHistoryViewCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		final PlotCommit commit = getSingleCommit(event);
		final Repository repo = getRepository(event);

		// for the sake of UI responsiveness, let's start a job
		Job job = new Job(NLS.bind(UIText.RepositoriesView_CheckingOutMessage,
				commit.getId().name())) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				final BranchOperation op = new BranchOperation(repo, commit
						.getId());
				IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

					public void run(IProgressMonitor myMonitor)
							throws CoreException {
						op.execute(myMonitor);
					}
				};

				try {
					ResourcesPlugin.getWorkspace().run(wsr,
							ResourcesPlugin.getWorkspace().getRoot(),
							IWorkspace.AVOID_UPDATE, monitor);
				} catch (CoreException e1) {
					return Activator.createErrorStatus(e1.getMessage(), e1);
				}

				return Status.OK_STATUS;
			}
		};

		job.setUser(true);
		job.schedule();

		return null;
	}

}
