/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jgit.api.CheckoutResult;

/**
 * The UI wrapper for {@link BranchOperation}
 */
public class BranchOperationUI {

	private BranchOperation bop;

	private final Repository repository;

	private String refName;

	private ObjectId commitId;

	/**
	 * @param repository
	 * @param refName
	 */
	public BranchOperationUI(Repository repository, String refName) {
		this.repository = repository;
		this.refName = refName;
	}

	/**
	 * @param repository
	 */
	public BranchOperationUI(Repository repository) {
		this.repository = repository;
	}

	/**
	 * @param repository
	 * @param commitId
	 */
	public BranchOperationUI(Repository repository, ObjectId commitId) {
		this.repository = repository;
		this.commitId = commitId;
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
		if (commitId == null && refName == null) {
			BranchSelectionDialog dialog = new BranchSelectionDialog(
					getShell(), repository);
			if (dialog.open() != Window.OK) {
				return;
			}
			refName = dialog.getRefName();
		}

		String jobname;
		String repoName = Activator.getDefault().getRepositoryUtil().getRepositoryName(repository);
		if (refName != null) {
			bop = new BranchOperation(repository, refName);
			jobname = NLS.bind(UIText.BranchAction_checkingOut, repoName, refName);
		} else {
			bop = new BranchOperation(repository, commitId);
			jobname = NLS
					.bind(UIText.BranchAction_checkingOut, repoName, commitId.name());
		}

		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
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
				showResultDialog();
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
		if (refName == null) {
			BranchSelectionDialog dialog = new BranchSelectionDialog(
					getShell(), repository);
			if (dialog.open() != Window.OK) {
				return;
			}
			refName = dialog.getRefName();
		}

		bop = new BranchOperation(repository, refName);
		bop.execute(monitor);
		showResultDialog();
	}

	private void showResultDialog() {
		BranchResultDialog.show(bop.getResult(), repository, refName);
		try {
			if (ObjectId.isId(repository.getFullBranch()) && bop.getResult().getStatus() == CheckoutResult.Status.OK)
				showDetachedHeadWarning();
		} catch (IOException e) {
			// Don't show warning then.
		}
	}

	private void showDetachedHeadWarning() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IPreferenceStore store = Activator.getDefault()
						.getPreferenceStore();

				if (store.getString(UIPreferences.SHOW_DETACHED_HEAD_WARNING)
						.equals(MessageDialogWithToggle.PROMPT)) {
					String toggleMessage = UIText.ConfigurationChecker_doNotShowAgain;
					MessageDialogWithToggle.openInformation(getShell(),
							UIText.BranchOperationUI_DetachedHeadTitle,
							UIText.BranchOperationUI_DetachedHeadMessage,
							toggleMessage, false, store,
							UIPreferences.SHOW_DETACHED_HEAD_WARNING);
				}
			}
		});
	}

	private Shell getShell() {
		return PlatformUI.getWorkbench().getDisplay().getActiveShell();
	}
}
