/*******************************************************************************
 * Copyright (C) 2010, Roland Grunberg <rgrunber@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.storage.file.Repository;

/**
 * Checkout all selected dirty files.
 */
public class DiscardChangesAction extends RepositoryAction {

	@Override
	public void execute(IAction action) {

		boolean performAction = MessageDialog.openConfirm(getShell(),
				UIText.DiscardChangesAction_confirmActionTitle,
				UIText.DiscardChangesAction_confirmActionMessage);
		if (!performAction)
			return;
		final DiscardChangesOperation operation = new DiscardChangesOperation(
				getSelectedResources());
		String jobname = UIText.DiscardChangesAction_discardChanges;
		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					operation.execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(e.getStatus()
							.getMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setRule(operation.getSchedulingRule());
		job.schedule();
	}

	@Override
	public boolean isEnabled() {
		for (IResource res : getSelectedResources()) {
			IProject[] proj = new IProject[] { res.getProject() };
			Repository repository = getRepositoriesFor(proj)[0];
			if (!repository.getRepositoryState().equals(RepositoryState.SAFE)) {
				return false;
			}
		}
		return true;
	}

}
