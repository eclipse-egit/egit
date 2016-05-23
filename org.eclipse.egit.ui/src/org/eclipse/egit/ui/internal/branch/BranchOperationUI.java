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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
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
import org.eclipse.egit.ui.internal.dialogs.NonDeletedFilesDialog;
import org.eclipse.egit.ui.internal.dialogs.RenameBranchDialog;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
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

	private String base;

	/**
	 * In the case of checkout conflicts, a dialog is shown to let the user
	 * stash, reset or commit. After that, checkout is tried again. The second
	 * time we do checkout, we don't want to ask any questions we already asked
	 * the first time, so this will be false then.
	 */
	private final boolean showQuestionsBeforeCheckout;

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
	 * Create an operation for creating a local branch with a given base reference
	 *
	 * @param repository
	 * @param baseRef
	 * @return the {@link BranchOperationUI}
	 */
	public static BranchOperationUI createWithRef(Repository repository, String baseRef) {
		BranchOperationUI op = new BranchOperationUI(repository, MODE_CREATE);
		op.base = baseRef;
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
	 * @param showQuestionsBeforeCheckout
	 */
	private BranchOperationUI(Repository repository, String target,
			boolean showQuestionsBeforeCheckout) {
		this.repository = repository;
		this.target = target;
		this.showQuestionsBeforeCheckout = showQuestionsBeforeCheckout;
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
		this.showQuestionsBeforeCheckout = true;
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

		if (shouldCancelBecauseOfRunningLaunches(new NullProgressMonitor()))
			return;

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

		Job job = new WorkspaceJob(jobname) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					if (restore) {
						final BranchProjectTracker tracker = new BranchProjectTracker(
								repository);
						final AtomicReference<IMemento> memento = new AtomicReference<>();
						bop.addPreExecuteTask(new PreExecuteTask() {

							@Override
							public void preExecute(Repository pRepo,
									IProgressMonitor pMonitor)
									throws CoreException {
								// Snapshot current projects before checkout
								// begins
								memento.set(tracker.snapshot());
							}
						});
						bop.addPostExecuteTask(new PostExecuteTask() {

							@Override
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
				if (JobFamilies.CHECKOUT.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		// Set scheduling rule to workspace because we may have to re-create
		// projects using BranchProjectTracker.
		if (restore)
			job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent cevent) {
				show(bop.getResult());
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
				@Override
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

		if (shouldCancelBecauseOfRunningLaunches(monitor))
			return;

		askForTargetIfNecessary();
		if (target == null)
			return;

		BranchOperation bop = new BranchOperation(repository, target);
		bop.execute(monitor);

		show(bop.getResult());
	}

	private void askForTargetIfNecessary() {
		if (target == null)
			target = getTargetWithDialog();
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

	private String getTargetWithDialog() {
		final String[] dialogResult = new String[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				dialogResult[0] = getTargetWithDialogInUI();
			}
		});
		return dialogResult[0];
	}

	private String getTargetWithDialogInUI() {
		AbstractBranchSelectionDialog dialog;
		switch (mode) {
		case MODE_CHECKOUT:
			dialog = new CheckoutDialog(getShell(), repository);
			break;
		case MODE_CREATE:
			CreateBranchWizard wiz;
			try {
				if (base == null)
					base = repository.getFullBranch();
				wiz = new CreateBranchWizard(repository, base);
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
	public void show(final @NonNull CheckoutResult result) {
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
						BranchOperationUI op = BranchOperationUI
								.checkoutWithoutQuestion(repository, target);
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
					MessageDialogWithToggle.openInformation(PlatformUI
							.getWorkbench().getActiveWorkbenchWindow()
							.getShell(),
							UIText.BranchOperationUI_DetachedHeadTitle,
							UIText.BranchOperationUI_DetachedHeadMessage,
							toggleMessage, false, store,
							UIPreferences.SHOW_DETACHED_HEAD_WARNING);
				}
			}
		});
	}

	private boolean shouldCancelBecauseOfRunningLaunches(
			IProgressMonitor monitor) {
		if (mode == MODE_CHECKOUT)
			return false;
		if (!showQuestionsBeforeCheckout)
			return false;
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		if (!store
				.getBoolean(UIPreferences.SHOW_RUNNING_LAUNCH_ON_CHECKOUT_WARNING))
			return false;

		final ILaunchConfiguration launchConfiguration = getRunningLaunchConfiguration(monitor);
		if (launchConfiguration != null) {
			final boolean[] dialogResult = new boolean[1];
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					dialogResult[0] = showContinueDialogInUI(store,
							launchConfiguration);
				}
			});
			return dialogResult[0];
		}
		return false;
	}

	private boolean showContinueDialogInUI(final IPreferenceStore store,
			final ILaunchConfiguration launchConfiguration) {
		String[] buttons = new String[] { UIText.BranchOperationUI_Continue,
				IDialogConstants.CANCEL_LABEL };
		String message = NLS.bind(
				UIText.BranchOperationUI_RunningLaunchMessage,
				launchConfiguration.getName());
		MessageDialogWithToggle continueDialog = new MessageDialogWithToggle(
				getShell(), UIText.BranchOperationUI_RunningLaunchTitle, null,
				message, MessageDialog.NONE, buttons, 0,
				UIText.BranchOperationUI_RunningLaunchDontShowAgain, false);
		int result = continueDialog.open();
		// cancel
		if (result == IDialogConstants.CANCEL_ID || result == SWT.DEFAULT)
			return true;
		boolean dontWarnAgain = continueDialog.getToggleState();
		if (dontWarnAgain)
			store.setValue(
					UIPreferences.SHOW_RUNNING_LAUNCH_ON_CHECKOUT_WARNING,
					false);
		return false;
	}

	private ILaunchConfiguration getRunningLaunchConfiguration(
			IProgressMonitor monitor) {
		final ILaunchConfiguration[] lc = new ILaunchConfiguration[1];
		try {
			ModalContext.run(new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor m)
						throws InvocationTargetException, InterruptedException {

					Set<IProject> projects = new HashSet<>(Arrays
							.asList(ProjectUtil.getProjects(repository)));

					ILaunchManager launchManager = DebugPlugin.getDefault()
							.getLaunchManager();
					ILaunch[] launches = launchManager.getLaunches();
					m.beginTask(
							UIText.BranchOperationUI_SearchLaunchConfiguration,
							launches.length);
					for (ILaunch launch : launches) {
						m.worked(1);
						if (launch.isTerminated())
							continue;
						ISourceLocator locator = launch.getSourceLocator();
						if (locator instanceof ISourceLookupDirector) {
							ISourceLookupDirector director = (ISourceLookupDirector) locator;
							ISourceContainer[] containers = director
									.getSourceContainers();
							if (isAnyProjectInSourceContainers(containers,
									projects)) {
								lc[0] = launch.getLaunchConfiguration();
								return;
							}
						}
					}
				}
			}, true, monitor, Display.getDefault());
		} catch (InvocationTargetException e) {
			// ignore
		} catch (InterruptedException e) {
			// ignore
		}
		return lc[0];
	}

	private boolean isAnyProjectInSourceContainers(
			ISourceContainer[] containers, Set<IProject> projects) {
		for (ISourceContainer container : containers) {
			if (container instanceof ProjectSourceContainer) {
				ProjectSourceContainer projectContainer = (ProjectSourceContainer) container;
				if (projects.contains(projectContainer.getProject()))
					return true;
			}
			try {
				boolean found = isAnyProjectInSourceContainers(
						container.getSourceContainers(), projects);
				if (found)
					return true;
			} catch (CoreException e) {
				// Ignore the child source containers, continue search
			}
		}
		return false;
	}

}
