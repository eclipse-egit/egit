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
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;

/**
 * Helper class for preparing a commit in EGit UI
 *
 */
public class CommitHelper {

	private Repository repository;

	boolean canCommit;

	String cannotCommitMessage;

	private RevCommit previousCommit;

	String author;

	String committer;

	boolean isMergedResolved;

	boolean isCherryPickResolved;

	private String previousCommitMessage;

	private String previousAuthor;

	private String commitMessage;

	/**
	 * @param repository
	 */
	public CommitHelper(Repository repository) {
		this.repository = repository;
		calculateCommitInfo();
	}

	private void calculateCommitInfo() {
		Repository mergeRepository = null;
		isMergedResolved = false;
		isCherryPickResolved = false;
		RepositoryState state = repository.getRepositoryState();
		canCommit = state.canCommit();
		if (!canCommit) {
			cannotCommitMessage = NLS.bind(UIText.CommitAction_repositoryState,
					state.getDescription());
			return;
		}
		if (state.equals(RepositoryState.MERGING_RESOLVED)) {
			isMergedResolved = true;
			mergeRepository = repository;
		} else if (state.equals(RepositoryState.CHERRY_PICKING_RESOLVED)) {
			isCherryPickResolved = true;
			mergeRepository = repository;
		}
		loadPreviousCommit();
		final UserConfig config = repository.getConfig().get(UserConfig.KEY);
		author = config.getAuthorName();
		final String authorEmail = config.getAuthorEmail();
		author = author + " <" + authorEmail + ">"; //$NON-NLS-1$ //$NON-NLS-2$

		committer = config.getCommitterName();
		final String committerEmail = config.getCommitterEmail();
		committer = committer + " <" + committerEmail + ">"; //$NON-NLS-1$ //$NON-NLS-2$

		if (previousCommit != null) {
			previousCommitMessage = previousCommit.getFullMessage();
			PersonIdent previousAuthorIdent = previousCommit.getAuthorIdent();
			previousAuthor = previousAuthorIdent.getName()
					+ " <" + previousAuthorIdent.getEmailAddress() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (isMergedResolved || isCherryPickResolved) {
			commitMessage = getMergeResolveMessage(mergeRepository);
		}

		if (isCherryPickResolved) {
			author = getCherryPickOriginalAuthor(mergeRepository);
		}
	}

	private void loadPreviousCommit() {
		try {
			ObjectId parentId = repository.resolve(Constants.HEAD);
			if (parentId != null)
				previousCommit = new RevWalk(repository).parseCommit(parentId);
		} catch (IOException e) {
			Activator.handleError(UIText.CommitAction_errorRetrievingCommit, e,
					true);
		}
	}

	private String getMergeResolveMessage(Repository mergeRepository) {
		File mergeMsg = new File(mergeRepository.getDirectory(),
				Constants.MERGE_MSG);
		FileReader reader;
		try {
			reader = new FileReader(mergeMsg);
			BufferedReader br = new BufferedReader(reader);
			try {
				StringBuilder message = new StringBuilder();
				String s;
				String newLine = newLine();
				while ((s = br.readLine()) != null) {
					message.append(s).append(newLine);
				}
				return message.toString();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					// Empty
				}
			}
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String getCherryPickOriginalAuthor(Repository mergeRepository) {
		try {
			ObjectId cherryPickHead = mergeRepository.readCherryPickHead();
			PersonIdent author = new RevWalk(mergeRepository).parseCommit(
					cherryPickHead).getAuthorIdent();
			return author.getName() + " <" + author.getEmailAddress() + ">"; //$NON-NLS-1$//$NON-NLS-2$
		} catch (IOException e) {
			Activator.handleError(UIText.CommitAction_errorRetrievingCommit, e,
					true);
			throw new IllegalStateException(e);
		}
	}

	private String newLine() {
		return System.getProperty("line.separator"); //$NON-NLS-1$
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
	 * @return previous commit message
	 */
	public String getPreviousCommitMessage() {
		return previousCommitMessage;
	}

	/**
	 * @return previous author
	 */
	public String getPreviousAuthor() {
		return previousAuthor;
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

}
