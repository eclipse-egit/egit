/*******************************************************************************
 * Copyright (C) 2010, 2016 Roland Grunberg <rgrunber@redhat.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Benjamin Muskalla (Tasktop Technologies Inc.) - support for model scoping
 *   François Rey <eclipse.org_@_francois_._rey_._name> - handling of linked resources
 *   Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Checkout all selected dirty files.
 */
public class DiscardChangesActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// capture selection from active part as long as we have context
		mySelection = getSelection(event);
		try {
			IWorkbenchPart part = getPart(event);
			String question = UIText.DiscardChangesAction_confirmActionMessage;
			ILaunchConfiguration launch = LaunchFinder
					.getRunningLaunchConfiguration(
							Arrays.asList(getRepositories()), null);
			if (launch != null) {
				question = MessageFormat.format(question,
						"\n\n" + MessageFormat.format( //$NON-NLS-1$
								UIText.LaunchFinder_RunningLaunchMessage,
								launch.getName()));
			} else {
				question = MessageFormat.format(question, ""); //$NON-NLS-1$
			}
			boolean performAction = openConfirmationDialog(event, question);
			if (!performAction) {
				return null;
			}
			final DiscardChangesOperation operation = createOperation(part,
					event);

			if (operation == null) {
				return null;
			}
			String jobname = UIText.DiscardChangesAction_discardChanges;
			Job job = new WorkspaceJob(jobname) {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					try {
						operation.execute(monitor);
					} catch (CoreException e) {
						return Activator.createErrorStatus(
								e.getStatus().getMessage(), e);
					}
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					if (JobFamilies.DISCARD_CHANGES.equals(family)) {
						return true;
					}
					return super.belongsTo(family);
				}
			};
			job.setUser(true);
			job.setRule(operation.getSchedulingRule());
			job.schedule();
			return null;
		} finally {
			// cleanup mySelection to avoid side effects later after execution
			mySelection = null;
		}
	}

	private boolean openConfirmationDialog(ExecutionEvent event,
			String question) throws ExecutionException {
		MessageDialog dlg = new MessageDialog(getShell(event),
				UIText.DiscardChangesAction_confirmActionTitle, null, question,
				MessageDialog.CONFIRM,
				new String[] {
						UIText.DiscardChangesAction_discardChangesButtonText,
						IDialogConstants.CANCEL_LABEL },
				0);
		return dlg.open() == Window.OK;
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
