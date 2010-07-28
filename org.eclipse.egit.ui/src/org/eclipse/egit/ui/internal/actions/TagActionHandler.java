/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.CreateTagDialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;

/**
 * An action for creating tag.
 *
 * @see TagOperation
 */
public class TagActionHandler extends RepositoryActionHandler {

	private Repository repo;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		repo = getRepository(true, event);
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
				ValidationUtils
						.getRefNameInputValidator(repo, Constants.R_TAGS),
				currentBranchName);

		// get and set commits
		RevWalk revCommits = getRevCommits(event);
		dialog.setRevCommitList(revCommits);

		// get and set existing tags
		List<Tag> tags = getRevTags(event);
		dialog.setExistingTags(tags);

		if (dialog.open() != IDialogConstants.OK_ID)
			return null;

		final Tag tag = new Tag(repo);
		PersonIdent personIdent = new PersonIdent(repo);
		String tagName = dialog.getTagName();

		tag.setTag(tagName);
		tag.setTagger(personIdent);
		tag.setMessage(dialog.getTagMessage());

		ObjectId tagCommit;
		try {
			tagCommit = getTagCommit(dialog.getTagCommit());
		} catch (IOException e1) {
			Activator.handleError(UIText.TagAction_unableToResolveHeadObjectId,
					e1, true);
			return null;
		}
		tag.setObjId(tagCommit);

		String tagJobName = NLS.bind(UIText.TagAction_creating, tagName);
		final boolean shouldMoveTag = dialog.shouldOverWriteTag();

		Job tagJob = new Job(tagJobName) {
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

		};

		tagJob.setUser(true);
		tagJob.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		try {
			return getRepository(false, null) != null;
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, false);
			return false;
		}
	}

	private List<Tag> getRevTags(ExecutionEvent event)
			throws ExecutionException {
		Collection<Ref> revTags = repo.getTags().values();
		List<Tag> tags = new ArrayList<Tag>();
		RevWalk walk = new RevWalk(repo);
		for (Ref ref : revTags) {
			try {
				Tag tag = walk.parseTag(repo.resolve(ref.getName())).asTag(walk);
				tags.add(tag);
			} catch (IOException e) {
				ErrorDialog.openError(getShell(event),
						UIText.TagAction_errorDuringTagging, NLS.bind(
								UIText.TagAction_errorWhileMappingRevTag, ref
										.getName()), new Status(IStatus.ERROR,
								Activator.getPluginId(), e.getMessage(), e));
			}
		}
		return tags;
	}

	private RevWalk getRevCommits(ExecutionEvent event)
			throws ExecutionException {
		RevWalk revWalk = new RevWalk(repo);
		revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
		revWalk.sort(RevSort.BOUNDARY, true);

		try {
			AnyObjectId headId = repo.resolve(Constants.HEAD);
			if (headId != null)
				revWalk.markStart(revWalk.parseCommit(headId));
		} catch (IOException e) {
			ErrorDialog.openError(getShell(event),
					UIText.TagAction_errorDuringTagging,
					UIText.TagAction_errorWhileGettingRevCommits, new Status(
							IStatus.ERROR, Activator.getPluginId(), e
									.getMessage(), e));
		}

		return revWalk;
	}

	private ObjectId getTagCommit(ObjectId objectId) throws IOException {
		ObjectId result = null;
		if (objectId == null) {
			result = repo.resolve(Constants.HEAD);

		} else {
			result = objectId;
		}
		return result;
	}
}
