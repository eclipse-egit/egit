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

import static org.eclipse.jface.dialogs.MessageDialog.openQuestion;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.InitOperation;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.gitflow.ui.internal.dialogs.InitDialog;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * git flow feature init
 */
public class InitHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell activeShell = HandlerUtil.getActiveShell(event);
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);
		if (gfRepo == null) {
			return null;
		}


		Repository repository = gfRepo.getRepository();
		if (!gfRepo.hasBranches()) {
			boolean createMaster = openQuestion(
					activeShell,
					UIText.InitHandler_emptyRepository,
					UIText.InitHandler_doYouWantToInitNow);
			if (!createMaster) {
				return null;
			}

			createInitialCommit(repository);
		}

		InitDialog dialog = new InitDialog(activeShell, gfRepo, getBranches(repository));
		if (dialog.open() != Window.OK) {
			return null;
		}

		InitOperation initOperation = new InitOperation(repository,
				dialog.getResult());
		JobUtil.scheduleUserWorkspaceJob(initOperation,
				UIText.InitHandler_initializing, JobFamilies.GITFLOW_FAMILY);

		return null;
	}

	private void createInitialCommit(Repository repository)
			throws ExecutionException {
		CommitHelper commitHelper = new CommitHelper(repository);

		CommitOperation commitOperation;
		try {
			commitOperation = new CommitOperation(repository,
					commitHelper.getAuthor(), commitHelper.getCommitter(),
					UIText.InitHandler_initialCommit);
			commitOperation.execute(null);
		} catch (CoreException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
	}

	private List<Ref> getBranches(Repository repository) throws ExecutionException {
		List<Ref> branchList;
		try {
			branchList = Git.wrap(repository).branchList().call();
		} catch (GitAPIException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		return branchList;
	}
}
