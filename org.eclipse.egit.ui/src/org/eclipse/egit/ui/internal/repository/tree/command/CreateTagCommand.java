/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dariusz Luksza <dariusz@luksza.org> - initial implementation
 *    Mathias Kinzler (SAP AG) - move to command framework
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

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
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
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
 * Implements "Create Tag"
 */
public class CreateTagCommand extends RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode node = getSelectedNodes(event).get(0);

		final Repository repo = node.getRepository();

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
			Activator.handleError(e.getMessage(), e, true);
			// TODO correct message
			throw new ExecutionException(e.getMessage(), e);
		}

		CreateTagDialog dialog = new CreateTagDialog(getView(event).getSite()
				.getShell(), ValidationUtils.getRefNameInputValidator(repo,
				Constants.R_TAGS), currentBranchName);

		// get and set commits
		RevWalk revCommits = getRevCommits(repo);
		dialog.setRevCommitList(revCommits);

		// get and set existing tags
		List<Tag> tags = getRevTags(repo);
		dialog.setExistingTags(tags);

		if (dialog.open() != IDialogConstants.OK_ID)
			return null;

		final Tag tag = new Tag(repo);
		PersonIdent personIdent = new PersonIdent(repo);
		String tagName = dialog.getTagName();

		tag.setTag(tagName);
		tag.setTagger(personIdent);
		tag.setMessage(dialog.getTagMessage());

		ObjectId tagCommit = getTagCommit(dialog.getTagCommit(), repo);
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

	private List<Tag> getRevTags(Repository repo) throws ExecutionException {
		Collection<Ref> revTags = repo.getTags().values();
		List<Tag> tags = new ArrayList<Tag>();
		for (Ref ref : revTags) {
			try {
				Tag tag = repo.mapTag(ref.getName());
				tags.add(tag);
			} catch (IOException e) {
				throw new ExecutionException(NLS
						.bind(UIText.TagAction_errorWhileMappingRevTag, ref
								.getName()), e);
			}
		}
		return tags;
	}

	private RevWalk getRevCommits(Repository repo) throws ExecutionException {
		RevWalk revWalk = new RevWalk(repo);
		try {
			revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
			revWalk.sort(RevSort.BOUNDARY, true);
			AnyObjectId headId = repo.resolve(Constants.HEAD);
			if (headId != null)
				revWalk.markStart(revWalk.parseCommit(headId));

		} catch (IOException e) {
			throw new ExecutionException(
					UIText.TagAction_errorWhileGettingRevCommits, e);
		}
		return revWalk;
	}

	private ObjectId getTagCommit(ObjectId objectId, Repository repo)
			throws ExecutionException {
		ObjectId result = null;
		if (objectId == null) {
			try {
				result = repo.resolve(Constants.HEAD);
			} catch (IOException e) {
				throw new ExecutionException(
						UIText.TagAction_unableToResolveHeadObjectId, e);
			}
		} else {
			result = objectId;
		}
		return result;
	}
}
