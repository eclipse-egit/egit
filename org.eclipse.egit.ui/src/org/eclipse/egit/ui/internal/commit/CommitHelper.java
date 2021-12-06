/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Jing Xue <jingxue@digizenstudio.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Text;

/**
 * Helper class for preparing a commit in EGit UI
 */
public class CommitHelper {

	private final Repository repository;

	private final boolean canCommit;

	private final String cannotCommitMessage;

	private final RevCommit previousCommit;

	private final String author;

	private final String committer;

	private final boolean isMergedResolved;

	private final boolean isCherryPickResolved;

	private final String commitMessage;

	private String commitTemplate;

	/**
	 * @param repository
	 */
	public CommitHelper(Repository repository) {
		this.repository = repository;

		RepositoryState state = repository.getRepositoryState();
		canCommit = state.canCommit();
		if (!canCommit) {
			cannotCommitMessage = NLS.bind(UIText.CommitAction_repositoryState,
					state.getDescription());
			author = null;
			committer = null;
			previousCommit = null;
			commitMessage = null;
			isMergedResolved = false;
			isCherryPickResolved = false;
			return;
		} else {
			cannotCommitMessage = null;
		}

		Repository mergeRepository = null;
		switch (state) {
		case MERGING_RESOLVED:
			isMergedResolved = true;
			isCherryPickResolved = false;
			commitMessage = getMergeResolveMessage(mergeRepository);
			mergeRepository = repository;
			break;
		case CHERRY_PICKING_RESOLVED:
			isMergedResolved = false;
			isCherryPickResolved = true;
			commitMessage = getMergeResolveMessage(mergeRepository);
			mergeRepository = repository;
			break;
		default:
			isMergedResolved = false;
			isCherryPickResolved = false;
			commitMessage = null;
		}

		previousCommit = getHeadCommit(repository);

		UserConfig config = repository.getConfig().get(UserConfig.KEY);
		if (isCherryPickResolved) {
			author = getCherryPickOriginalAuthor(mergeRepository);
		} else {
			author = formatUser(config.getAuthorName(),
					config.getAuthorEmail());
		}
		committer = formatUser(config.getCommitterName(),
				config.getCommitterEmail());
	}

	private static String formatUser(String name, String email) {
		return name + " <" + email + ">"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @return true if there is no commit message but a commit template
	 */
	public boolean shouldUseCommitTemplate() {
		return StringUtils.isEmptyOrNull(getCommitMessage())
				&& getCommitTemplate() != null;
	}

	/**
	 * @return commit message template
	 */
	public String getCommitTemplate() {
		if (commitTemplate == null && repository != null) {
			CommitConfig commitConfig = repository.getConfig()
					.get(CommitConfig.KEY);
			try {
				commitTemplate = commitConfig
						.getCommitTemplateContent(repository);
			} catch (IOException | ConfigInvalidException e) {
				Activator.handleError(UIText.CommitAction_CommitTemplateFailed,
						e, true);
			}
		}

		return commitTemplate;
	}

	private static RevCommit getHeadCommit(Repository repository) {
		if (repository == null) {
			return null;
		}
		RevCommit headCommit = null;
		try (RevWalk rw = new RevWalk(repository)) {
			ObjectId parentId = repository.resolve(Constants.HEAD);
			if (parentId != null)
				headCommit = rw.parseCommit(parentId);
		} catch (IOException e) {
			Activator.handleError(UIText.CommitAction_errorRetrievingCommit, e,
					true);
		}
		return headCommit;
	}

	private String getMergeResolveMessage(Repository mergeRepository) {
		try {
			String message = mergeRepository.readMergeCommitMsg();
			if (message != null)
				return message;
		} catch (IOException e) {
			// Return "Could not find ..." below
		}
		return NLS.bind(UIText.CommitHelper_couldNotFindMergeMsg,
				Constants.MERGE_MSG);
	}

	private static String getCherryPickOriginalAuthor(Repository mergeRepository) {
		try (RevWalk rw = new RevWalk(mergeRepository)) {
			ObjectId cherryPickHead = mergeRepository.readCherryPickHead();
			PersonIdent author = rw.parseCommit(
					cherryPickHead).getAuthorIdent();
			return author.getName() + " <" + author.getEmailAddress() + ">"; //$NON-NLS-1$//$NON-NLS-2$
		} catch (IOException e) {
			Activator.handleError(UIText.CommitAction_errorRetrievingCommit, e,
					true);
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @return true if committing is possible
	 */
	public boolean canCommit() {
		return canCommit;
	}

	/**
	 * @return error message if committing is not possible
	 */
	public String getCannotCommitMessage() {
		return cannotCommitMessage;
	}

	/**
	 * @return author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @return committer
	 */
	public String getCommitter() {
		return committer;
	}

	/**
	 * @return commit message
	 */
	public String getCommitMessage() {
		return commitMessage;
	}

	/**
	 * @return isMergedResolved
	 */
	public boolean isMergedResolved() {
		return isMergedResolved;
	}

	/**
	 * @return isCherryPickResolved
	 */
	public boolean isCherryPickResolved() {
		return isCherryPickResolved;
	}

	/**
	 * @return previous commit
	 */
	public RevCommit getPreviousCommit() {
		return previousCommit;
	}

	/**
	 * @return true if amending is allowed
	 */
	public boolean amendAllowed() {
		return previousCommit != null && repository.getRepositoryState().canAmend();
	}

	/**
	 * @param repository
	 *            to check
	 * @return true if an empty commit without files is allowed in the
	 *         current state
	 */
	public static boolean isCommitWithoutFilesAllowed(Repository repository) {
		if (repository == null) {
			return false;
		}
		RepositoryState state = repository.getRepositoryState();
		return state == RepositoryState.MERGING_RESOLVED;
	}

	/**
	 * @param repository
	 * @return info related to the HEAD commit
	 */
	public static CommitInfo getHeadCommitInfo(Repository repository) {
		if (repository == null) {
			return null;
		}
		RevCommit headCommit = getHeadCommit(repository);
		if (headCommit == null) {
			return null;
		}
		String commitMessage = headCommit.getFullMessage().replaceAll(
				"\n", Text.DELIMITER); //$NON-NLS-1$
		PersonIdent authorIdent = headCommit.getAuthorIdent();
		String author = authorIdent.getName()
				+ " <" + authorIdent.getEmailAddress() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		PersonIdent committerIdent = headCommit.getCommitterIdent();
		String committer = committerIdent.getName()
				+ " <" + committerIdent.getEmailAddress() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		return new CommitInfo(headCommit, author, committer, commitMessage);
	}

	/**
	 * Commit Info
	 *
	 */
	public static class CommitInfo {
		private RevCommit commit;
		private String author;
		private String committer;
		private String commitMessage;

		/**
		 * @param commit
		 * @param author
		 * @param committer
		 * @param commitMessage
		 */
		public CommitInfo(RevCommit commit, String author, String committer, String commitMessage) {
			super();
			this.commit = commit;
			this.author = author;
			this.committer = committer;
			this.commitMessage = commitMessage;
		}

		/**
		 * @return commit
		 */
		public RevCommit getCommit() {
			return commit;
		}

		/**
		 * @return author
		 */
		public String getAuthor() {
			return author;
		}

		/**
		 * @return committer
		 */
		public String getCommitter() {
			return committer;
		}

		/**
		 * @return commit message
		 */
		public String getCommitMessage() {
			return commitMessage;
		}
	}

}
