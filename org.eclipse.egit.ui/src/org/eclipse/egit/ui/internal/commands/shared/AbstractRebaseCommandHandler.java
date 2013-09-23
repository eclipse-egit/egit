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
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.RebaseOperation;
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
public abstract class AbstractRebaseCommandHandler extends AbstractSharedCommandHandler {

	/**
	 * The jobname to be displayed
	 */
	protected String jobname;

	private final String dialogMessage;

	/**
	 * @param jobname
	 * @param dialogMessage
	 */
	protected AbstractRebaseCommandHandler(String jobname, String dialogMessage) {
		this.dialogMessage = dialogMessage;
		this.jobname = jobname;
	}

	/**
	 * @param rebase
	 * @return the result of the execution. Reserved for future use, must be
	 *         null.
	 * @throws ExecutionException
	 */
	protected Object execute(final RebaseOperation rebase)
			throws ExecutionException {
		final Repository repository = rebase.getRepository();
		final RebaseCommand.Operation operation = rebase.getOperation();

		JobUtil.scheduleUserJob(rebase, jobname, JobFamilies.REBASE,
				new JobChangeAdapter() {
					@Override
					public void aboutToRun(IJobChangeEvent event) {
						// safeguard against broken handlers which don't check
						// that repository state is safe
						if (operation == Operation.BEGIN
								&& !repository.getRepositoryState().equals(
										RepositoryState.SAFE)) {
							throw new IllegalStateException(
									"Can't start rebase if repository state isn't SAFE"); //$NON-NLS-1$
						}
						super.aboutToRun(event);
					}

					@Override
					public void done(IJobChangeEvent cevent) {
						IStatus result = cevent.getJob().getResult();
						// if a rebase was started, returned with an exception
						// and left the repository in an unsafe state, try to
						// abort and show exception
						if (operation == Operation.BEGIN
								&& result.getSeverity() == IStatus.ERROR) {
							if (!repository.getRepositoryState().equals(
									RepositoryState.SAFE)) {
								Throwable t = result.getException();
								try {
									new RebaseOperation(repository,
											Operation.ABORT).execute(null);
									Activator.showError(t.getMessage(), t);
								} catch (CoreException e1) {
									IStatus mStatus = createMultiStatus(t, e1);
									CoreException mStatusException = new CoreException(
											mStatus);
									Activator.showError(
											mStatusException.getMessage(),
											mStatusException);
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
									MessageDialog
											.openInformation(
													shell,
													UIText.AbstractRebaseCommand_DialogTitle,
													dialogMessage);
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
		final RebaseOperation rebase = createRebaseOperation(event);
		if (rebase == null)
			return null;
		return execute(rebase);
	}

	/**
	 * @param event
	 * @return the {@link RebaseOperation} to be executed
	 * @throws ExecutionException
	 */
	protected abstract RebaseOperation createRebaseOperation(
			ExecutionEvent event) throws ExecutionException;

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
	 * @param ctx the context
	 * @return the editor input for the given context or <code>null</code> if not available
	 * @since 2.1
	 */
	protected IEditorInput getActiveEditorInput(IEvaluationContext ctx) {
		Object editorInput = ctx.getVariable(ISources.ACTIVE_EDITOR_INPUT_NAME);
		if (editorInput instanceof IEditorInput)
			return (IEditorInput) editorInput;

		return null;
	}

	private static IStatus createMultiStatus(Throwable originalException,
			Throwable e) {
		IStatus childStatus = Activator.createErrorStatus(
				originalException.getMessage(),
				originalException);
		return new MultiStatus(Activator.getPluginId(), IStatus.ERROR,
				new IStatus[] { childStatus }, e.getMessage(), e);
	}

}
