/*******************************************************************************
 * Copyright (C) 2010, 2013 Roland Grunberg <rgrunber@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Benjamin Muskalla (Tasktop Technologies Inc.) - support for model scoping
 *   François Rey <eclipse.org_@_francois_._rey_._name> - handling of linked resources
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Checkout all selected dirty files.
 */
public class DiscardChangesActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchPart part = getPart(event);
		boolean performAction = MessageDialog.openConfirm(getShell(event),
				UIText.DiscardChangesAction_confirmActionTitle,
				UIText.DiscardChangesAction_confirmActionMessage);
		if (!performAction)
			return null;
		final DiscardChangesOperation operation = createOperation(part, event);
		if (operation == null)
			return null;
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

			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.DISCARD_CHANGES))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(operation.getSchedulingRule());
		job.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository[] repositories = getRepositories();
		if (repositories.length == 0)
			return false;
		for (Repository repository : repositories) {
			if (!repository.getRepositoryState().equals(RepositoryState.SAFE))
				return false;
		}
		return true;
	}

	private DiscardChangesOperation createOperation(IWorkbenchPart part,
			ExecutionEvent event) throws ExecutionException {

		IResource[] selectedResources = gatherResourceToOperateOn(event);
		String revision;
		try {
			revision = gatherRevision(event);
		} catch (OperationCanceledException e) {
			return null;
		}

		IResource[] resourcesInScope;
		try {
			resourcesInScope = GitScopeUtil.getRelatedChanges(part,
					selectedResources);
		} catch (InterruptedException e) {
			// ignore, we will not discard the files in case the user
			// cancels the scope operation
			return null;
		}

		return new DiscardChangesOperation(resourcesInScope, revision);

	}

	/**
	 * @param event
	 * @return set of resources to operate on
	 * @throws ExecutionException
	 */
	protected IResource[] gatherResourceToOperateOn(ExecutionEvent event)
			throws ExecutionException {
		return getSelectedResources(event);
	}

	/**
	 * @param event
	 * @return the revision to use
	 * @throws ExecutionException
	 */
	protected String gatherRevision(ExecutionEvent event)
			throws ExecutionException {
		return null;
	}
}
