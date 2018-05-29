/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.CleanupUncomittedChangesDialog;
import org.eclipse.egit.ui.internal.rebase.RebaseResultDialog;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
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
		this.jobname = jobname;
		this.dialogMessage = dialogMessage;
	}

	/**
	 * Executes the given {@link RebaseOperation}
	 *
	 * @param rebase
	 * @throws ExecutionException
	 */
	protected void execute(final RebaseOperation rebase)
			throws ExecutionException {
		final Repository repository = rebase.getRepository();
		if (repository == null)
			return;
		final RebaseCommand.Operation operation = rebase.getOperation();

		startRebaseJob(rebase, repository, operation);
	}

	private void startRebaseJob(final RebaseOperation rebase,
			final Repository repository, final RebaseCommand.Operation operation) {
		JobUtil.scheduleUserWorkspaceJob(rebase, jobname, JobFamilies.REBASE,
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
						finishRebaseInteractive();
						IStatus result = cevent.getJob().getResult();
						if (result == null) {
							return;
						}
						// if a rebase was started, returned with an exception
						// and left the repository in an unsafe state, try to
						// abort and show exception
						if (operation == Operation.BEGIN
								&& result.getSeverity() == IStatus.ERROR) {
							handleBeginError(repository, result);
						} else if (result.getSeverity() == IStatus.CANCEL)
							Display.getDefault().asyncExec(new Runnable() {
								@Override
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
						else if (result.isOK()) {
							if (rebase.getResult().getStatus() == Status.UNCOMMITTED_CHANGES) {
								handleUncommittedChanges(repository,
										rebase.getResult()
												.getUncommittedChanges());
							} else {
								RebaseResultDialog.show(rebase.getResult(),
										repository);
								if (operation == Operation.ABORT)
									setAmending(false, false);
								if (rebase.getResult().getStatus() == Status.EDIT)
									setAmending(true, true);
							}
						}
					}

					private void setAmending(final boolean amending,
							final boolean openStagingView) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								try {
									IViewPart view;
									if (openStagingView)
										view = PlatformUI.getWorkbench()
												.getActiveWorkbenchWindow()
												.getActivePage()
												.showView(StagingView.VIEW_ID);
									else
										view = PlatformUI
											.getWorkbench()
											.getActiveWorkbenchWindow()
											.getActivePage()
											.findView(StagingView.VIEW_ID);
									if (view instanceof StagingView) {
										final StagingView sv = (StagingView) view;
										sv.reload(repository);
										Display.getDefault().asyncExec(
												new Runnable() {
													@Override
													public void run() {
														sv.setAmending(amending);
													}
												});
									}
								} catch (PartInitException e) {
									Activator.logError(e.getMessage(),
											e);
								}
							}
						});
					}

					private void finishRebaseInteractive() {
						RebaseInteractivePlan plan = RebaseInteractivePlan
								.getPlan(repository);
						if (plan != null && !plan.isRebasingInteractive())
							plan.dispose();
					}
				});
	}

	private void handleUncommittedChanges(final Repository repository,
			final List<String> files) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				String repoName = Activator.getDefault().getRepositoryUtil()
						.getRepositoryName(repository);
				CleanupUncomittedChangesDialog cleanupUncomittedChangesDialog = new CleanupUncomittedChangesDialog(
						shell,
						MessageFormat
								.format(UIText.AbstractRebaseCommandHandler_cleanupDialog_title,
										repoName),
						UIText.AbstractRebaseCommandHandler_cleanupDialog_text,
						repository, files);
				cleanupUncomittedChangesDialog.open();
				if (cleanupUncomittedChangesDialog.shouldContinue()) {
					try {
						execute(repository);
					} catch (ExecutionException e) {
						Activator.logError(e.getMessage(), e);
					}
				}
			}
		});
	}


	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		execute(repository);
		return null;
	}

	/**
	 * Create a {@link RebaseOperation} by calling
	 * {@link AbstractRebaseCommandHandler#createRebaseOperation(Repository)}
	 * and execute it.
	 *
	 * @param repository
	 * @throws ExecutionException
	 */
	public void execute(Repository repository) throws ExecutionException {
		final RebaseOperation rebase = createRebaseOperation(repository);
		if (rebase != null) {
			execute(rebase);
		}
	}

	private void handleBeginError(final Repository repository, IStatus result) {
		if (!repository.getRepositoryState().equals(RepositoryState.SAFE)) {
			Throwable t = result.getException();
			try {
				new RebaseOperation(repository, Operation.ABORT).execute(null);
				Activator.showError(t.getMessage(), t);
			} catch (CoreException e1) {
				IStatus childStatus = Activator.createErrorStatus(
						e1.getMessage(), e1);
				IStatus mStatus = new MultiStatus(Activator.getPluginId(),
						IStatus.ERROR, new IStatus[] { childStatus },
						t.getMessage(), t);
				CoreException mStatusException = new CoreException(mStatus);
				Activator.showError(mStatusException.getMessage(),
						mStatusException);
			}
		}
	}

	/**
	 * Factory method delegating creation of RebaseOperation to concrete
	 * subclasses.
	 *
	 * @param repository
	 * @return the {@link RebaseOperation} to be executed
	 * @throws ExecutionException
	 */
	protected abstract RebaseOperation createRebaseOperation(
			Repository repository) throws ExecutionException;

}
