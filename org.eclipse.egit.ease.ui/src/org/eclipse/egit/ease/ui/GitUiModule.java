/*******************************************************************************
 * Copyright (C) 2016, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ease.ui;

import static org.eclipse.ease.modules.ScriptParameter.NULL;
import static org.eclipse.egit.ease.ui.PluginConstants.PLUGIN_ID;
import static org.eclipse.egit.ease.ui.internal.UIText.failedToCheckoutBranch;
import static org.eclipse.egit.ease.ui.internal.UIText.merging;
import static org.eclipse.egit.ease.ui.internal.UIText.rebasing;
import static org.eclipse.egit.ease.ui.internal.UIText.rebasingOn;
import static org.eclipse.jgit.api.ResetCommand.ResetType.HARD;
import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ease.Logger;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.ease.ui.internal.UIText;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Display;

/**
 * EASE Module for scripting basic EGit operations.
 */
public class GitUiModule extends AbstractScriptModule {

	/**
	 * Checkout the branch with the given name.
	 *
	 * @param repository
	 *            The JGit repository to work with.
	 * @param branchName
	 *            The (possibly abbreviated) name of the branch to checkout.
	 */
	@WrapToScript
	public void checkoutBranch(@ScriptParameter(defaultValue = NULL) Object repository, @ScriptParameter(defaultValue = NULL) final String branchName) {
		try {
			final Ref branchRef = ((Repository) repository).findRef(branchName);
			BranchOperationUI.checkout((Repository) repository, branchRef.getName()).start();
		} catch (final IOException e) {
			Logger.error(PLUGIN_ID, failedToCheckoutBranch + branchName, e);
		}
	}

	/**
	 * Get the JGit repository for the first selected repository node.
	 *
	 * @param selection
	 *            <code>IStructuredSelection</code> containing a <code>RepositoryNode</code> as its first element.
	 * @return The JGit repository.
	 */
	@WrapToScript
	public static Repository getRepository(@ScriptParameter(defaultValue = NULL) ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			throw new IllegalArgumentException();
		}
		final RepositoryNode repositoryNode = (RepositoryNode) ((IStructuredSelection) selection).getFirstElement();
		return repositoryNode.getObject();
	}

	/**
	 * Opens a dialog, allowing to commit, stash, or reset uncommitted changes.
	 *
	 * @param repository
	 *            The JGit repository to work with.
	 */
	@WrapToScript
	public void handleUncommittedFiles(@ScriptParameter(defaultValue = NULL) final Object repository) {
		if (!(repository instanceof Repository)) {
			throw new IllegalArgumentException();
		}
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					UIRepositoryUtils.handleUncommittedFiles((Repository) repository, Display.getDefault().getActiveShell());
				} catch (final GitAPIException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	/**
	 * Rebase the current branch onto the <code>HEAD</code> of the specified branch.
	 *
	 * @param repository
	 *            The JGit repository to work with.
	 * @param branchName
	 *            The (possibly abbreviated) name of the branch to rebase on.
	 */
	@WrapToScript
	public void rebaseOn(@ScriptParameter(defaultValue = NULL) final Object repository, @ScriptParameter(defaultValue = NULL) final String branchName) {
		if (!(repository instanceof Repository)) {
			throw new IllegalArgumentException();
		}

		final Job job = new Job(rebasingOn + branchName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final Ref branchRef = ((Repository) repository).findRef(branchName);
					final RebaseOperation op = new RebaseOperation((Repository) repository, branchRef);
					op.execute(monitor);
					if (op.getResult().getStatus().isSuccessful()) {
						return Status.OK_STATUS;
					}
				} catch (final IOException e) {
					return createErrorStatus(e);
				} catch (final CoreException e) {
					return createErrorStatus(e);
				}
				return createErrorStatus(UIText.rebaseFailed);
			}

		};
		job.setUser(true);
		job.schedule();
	}

	/**
	 * Merges the given branch into the current branch.
	 *
	 * @param repository
	 *            The git repository to work with.
	 * @param branchName
	 *            The (possibly abbreviated) name of the branch to merge.
	 *
	 */
	@WrapToScript
	public void mergeFrom(@ScriptParameter(defaultValue = NULL) final Object repository, @ScriptParameter(defaultValue = NULL) final String branchName) {
		if (!(repository instanceof Repository)) {
			throw new IllegalArgumentException();
		}

		final Job job = new Job(merging + branchName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final Ref branchRef = ((Repository) repository).findRef(branchName);
					final MergeOperation op = new MergeOperation((Repository) repository, branchRef.getName());
					op.setSquash(false);
					op.setFastForwardMode(FastForwardMode.FF);
					op.execute(monitor);
				} catch (final CoreException e) {
					return e.getStatus();
				} catch (final IOException e) {
					return createErrorStatus(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	/**
	 * Resets the current branch hard by the given number of commits.
	 *
	 * @param repository
	 *            The git repository to work with.
	 * @param commits
	 *            The number of commits to go back from HEAD.
	 */
	@WrapToScript
	public void resetCommits(@ScriptParameter(defaultValue = NULL) final Object repository, @ScriptParameter(defaultValue = NULL) final Integer commits) {
		if (commits == null) {
			throw new IllegalArgumentException();
		}

		final Job job = new Job(rebasing + commits + commits) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final ResetOperation resetOperation = new ResetOperation((Repository) repository, HEAD + "~" + commits.toString(), HARD);
					resetOperation.execute(monitor);
				} catch (final CoreException e) {
					return createErrorStatus(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private IStatus createErrorStatus(final Exception e) {
		return new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e);
	}

	private IStatus createErrorStatus(String message) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message);
	}
}
