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
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * The UI wrapper for {@link BranchOperation}
 */
public class BranchOperationUI {
	private BranchOperation bop;

	private final Repository repository;

	private String target;

	/**
	 * Create an operation for selecting and checking out a branch
	 *
	 * @param repository
	 * @return the {@link BranchOperationUI}
	 */
	public static BranchOperationUI branch(Repository repository) {
		return new BranchOperationUI(repository);
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
		return new BranchOperationUI(repository, target);
	}

	/**
	 * @param repository
	 * @param target
	 */
	private BranchOperationUI(Repository repository, String target) {
		this.repository = repository;
		this.target = target;
	}

	/**
	 * Select and checkout a branch
	 *
	 * @param repository
	 */
	private BranchOperationUI(Repository repository) {
		this.repository = repository;
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
		if (target == null) {
			BranchSelectionDialog dialog = new BranchSelectionDialog(
					getShell(), repository);
			if (dialog.open() != Window.OK) {
				return;
			}
			target = dialog.getRefName();
		}

		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		String jobname = NLS.bind(UIText.BranchAction_checkingOut, repoName,
				target);

		bop = new BranchOperation(repository, target);

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
		if (target == null) {
			BranchSelectionDialog dialog = new BranchSelectionDialog(
					getShell(), repository);
			if (dialog.open() != Window.OK) {
				return;
			}
			target = dialog.getRefName();
		}

		bop = new BranchOperation(repository, target);
		bop.execute(monitor);

		BranchResultDialog.show(bop.getResult(), repository, target);
	}

	private Shell getShell() {
		return PlatformUI.getWorkbench().getDisplay().getActiveShell();
	}
}
