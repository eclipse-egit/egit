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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.UIText;
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

	private final Repository[] repositories;

	private String target;

	private boolean isSingleRepositoryOperation;

	/**
	 * In the case of checkout conflicts, a dialog is shown to let the user
	 * stash, reset or commit. After that, checkout is tried again. The second
	 * time we do checkout, we don't want to ask any questions we already asked
	 * the first time, so this will be false then.
	 * <p>
	 * This behavior is disabled when checking out multiple repositories at
	 * once.
	 * </p>
	 */
	private final boolean showQuestionsBeforeCheckout;

	/**
	 * Create an operation for checking out a branch on multiple repositories.
	 *
	 * @param repositories
	 * @param target
	 *            a valid {@link Ref} name or commit id
	 * @return the {@link BranchOperationUI}
	 */
	public static BranchOperationUI checkout(Repository[] repositories,
			String target) {
		return new BranchOperationUI(repositories, target, true);
	}

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
		return checkout(repository, target, true);
	}

	/**
	 * Create an operation for checking out a branch
	 *
	 * @param repository
	 * @param target
	 *            a valid {@link Ref} name or commit id
	 * @param showQuestionsBeforeCheckout
	 * @return the {@link BranchOperationUI}
	 */
	public static BranchOperationUI checkout(Repository repository,
			String target, boolean showQuestionsBeforeCheckout) {
		return new BranchOperationUI(new Repository[] { repository }, target,
				showQuestionsBeforeCheckout);
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
	 * @param repositories
	 * @param target
	 * @param showQuestionsBeforeCheckout
	 */
	private BranchOperationUI(Repository[] repositories, String target,
			boolean showQuestionsBeforeCheckout) {
		this.repositories = repositories;
		this.target = target;
		/*
		 * We do not have support for CreateBranchWizards when performing
		 * checkout on multiple repositories at once, thus, the
		 * showQuestionsBeforeCheckout is forced to false in this case
		 */
		this.isSingleRepositoryOperation = repositories.length == 1;
		this.showQuestionsBeforeCheckout = isSingleRepositoryOperation
				? showQuestionsBeforeCheckout
				: false;
	}

	private String confirmTarget(IProgressMonitor monitor) {
		if (target == null) {
			return null;
		}
		Optional<Repository> invalidRepo = Stream.of(repositories)
				.filter(r -> !r.getRepositoryState().canCheckout()).findFirst();

		if (invalidRepo.isPresent()) {
			PlatformUI.getWorkbench().getDisplay()
					.asyncExec(() -> showRepositoryInInvalidStateForCheckout(
							invalidRepo.get()));
			return null;
		}

		Collection<Repository> repos = Arrays.asList(repositories);
		if (LaunchFinder.shouldCancelBecauseOfRunningLaunches(repos, monitor)) {
			return null;
		}

		askForTargetIfNecessary();
		return target;
	}

	private void showRepositoryInInvalidStateForCheckout(Repository repo) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repo);
		String description = repo.getRepositoryState().getDescription();
		String message = NLS.bind(UIText.BranchAction_repositoryState, repoName,
				description);

		MessageDialog.openError(getShell(), UIText.BranchAction_cannotCheckout,
				message);
	}

	private void doCheckout(BranchOperation bop, boolean restore,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, restore ? 10 : 1);
		if (!restore) {
			bop.execute(progress.newChild(1));
		} else {
			final BranchProjectTracker tracker = new BranchProjectTracker(
					repositories);
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
		if (repositories == null || repositories.length == 0) {
			return;
		}
		target = confirmTarget(new NullProgressMonitor());
		if (target == null) {
			return;
		}
		String jobname = getJobName(repositories, target);
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

	private static String getJobName(Repository[] repos, String target) {
		if (repos.length > 1) {
			return NLS.bind(UIText.BranchAction_checkingOutMultiple, target);
		}
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repos[0]);
		return NLS.bind(UIText.BranchAction_checkingOut, repoName, target);
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
			bop = new BranchOperation(repositories, target, !restore);
			try {
				doCheckout(bop, restore, monitor);
			} catch (CoreException e) {
				/*
				 * For a checkout operation with multiple repositories we can
				 * handle any error status by displaying all of them in a table.
				 * For a single repository, though, we will stick to using a
				 * simple message in case of an unexpected exception.
				 */
				if (!isSingleRepositoryOperation) {
					return Status.OK_STATUS;
				}
				CheckoutResult result = bop.getResult(repositories[0]);

				if (result.getStatus() == CheckoutResult.Status.CONFLICTS ||
						result.getStatus() == CheckoutResult.Status.NONDELETED) {
					return Status.OK_STATUS;
				}

				return Activator
						.createErrorStatus(UIText.BranchAction_branchFailed, e);
			} finally {
				GitLightweightDecorator.refresh();
				monitor.done();
			}
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return JobFamilies.CHECKOUT.equals(family)
					|| super.belongsTo(family);
		}

		@NonNull
		public Map<Repository, CheckoutResult> getCheckoutResult() {
			return bop.getResults();
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
		BranchOperation bop = new BranchOperation(repositories, target,
				!restore);
		doCheckout(bop, restore, progress.newChild(80));
		show(bop.getResults());
	}

	private void askForTargetIfNecessary() {
		if (target == null || !showQuestionsBeforeCheckout
				|| !shouldShowCheckoutRemoteTrackingDialog(target)) {
			return;
		}
		target = getTargetWithCheckoutRemoteTrackingDialog(repositories[0]);
	}

	private static boolean shouldShowCheckoutRemoteTrackingDialog(
			String refName) {
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

	private String getTargetWithCheckoutRemoteTrackingDialog(Repository repo) {
		final String[] dialogResult = new String[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(
				() -> dialogResult[0] = getTargetWithCheckoutRemoteTrackingDialogInUI(
						repo));

		return dialogResult[0];
	}

	private String getTargetWithCheckoutRemoteTrackingDialogInUI(
			Repository repo) {
		String[] buttons = new String[] {
				UIText.BranchOperationUI_CheckoutRemoteTrackingAsLocal,
				UIText.BranchOperationUI_CheckoutRemoteTrackingCommit,
				IDialogConstants.CANCEL_LABEL };
		MessageDialog questionDialog = new MessageDialog(getShell(),
				UIText.BranchOperationUI_CheckoutRemoteTrackingTitle, null,
				UIText.BranchOperationUI_CheckoutRemoteTrackingQuestion,
				MessageDialog.QUESTION, buttons, 0);
		int result = questionDialog.open();
		if (result == 0) {
			// Check out as new local branch
			CreateBranchWizard wizard = new CreateBranchWizard(repo,
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
	 * @param results
	 */
	private void show(final @NonNull Map<Repository, CheckoutResult> results) {
		if (allBranchOperationsSucceeded(results)) {
			if (anyRepositoryIsInDetachedHeadState(results)) {
				showDetachedHeadWarning();
			}
			return;
		}
		if (this.isSingleRepositoryOperation) {
			Repository repo = repositories[0];
			CheckoutResult result = results.get(repo);
			handleSingleRepositoryCheckoutOperationResult(repo,
					result);
		} else {
			handleMultipleRepositoryCheckoutError(results);
		}
	}

	private boolean allBranchOperationsSucceeded(
			@NonNull Map<Repository, CheckoutResult> results) {
		return results.values().stream()
				.allMatch(r -> r.getStatus() == CheckoutResult.Status.OK);
	}

	private boolean anyRepositoryIsInDetachedHeadState(
			final @NonNull Map<Repository, CheckoutResult> results) {
		return results.keySet().stream()
				.anyMatch(RepositoryUtil::isDetachedHead);
	}

	private void handleSingleRepositoryCheckoutOperationResult(
			Repository repository, CheckoutResult result) {

		if (result.getStatus() == CheckoutResult.Status.CONFLICTS) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				if (UIRepositoryUtils.showCleanupDialog(repository,
						result.getConflictList(),
						UIText.BranchResultDialog_CheckoutConflictsTitle,
						shell)) {
					BranchOperationUI.checkout(repository, target, false)
							.start();
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

			if (!show) {
				return;
			}
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				new NonDeletedFilesDialog(shell, repository,
						result.getUndeletedList()).open();
			});
		} else {
			String repoName = Activator.getDefault().getRepositoryUtil()
					.getRepositoryName(repository);
			String message = NLS.bind(
					UIText.BranchOperationUI_CheckoutError_DialogMessage,
					repoName, target);
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				MessageDialog.openError(shell,
						UIText.BranchOperationUI_CheckoutError_DialogTitle,
						message);
			});
		}
	}

	private void handleMultipleRepositoryCheckoutError(
			Map<Repository, CheckoutResult> results) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				new MultiBranchOperationResultDialog(shell, results).open();
			}
		);
	}

	private void showDetachedHeadWarning() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
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
						new String[] { IDialogConstants.CLOSE_LABEL }, 0,
						toggleMessage, false);
				dialog.open();
				if (dialog.getToggleState()) {
					store.setValue(UIPreferences.SHOW_DETACHED_HEAD_WARNING,
							false);
				}
			}
		});
	}
}
