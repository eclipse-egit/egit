/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import static org.eclipse.egit.gitflow.ui.Activator.error;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.op.FeatureFinishOperation;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.gitflow.ui.internal.dialogs.FinishFeatureDialog;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.rebase.CommitMessageEditorDialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * git flow feature finish
 */
public class FeatureFinishHandler extends AbstractGitFlowHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);
		if (gfRepo == null) {
			return error(UIText.Handlers_noGitflowRepositoryFound);
		}
		final String featureBranch;
		Repository repo = gfRepo.getRepository();
		try {
			featureBranch = repo.getBranch();
		} catch (IOException e) {
			return error(e.getMessage(), e);
		}

		Shell activeShell = HandlerUtil.getActiveShell(event);
		FinishFeatureDialog dialog = new FinishFeatureDialog(activeShell,
				featureBranch);
		if (dialog.open() != Window.OK) {
			return null;
		}
		final boolean squash = dialog.isSquash();
		boolean keepBranch = dialog.isKeepBranch();

		try {
			try {
				if (squash && !UIRepositoryUtils.handleUncommittedFiles(repo, activeShell))
					return null;
			} catch (GitAPIException e) {
				Activator.logError(e.getMessage(), e);
				return null;
			}

			final FeatureFinishOperation operation = new FeatureFinishOperation(
					gfRepo);
			operation.setSquash(squash);
			operation.setKeepBranch(keepBranch);

			JobUtil.scheduleUserWorkspaceJob(operation,
					UIText.FeatureFinishHandler_finishingFeature,
					JobFamilies.GITFLOW_FAMILY, new JobChangeAdapter() {
						@Override
						public void done(IJobChangeEvent jobChangeEvent) {
							if (jobChangeEvent.getResult().isOK()) {
								postMerge(gfRepo, featureBranch, squash,
										operation.getMergeResult());
							}
						}
					});
		} catch (WrongGitFlowStateException | CoreException | IOException
				| OperationCanceledException e) {
			return error(e.getMessage(), e);
		}


		return null;
	}

	private void postMerge(final GitFlowRepository gfRepo,
			final String featureBranch, final boolean squash,
			final MergeResult mergeResult) {
		Display display = Display.getDefault();

		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				Shell activeShell = Display.getDefault().getActiveShell();

				if (squash && mergeResult.getMergedCommits().length > 1) {
					try {
						rewordCommitMessage(activeShell, gfRepo);
					} catch (CoreException | IOException e) {
						throw new RuntimeException(e);
					}
				}

				MergeStatus mergeStatus = mergeResult.getMergeStatus();
				if (MergeStatus.CONFLICTING.equals(mergeStatus)) {
					String develop = gfRepo.getConfig().getDevelop();
					MultiStatus status = createMergeConflictInfo(develop,
							featureBranch, mergeResult);
					ErrorDialog.openError(null, UIText.FeatureFinishHandler_Conflicts,
							null, status);
				}
			}
		});

	}

	private void rewordCommitMessage(Shell activeShell,
			final GitFlowRepository gfRepo) throws CoreException, IOException {
		Repository repository = gfRepo.getRepository();
		CommitHelper commitHelper = new CommitHelper(repository);

		CommitMessageEditorDialog messageEditorDialog = new CommitMessageEditorDialog(
				activeShell, repository.readSquashCommitMsg(),
				UIText.FeatureFinishHandler_rewordSquashedCommitMessage);

		if (Window.OK == messageEditorDialog.open()) {
			String commitMessage = stripCommentLines(messageEditorDialog
					.getCommitMessage());
			CommitOperation commitOperation = new CommitOperation(repository,
					commitHelper.getAuthor(), commitHelper.getCommitter(),
					commitMessage);
			commitOperation.execute(null);
		}
	}

	private static String stripCommentLines(String commitMessage) {
		StringBuilder result = new StringBuilder();
		for (String line : commitMessage.split("\n")) { //$NON-NLS-1$
			if (!line.trim().startsWith("#")) //$NON-NLS-1$
				result.append(line).append("\n"); //$NON-NLS-1$
		}
		if (!commitMessage.endsWith("\n")) //$NON-NLS-1$
			result.deleteCharAt(result.length() - 1);
		return result.toString();
	}
}
