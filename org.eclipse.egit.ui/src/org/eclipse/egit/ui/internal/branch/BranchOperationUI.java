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
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Refactor
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.BranchProjectTracker;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.NonDeletedFilesDialog;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * The UI wrapper for {@link BranchOperation}
 */
public class BranchOperationUI {

	private final Repository repository;

	private String target;

	/**
	 * In the case of checkout conflicts, a dialog is shown to let the user
	 * stash, reset or commit. After that, checkout is tried again. The second
	 * time we do checkout, we don't want to ask any questions we already asked
	 * the first time, so this will be false then.
	 */
	private final boolean showQuestionsBeforeCheckout;

	/**
	 * Create an operation for checking out a branch
	 *
	 * @param repository
	 * @param target
	 *            a valid {@link Ref} name or commit id
	 * @return the {@link BranchOperationUI}
	 */
	public static BranchOperationUI checkout(Repository repository,
			String target) {
		return new BranchOperationUI(repository, target, true);
	}

	/**
	 * @param refName
	 *            the full ref name which will be checked out
	 * @return true if checkout will need additional input from the user before
	 *         continuing
	 */
	public static boolean checkoutWillShowQuestionDialog(String refName) {
		return shouldShowCheckoutRemoteTrackingDialog(refName);
	}

	/**
	 * @param repository
	 * @param target
	 * @param showQuestionsBeforeCheckout
	 */
	private BranchOperationUI(Repository repository, String target,
			boolean showQuestionsBeforeCheckout) {
		this.repository = repository;
		this.target = target;
		this.showQuestionsBeforeCheckout = showQuestionsBeforeCheckout;
	}

	private String confirmTarget(IProgressMonitor monitor) {
		if (target != null) {
			if (!repository.getRepositoryState().canCheckout()) {
				PlatformUI.getWorkbench().getDisplay()
						.asyncExec(new Runnable() {
							@Override
							public void run() {
								MessageDialog.openError(getShell(),
										UIText.BranchAction_cannotCheckout,
										NLS.bind(
												UIText.BranchAction_repositoryState,
												repository.getRepositoryState()
														.getDescription()));
							}
						});
				return null;
			}

			if (LaunchFinder.shouldCancelBecauseOfRunningLaunches(repository,
					monitor)) {
				return null;
			}

			askForTargetIfNecessary();
		}
		return target;
	}

	private void doCheckout(BranchOperation bop, boolean restore,
			IProgressMonitor monitor)
			throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, restore ? 10 : 1);
		if (!restore) {
			bop.execute(progress.newChild(1));
		} else {
			final BranchProjectTracker tracker = new BranchProjectTracker(
					repository);
			ProjectTrackerMemento snapshot = tracker.snapshot();
			bop.execute(progress.newChild(7));
			tracker.save(snapshot);
			IWorkspaceRunnable action = new IWorkspaceRunnable() {

				@Override
				public void run(IProgressMonitor innerMonitor)
						throws CoreException {
					tracker.restore(innerMonitor);
				}
			};
			ResourcesPlugin.getWorkspace().run(action,
					ResourcesPlugin.getWorkspace().getRoot(),
					IWorkspace.AVOID_UPDATE, progress.newChild(3));
		}
	}

	/**
	 * Starts the operation asynchronously
	 */
	public void start() {
		target = confirmTarget(new NullProgressMonitor());
		if (target == null) {
			return;
		}
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		String jobname = NLS.bind(UIText.BranchAction_checkingOut, repoName,
				target);
		boolean restore = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.CHECKOUT_PROJECT_RESTORE);
		final CheckoutJob job = new CheckoutJob(jobname, restore);
		job.setUser(true);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent cevent) {
				show(job.getCheckoutResult());
			}
		});
		job.schedule();
	}

	private class CheckoutJob extends Job {

		private BranchOperation bop;

		private final boolean restore;

		public CheckoutJob(String jobName, boolean restore) {
			super(jobName);
			this.restore = restore;
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			bop = new BranchOperation(repository, target, !restore);
			try {
				doCheckout(bop, restore, monitor);
			} catch (CoreException e) {
				switch (bop.getResult().getStatus()) {
				case CONFLICTS:
				case NONDELETED:
					break;
				default:
					return Activator.createErrorStatus(
							UIText.BranchAction_branchFailed, e);
				}
			} finally {
				GitLightweightDecorator.refresh();
				monitor.done();
			}
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			if (JobFamilies.CHECKOUT.equals(family))
				return true;
			return super.belongsTo(family);
		}

		@NonNull
		public CheckoutResult getCheckoutResult() {
			return bop.getResult();
		}
	}

	/**
	 * Runs the operation synchronously.
	 *
	 * @param monitor
	 * @throws CoreException
	 *
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		target = confirmTarget(progress.newChild(20));
		if (target == null) {
			return;
		}
		final boolean restore = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.CHECKOUT_PROJECT_RESTORE);
		BranchOperation bop = new BranchOperation(repository, target, !restore);
		doCheckout(bop, restore, progress.newChild(80));
		show(bop.getResult());
	}

	private void askForTargetIfNecessary() {
		if (target != null && showQuestionsBeforeCheckout) {
			if (shouldShowCheckoutRemoteTrackingDialog(target))
				target = getTargetWithCheckoutRemoteTrackingDialog();
		}
	}

	private static boolean shouldShowCheckoutRemoteTrackingDialog(String refName) {
		boolean isRemoteTrackingBranch = refName != null
				&& refName.startsWith(Constants.R_REMOTES);
		if (isRemoteTrackingBranch) {
			boolean showDetachedHeadWarning = Activator.getDefault()
					.getPreferenceStore()
					.getBoolean(UIPreferences.SHOW_DETACHED_HEAD_WARNING);
			// If the user has not yet chosen to ignore the warning about
			// getting into a "detached HEAD" state, then we show them a dialog
			// whether a remote-tracking branch should be checked out with a
			// detached HEAD or checking it out as a new local branch.
			return showDetachedHeadWarning;
		} else {
			return false;
		}
	}

	private String getTargetWithCheckoutRemoteTrackingDialog() {
		final String[] dialogResult = new String[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				dialogResult[0] = getTargetWithCheckoutRemoteTrackingDialogInUI();
			}
		});
		return dialogResult[0];
	}

	private String getTargetWithCheckoutRemoteTrackingDialogInUI() {
		String[] buttons = new String[] {
				UIText.BranchOperationUI_CheckoutRemoteTrackingAsLocal,
				UIText.BranchOperationUI_CheckoutRemoteTrackingCommit,
				IDialogConstants.CANCEL_LABEL };
		MessageDialog questionDialog = new MessageDialog(
				getShell(),
				UIText.BranchOperationUI_CheckoutRemoteTrackingTitle,
				null,
				UIText.BranchOperationUI_CheckoutRemoteTrackingQuestion,
				MessageDialog.QUESTION, buttons, 0);
		int result = questionDialog.open();
		if (result == 0) {
			// Check out as new local branch
			CreateBranchWizard wizard = new CreateBranchWizard(repository,
					target);
			WizardDialog createBranchDialog = new WizardDialog(getShell(),
					wizard);
			createBranchDialog.open();
			return null;
		} else if (result == 1) {
			// Check out commit
			return target;
		} else {
			// Cancel
			return null;
		}
	}

	private Shell getShell() {
		return PlatformUI.getWorkbench().getDisplay().getActiveShell();
	}

	/**
	 * @param result
	 *            the result to show
	 */
	private void show(final @NonNull CheckoutResult result) {
		if (result.getStatus() == CheckoutResult.Status.CONFLICTS) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					Shell shell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					CleanupUncomittedChangesDialog cleanupUncomittedChangesDialog = new CleanupUncomittedChangesDialog(
							shell,
							UIText.BranchResultDialog_CheckoutConflictsTitle,
							NLS.bind(
									UIText.BranchResultDialog_CheckoutConflictsMessage,
									Repository.shortenRefName(target)),
							repository, result.getConflictList());
					cleanupUncomittedChangesDialog.open();
					if (cleanupUncomittedChangesDialog.shouldContinue()) {
						BranchOperationUI op = new BranchOperationUI(repository,
								target, false);
						op.start();
					}
				}
			});
		} else if (result.getStatus() == CheckoutResult.Status.NONDELETED) {
			// double-check if the files are still there
			boolean show = false;
			List<String> pathList = result.getUndeletedList();
			for (String path : pathList)
				if (new File(repository.getWorkTree(), path).exists()) {
					show = true;
					break;
				}
			if (!show)
				return;
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					Shell shell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					new NonDeletedFilesDialog(shell, repository, result
							.getUndeletedList()).open();
				}
			});
		} else if (result.getStatus() == CheckoutResult.Status.OK) {
			if (RepositoryUtil.isDetachedHead(repository))
				showDetachedHeadWarning();
		}
	}

	private void showDetachedHeadWarning() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				IPreferenceStore store = Activator.getDefault()
						.getPreferenceStore();

				if (store.getBoolean(UIPreferences.SHOW_DETACHED_HEAD_WARNING)) {
					String toggleMessage = UIText.BranchResultDialog_DetachedHeadWarningDontShowAgain;

					MessageDialogWithToggle dialog = new MessageDialogWithToggle(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow()
									.getShell(),
							UIText.BranchOperationUI_DetachedHeadTitle, null,
							UIText.BranchOperationUI_DetachedHeadMessage,
							MessageDialog.INFORMATION,
							new String[] { IDialogConstants.CLOSE_LABEL },
							0, toggleMessage, false);
					dialog.open();
					if (dialog.getToggleState()) {
						store.setValue(UIPreferences.SHOW_DETACHED_HEAD_WARNING,
								false);
					}
				}
			}
		});
	}

}
