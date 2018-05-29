/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.CreateTagDialog;
import org.eclipse.egit.ui.internal.push.PushTagsWizard;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;

/**
 * An action for creating tag.
 *
 * @see TagOperation
 */
public class TagActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repo = getRepository(true, event);
		if (repo == null)
			return null;

		if (!repo.getRepositoryState().canCheckout()) {
			MessageDialog.openError(getShell(event),
					UIText.TagAction_cannotCheckout, NLS.bind(
							UIText.TagAction_repositoryState, repo
									.getRepositoryState().getDescription()));
			return null;
		}

		String currentBranchName;
		try {
			currentBranchName = repo.getBranch();
		} catch (IOException e) {
			Activator
					.handleError(UIText.TagAction_cannotGetBranchName, e, true);
			return null;
		}

		CreateTagDialog dialog = new CreateTagDialog(getShell(event),
				currentBranchName, repo);

		if (dialog.open() != IDialogConstants.OK_ID)
			return null;

		final TagBuilder tag = new TagBuilder();
		PersonIdent personIdent = new PersonIdent(repo);
		final String tagName = dialog.getTagName();

		tag.setTag(tagName);
		tag.setTagger(personIdent);
		tag.setMessage(dialog.getTagMessage());

		RevObject tagTarget;
		try {
			tagTarget = getTagTarget(repo, dialog.getTagCommit());
		} catch (IOException e1) {
			Activator.handleError(UIText.TagAction_unableToResolveHeadObjectId,
					e1, true);
			return null;
		}
		tag.setObjectId(tagTarget);

		String tagJobName = NLS.bind(UIText.TagAction_creating, tagName);
		final boolean shouldMoveTag = dialog.shouldOverWriteTag();

		Job tagJob = new Job(tagJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					new TagOperation(repo, tag, shouldMoveTag).execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(
							UIText.TagAction_taggingFailed, e);
				} finally {
					GitLightweightDecorator.refresh();
				}

				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.TAG.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};

		if (dialog.shouldStartPushWizard()) {
			tagJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent jobChangeEvent) {
					if (jobChangeEvent.getResult().isOK())
						PushTagsWizard.openWizardDialog(repo, tagName);
				}
			});
		}

		tagJob.setUser(true);
		tagJob.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		final Repository repo = getRepository();
		return repo != null && containsHead(repo);
	}

	private RevObject getTagTarget(Repository repo, ObjectId objectId)
			throws IOException {
		try (RevWalk rw = new RevWalk(repo)) {
			if (objectId == null)
				return rw.parseAny(repo.resolve(Constants.HEAD));
			else
				return rw.parseAny(objectId);
		}
	}
}
