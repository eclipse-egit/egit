/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.IEGitOperation.PostExecuteTask;
import org.eclipse.egit.core.op.IEGitOperation.PreExecuteTask;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.AbstractBranchSelectionDialog;
import org.eclipse.egit.ui.internal.dialogs.CheckoutDialog;
import org.eclipse.egit.ui.internal.dialogs.DeleteBranchDialog;
import org.eclipse.egit.ui.internal.dialogs.RenameBranchDialog;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;

/**
 * The UI wrapper for {@link BranchOperation}
 */
public class BranchOperationUI {
	// create
	private final static int MODE_CREATE = 1;

	private final static int MODE_CHECKOUT = 2;

	private final static int MODE_DELETE = 3;

	private final static int MODE_RENAME = 4;

	private final Repository repository;

	private String target;

	/**
	 * In the case of checkout conflicts, a dialog is shown to let the user
	 * stash, reset or commit. After that, checkout is tried again. The second
	 * time we do checkout, we don't want to ask any questions we already asked
	 * the first time, so this will be false then.
	 */
	private final boolean showQuestionAboutTarget;

	private final int mode;

	/**
	 * Create an operation for manipulating branches
	 *
	 * @param repository
	 * @return the {@link BranchOperationUI}
	 */
	public static BranchOperationUI rename(Repository repository) {
		return new BranchOperationUI(repository, MODE_RENAME);
	}

	/**
	 * Create an operation for manipulating branches
	 *
	 * @param repository
	 * @return the {@link BranchOperationUI}
	 */
	public static BranchOperationUI delete(Repository repository) {
		return new BranchOperationUI(repository, MODE_DELETE);
	}

	/**
	 * Create an operation for creating a local branch
	 *
	 * @param repository
	 * @return the {@link BranchOperationUI}
	 */
	public static BranchOperationUI create(Repository repository) {
		BranchOperationUI op = new BranchOperationUI(repository, MODE_CREATE);
		return op;
	}

	/**
	 * Create an operation for checking out a local branch
	 *
	 * @param repository
	 * @return the {@link BranchOperationUI}
	 */
	public static BranchOperationUI checkout(Repository repository) {
		return new BranchOperationUI(repository, MODE_CHECKOUT);
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
		return new BranchOperationUI(repository, target, true);
	}

	/**
	 * Create an operation for checking out a branch without showing a question
	 * dialog about the target.
	 *
	 * @param repository
	 * @param target
	 *            a valid {@link Ref} name or commit id
	 * @return the {@link BranchOperationUI}
	 */
	public static BranchOperationUI checkoutWithoutQuestion(
			Repository repository, String target) {
		return new BranchOperationUI(repository, target, false);
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
	 * @param showQuestionAboutTarget
	 */
	private BranchOperationUI(Repository repository, String target,
			boolean showQuestionAboutTarget) {
		this.repository = repository;
		this.target = target;
		this.showQuestionAboutTarget = showQuestionAboutTarget;
		this.mode = 0;
	}

	/**
	 * Select and checkout a branch
	 *
	 * @param repository
	 * @param mode
	 */
	private BranchOperationUI(Repository repository, int mode) {
		this.repository = repository;
		this.mode = mode;
		this.showQuestionAboutTarget = true;
	}

	/**
	 * Starts the operation asynchronously
	 */
	public void start() {
		if (!repository.getRepositoryState().canCheckout()) {
			MessageDialog.openError(getShell(),
					UIText.BranchAction_cannotCheckout, NLS.bind(
							UIText.BranchAction_repositoryState, repository
									.getRepositoryState().getDescription()));
			return;
		}

		askForTargetIfNecessary();
		if (target == null)
			return;

		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		String jobname = NLS.bind(UIText.BranchAction_checkingOut, repoName,
				target);

		final boolean restore = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.CHECKOUT_PROJECT_RESTORE);
		final BranchOperation bop = new BranchOperation(repository, target,
				!restore);

		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (restore) {
						final BranchProjectTracker tracker = new BranchProjectTracker(
								repository);
						final AtomicReference<IMemento> memento = new AtomicReference<IMemento>();
						bop.addPreExecuteTask(new PreExecuteTask() {

							public void preExecute(Repository pRepo,
									IProgressMonitor pMonitor)
									throws CoreException {
								// Snapshot current projects before checkout
								// begins
								memento.set(tracker.snapshot());
							}
						});
						bop.addPostExecuteTask(new PostExecuteTask() {

							public void postExecute(Repository pRepo,
									IProgressMonitor pMonitor)
									throws CoreException {
								IMemento snapshot = memento.get();
								if (snapshot == null)
									return;
								// Save previous branch's projects and restore
								// current branch's projects
								tracker.save(snapshot).restore(pMonitor);
							}
						});
					}

					bop.execute(monitor);
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
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.CHECKOUT))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent cevent) {
				BranchResultDialog.show(bop.getResult(), repository, target);
			}
		});
		job.schedule();
	}

	/**
	 * Runs the operation synchronously
	 *
	 * @param monitor
	 * @throws CoreException
	 *
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		if (!repository.getRepositoryState().canCheckout()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(getShell(),
							UIText.BranchAction_cannotCheckout, NLS.bind(
									UIText.BranchAction_repositoryState,
									repository.getRepositoryState()
											.getDescription()));
				}
			});
			return;
		}

		askForTargetIfNecessary();
		if (target == null)
			return;

		BranchOperation bop = new BranchOperation(repository, target);
		bop.execute(monitor);

		BranchResultDialog.show(bop.getResult(), repository, target);
	}

	private void askForTargetIfNecessary() {
		if (target == null)
			target = getTargetWithDialog();
		if (target != null && showQuestionAboutTarget) {
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

	private String getTargetWithDialog() {
		AbstractBranchSelectionDialog dialog;
		switch (mode) {
		case MODE_CHECKOUT:
			dialog = new CheckoutDialog(getShell(), repository);
			break;
		case MODE_CREATE:
			CreateBranchWizard wiz;
			try {
				wiz = new CreateBranchWizard(repository, repository.getFullBranch());
			} catch (IOException e) {
				wiz = new CreateBranchWizard(repository);
			}
			new WizardDialog(getShell(), wiz).open();
			return null;
		case MODE_DELETE:
			new DeleteBranchDialog(getShell(), repository).open();
			return null;
		case MODE_RENAME:
			new RenameBranchDialog(getShell(), repository).open();
			return null;
		default:
			return null;
		}

		if (dialog.open() != Window.OK) {
			return null;
		}
		return dialog.getRefName();
	}

	private String getTargetWithCheckoutRemoteTrackingDialog() {
		String[] buttons = new String[] {
				UIText.BranchOperationUI_CheckoutRemoteTrackingAsLocal,
				IDialogConstants.CANCEL_LABEL,
				UIText.BranchOperationUI_CheckoutRemoteTrackingCommit };
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
			// Cancel
			return null;
		} else {
			// Check out commit
			return target;
		}
	}

	private Shell getShell() {
		return PlatformUI.getWorkbench().getDisplay().getActiveShell();
	}
}
