/*******************************************************************************
 * Copyright (c) 2012, 2020 Red Hat, Inc, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Andre Dietisheim - initial API and implementation
 *    Mickael Istria - 441231 Use simple push wizard
 *******************************************************************************/

package org.eclipse.egit.ui.internal.commit;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponentStateManager;
import org.eclipse.egit.ui.internal.push.PushMode;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Executes a given commit operation on a given repository. Will also eventually
 * show the commit that was performed and push the repository upstream.
 */
public class CommitJob extends Job {

	private CommitOperation commitOperation;

	private Repository repository;

	private boolean openCommitEditor;

	private PushMode pushMode;

	/**
	 * @param repository
	 *            the repository to commit to
	 * @param commitOperation
	 *            the commit operation to use
	 *
	 */
	public CommitJob(final Repository repository,
			final CommitOperation commitOperation) {
		super(UIText.CommitAction_CommittingChanges);
		this.repository = repository;
		this.commitOperation = commitOperation;
	}

	/**
	 * Sets this job to open the commit editor after committing.
	 *
	 * @param openCommitEditor
	 * @return this job instance
	 */
	public CommitJob setOpenCommitEditor(boolean openCommitEditor) {
		this.openCommitEditor = openCommitEditor;
		return this;
	}

	/**
	 * Sets this job to push the changes upstream after successfully committing.
	 *
	 * @param pushMode
	 * @return this commit job instance
	 */
	public CommitJob setPushUpstream(final PushMode pushMode) {
		this.pushMode = pushMode;
		return this;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		RevCommit commit = null;
		try {
			commitOperation.execute(monitor);
			commit = commitOperation.getCommit();
			CommitMessageComponentStateManager.deleteState(repository);
		} catch (CoreException e) {
			if (e.getCause() instanceof JGitInternalException) {
				return Activator.createErrorStatus(e.getLocalizedMessage(),
						e.getCause());
			} else if (e.getCause() instanceof AbortedByHookException) {
				showAbortedByHook((AbortedByHookException) e.getCause());
				return Status.CANCEL_STATUS;
			} else if (e.getCause() instanceof CanceledException) {
				return Status.CANCEL_STATUS;
			}

			return Activator
					.createErrorStatus(UIText.CommitAction_CommittingFailed, e);
		} finally {
			GitLightweightDecorator.refresh();
		}

		if (commit != null) {
			if (openCommitEditor) {
				openCommitEditor(commit);
			}
			if (pushMode != null) {
				pushUpstream(commit, pushMode);
			}
		}
		return Status.OK_STATUS;
	}

	private void showAbortedByHook(final AbortedByHookException cause) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			private String createTitle() {
				return MessageFormat.format(UIText.CommitJob_AbortedByHook,
						cause.getHookName());
			}

			@Override
			public void run() {
				MessageDialog.openWarning(PlatformUI.getWorkbench()
						.getDisplay().getActiveShell(), createTitle(),
						cause.getHookStdErr());
			}
		});
	}

	private void openCommitEditor(final RevCommit newCommit) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				CommitEditor.openQuiet(new RepositoryCommit(repository,
						newCommit));
			}
		});
	}

	private void pushUpstream(final RevCommit commit, final PushMode pushTo) {
		final RemoteConfig config = SimpleConfigurePushDialog
				.getConfiguredRemote(repository);

		if (pushTo == PushMode.GERRIT || config == null) {
			final Display display = PlatformUI.getWorkbench().getDisplay();
			display.asyncExec(new Runnable() {

				@Override
				public void run() {
					Wizard pushWizard = getPushWizard(commit, pushTo);
					if (pushWizard != null) {
						WizardDialog wizardDialog = new WizardDialog(
								display.getActiveShell(), pushWizard);
						wizardDialog.setHelpAvailable(true);
						wizardDialog.open();
					}
				}
			});
		} else {
			PushOperationUI op = new PushOperationUI(repository,
					config.getName(), false);
			op.start();
		}
	}

	private Wizard getPushWizard(final RevCommit commit,
			final PushMode pushTo) {
		Repository repo = repository;
		if (pushTo != null && repo != null) {
			try {
				return pushTo.getWizard(repo, commit);
			} catch (IOException e) {
				Activator.handleError(
						NLS.bind(UIText.CommitUI_pushFailedMessage, e), e,
						true);
			}
		}
		return null;
	}

	@Override
	public boolean belongsTo(Object family) {
		if (JobFamilies.COMMIT.equals(family))
			return true;
		return super.belongsTo(family);
	}

}
