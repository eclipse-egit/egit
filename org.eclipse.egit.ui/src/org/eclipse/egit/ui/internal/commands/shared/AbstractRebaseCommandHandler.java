/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.rebase.RebaseResultDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;

/**
 * Rebase command base class
 */
public abstract class AbstractRebaseCommandHandler extends
		AbstractSharedCommandHandler {

	private final String dialogMessage;

	private final String dialogTitle;

	/**
	 * @param dialogMessage
	 * @param dialogTitle
	 */
	protected AbstractRebaseCommandHandler(String dialogMessage,
			String dialogTitle) {
		this.dialogMessage = dialogMessage;
		this.dialogTitle = dialogTitle;
	}

	/**
	 * @param rebase
	 * @return the result of the execution. Reserved for future use, must be
	 *         null.
	 * @throws ExecutionException
	 */
	protected Object execute(final RebaseOperation rebase)
			throws ExecutionException {
		String jobname = getJobName(rebase);
		final Repository repository = rebase.getRepository();
		final RebaseCommand.Operation operation = rebase.getOperation();

		JobUtil.scheduleUserJob(rebase, jobname, JobFamilies.REBASE,
				new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent cevent) {
						IStatus result = cevent.getJob().getResult();
						// if a rebase was started, returned with an exception
						// and left the repository in an unsafe state, try to
						// abort
						if (operation == Operation.BEGIN
								&& result.getSeverity() == IStatus.ERROR) {
							if (!repository.getRepositoryState().equals(
									RepositoryState.SAFE)) {
								try {
									new RebaseOperation(repository,
											Operation.ABORT).execute(null);
								} catch (CoreException e) {
									Activator
											.error(UIText.AbstractRebaseCommandHandler_CleanUpFailed,
													e);
								}
							}
						}
						if (result.getSeverity() == IStatus.CANCEL)
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									// don't use getShell(event) here since
									// the active shell has changed since the
									// execution has been triggered.
									Shell shell = PlatformUI.getWorkbench()
											.getActiveWorkbenchWindow()
											.getShell();
									MessageDialog.openInformation(shell,
											dialogTitle, dialogMessage);
								}
							});
						else if (result.isOK())
							RebaseResultDialog.show(rebase.getResult(),
									repository);
					}
				});
		return null;
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RebaseOperation rebase = getRebaseOperation(event);
		if (rebase == null)
			return null;
		return execute(rebase);
	}

	/**
	 * @param event
	 * @return the {@link RebaseOperation} to be executed
	 * @throws ExecutionException
	 */
	public abstract RebaseOperation getRebaseOperation(ExecutionEvent event)
			throws ExecutionException;

	/**
	 * @param operation
	 * @return the jobname to be used
	 * @throws ExecutionException
	 */
	public abstract String getJobName(RebaseOperation operation)
			throws ExecutionException;


	/**
	 * Retrieve the current selection. The global selection is used if the menu
	 * selection is not available.
	 *
	 * @param ctx
	 * @return the selection
	 */
	protected Object getSelection(IEvaluationContext ctx) {
		Object selection = ctx.getVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
		if (selection == null || !(selection instanceof ISelection))
			selection = ctx.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
		return selection;
	}

	/**
	 * Extracts the editor input from the given context.
	 *
	 * @param ctx
	 *            the context
	 * @return the editor input for the given context or <code>null</code> if
	 *         not available
	 * @since 2.1
	 */
	protected IEditorInput getActiveEditorInput(IEvaluationContext ctx) {
		Object editorInput = ctx.getVariable(ISources.ACTIVE_EDITOR_INPUT_NAME);
		if (editorInput instanceof IEditorInput)
			return (IEditorInput) editorInput;

		return null;
	}

}
