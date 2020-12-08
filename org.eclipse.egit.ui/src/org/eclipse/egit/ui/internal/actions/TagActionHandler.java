/*******************************************************************************
 * Copyright (C) 2010, 2020 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.egit.ui.internal.dialogs.CreateTagDialog;
import org.eclipse.egit.ui.internal.push.PushTagsWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * An action for creating a tag.
 */
public class TagActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IRepositoryCommit commit = getCommit(getSelection(event));
		CreateTagDialog dialog;
		Repository repo;
		if (commit == null) {
			repo = getRepository(true, event);
			if (repo == null) {
				return null;
			}
			if (!repo.getRepositoryState().canCheckout()) {
				MessageDialog.openError(getShell(event),
						UIText.TagAction_cannotCheckout,
						MessageFormat.format(UIText.TagAction_repositoryState,
								repo.getRepositoryState().getDescription()));
				return null;
			}

			String currentBranchName;
			try {
				currentBranchName = repo.getBranch();
			} catch (IOException e) {
				Activator.handleError(UIText.TagAction_cannotGetBranchName, e,
						true);
				return null;
			}

			dialog = new CreateTagDialog(getShell(event), currentBranchName,
					repo);
		} else {
			repo = commit.getRepository();
			if (repo == null) {
				return null;
			}
			dialog = new CreateTagDialog(getShell(event),
					commit.getRevCommit().getId(), repo);
		}
		if (dialog.open() != Window.OK) {
			return null;
		}

		RevCommit tagTarget;
		if (commit == null) {
			try {
				tagTarget = getTagTarget(repo, dialog.getTagCommit());
			} catch (IOException e) {
				Activator.handleError(
						UIText.TagAction_cannotGetCommit, e, true);
				return null;
			}
		} else {
			tagTarget = commit.getRevCommit();
		}
		String tagName = dialog.getTagName();

		assert tagName != null;
		assert tagTarget != null;

		TagOperation operation = new TagOperation(repo)
				.setName(tagName)
				.setTarget(tagTarget)
				.setAnnotated(dialog.isAnnotated())
				.setForce(dialog.shouldOverWriteTag())
				.setSign(dialog.shouldSign())
				.setMessage(dialog.getTagMessage())
				.setCredentialsProvider(new EGitCredentialsProvider());

		IJobChangeListener jobCompleted = null;
		if (dialog.shouldStartPushWizard()) {
			jobCompleted = new JobChangeAdapter() {

				@Override
				public void done(IJobChangeEvent jobEvent) {
					if (jobEvent.getResult().isOK()) {
						PushTagsWizard.openWizardDialog(repo, tagName);
					}
				}
			};
		}
		String tagJobName = MessageFormat.format(UIText.TagAction_creating,
				tagName);
		JobUtil.scheduleUserJob(operation, tagJobName, JobFamilies.TAG,
				jobCompleted);
		return null;
	}

	private RevCommit getTagTarget(Repository repo, ObjectId objectId)
			throws IOException {
		try (RevWalk rw = new RevWalk(repo)) {
			if (objectId == null) {
				return rw.parseCommit(repo.resolve(Constants.HEAD));
			}
			return rw.parseCommit(objectId);
		}
	}

	private IRepositoryCommit getCommit(IStructuredSelection selection) {
		if (selection != null && selection.size() == 1) {
			Object obj = selection.getFirstElement();
			return Adapters.adapt(obj, IRepositoryCommit.class);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		// We manage activeWhen via plugin.xml
		return true;
	}
}
