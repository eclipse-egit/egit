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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.op.TagOperation;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.CreateTagDialog;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
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
				.getShell(), currentBranchName, repo);

		if (dialog.open() != IDialogConstants.OK_ID)
			return null;

		final TagBuilder tag = new TagBuilder();
		PersonIdent personIdent = new PersonIdent(repo);
		String tagName = dialog.getTagName();

		tag.setTag(tagName);
		tag.setTagger(personIdent);
		tag.setMessage(dialog.getTagMessage());
		tag.setObjectId(getTagTarget(dialog.getTagCommit(), repo));

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

			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.TAG))
					return true;
				return super.belongsTo(family);
			}
		};

		tagJob.setUser(true);
		tagJob.schedule();

		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		enableWhenRepositoryHaveHead(evaluationContext);
	}

	private RevObject getTagTarget(ObjectId objectId, Repository repo)
			throws ExecutionException {
		try {
			RevWalk rw = new RevWalk(repo);
			try {
				if (objectId == null) {
					return rw.parseAny(repo.resolve(Constants.HEAD));

				} else {
					return rw.parseAny(objectId);
				}
			} finally {
				rw.release();
			}
		} catch (IOException e) {
			throw new ExecutionException(
					UIText.TagAction_unableToResolveHeadObjectId, e);
		}
	}
}
