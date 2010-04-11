/*******************************************************************************
 * Copyright (C) 2010, Darusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.SWTUtils;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.CreateTagDialog;
import org.eclipse.jface.action.IAction;
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
import org.eclipse.team.internal.ui.Utils;

/**
 * An action for creating tag.
 *
 * @see TagOperation
 */
public class TagAction extends RepositoryAction {

	private Repository repo;

	@Override
	public boolean isEnabled() {
		return getRepository(false) != null;
	}

	@Override
	protected void execute(IAction action) throws InvocationTargetException,
			InterruptedException {
		repo = getRepository(true);
		if (repo == null)
			return;

		if (!repo.getRepositoryState().canCheckout()) {
			MessageDialog.openError(getShell(),
					UIText.TagAction_cannotCheckout, NLS.bind(
							UIText.TagAction_repositoryState, repo
									.getRepositoryState().getDescription()));
			return;
		}

		String currentBranchName;
		try {
			currentBranchName = repo.getBranch();
		} catch (IOException e) {
			throw new InvocationTargetException(e, UIText.TagAction_cannotGetBranchName);
		}

		CreateTagDialog dialog = new CreateTagDialog(getShell(), SWTUtils
				.getRefNameInputValidator(repo, Constants.R_TAGS), currentBranchName);

		// get and set commits
		RevWalk revCommits = getRevCommits();
		dialog.setRevCommitList(revCommits);

		// get and set existing tags
		List<Tag> tags = getRevTags();
		dialog.setExistingTags(tags);

		if (dialog.open() != IDialogConstants.OK_ID)
			return;

		final Tag tag = new Tag(repo);
		PersonIdent personIdent = new PersonIdent(repo);
		String tagName = dialog.getTagName();

		tag.setTag(tagName);
		tag.setTagger(personIdent);
		tag.setMessage(dialog.getTagMessage());

		ObjectId tagCommit = getTagCommit(dialog.getTagCommit());
		tag.setObjId(tagCommit);

		String tagJobName = NLS.bind(UIText.TagAction_creating, tagName);
		final boolean shouldMoveTag = dialog.shouldOverWriteTag();

		Job tagJob = new Job(tagJobName) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					new TagOperation(repo, tag, shouldMoveTag).execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(UIText.TagAction_taggingFailed, e);
				} finally {
					GitLightweightDecorator.refresh();
				}

				return Status.OK_STATUS;
			}

		};

		tagJob.setUser(true);
		tagJob.schedule();
	}

	private List<Tag> getRevTags() {
		Collection<Ref> revTags = repo.getTags().values();
		List<Tag> tags = new ArrayList<Tag>();
		for (Ref ref : revTags) {
			try {
				Tag tag = repo.mapTag(ref.getName());
				tags.add(tag);
			} catch (IOException e) {
				Utils.handleError(getTargetPart().getSite().getShell(), e,
						UIText.TagAction_errorDuringTagging,
						NLS.bind(UIText.TagAction_errorWhileMappingRevTag, ref.getName()));
			}
		}
		return tags;
	}

	private RevWalk getRevCommits() {
		RevWalk revWalk = new RevWalk(repo);
		revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
		revWalk.sort(RevSort.BOUNDARY, true);
		AnyObjectId headId = getHeadId();

		try {
			revWalk.markStart(revWalk.parseCommit(headId));
		} catch (IOException e) {
			Utils.handleError(getTargetPart().getSite().getShell(), e,
					UIText.TagAction_errorDuringTagging,
					UIText.TagAction_errorWhileGettingRevCommits);
		}

		return revWalk;
	}

	private AnyObjectId getHeadId() {
		AnyObjectId headId;
		try {
			headId = repo.resolve(Constants.HEAD);
		} catch (IOException e1) {
			headId = null;
		}
		return headId;
	}

	private ObjectId getTagCommit(ObjectId objectId) throws InvocationTargetException {
		ObjectId result = null;
			if (null == objectId) {
				try {
					result = repo.resolve(Constants.HEAD);
				} catch (IOException e) {
					throw new InvocationTargetException(e, UIText.TagAction_unableToResolveHeadObjectId);
				}
			} else {
				result = objectId;
			}
		return result;
	}
}
