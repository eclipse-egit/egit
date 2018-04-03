/*******************************************************************************
 * Copyright (c) 2014 Maik Schreiber
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Maik Schreiber - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.branch.CleanupUncomittedChangesDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;

/** Utility class for handling repositories in the UI. */
public final class UIRepositoryUtils {
	private UIRepositoryUtils() {
		// nothing to do
	}

	/**
	 * Checks the repository to see if there are uncommitted changes, and
	 * prompts the user to clean them up.
	 *
	 * @param repo
	 *            the repository
	 * @param shell
	 *            the parent shell for opening the dialog
	 * @return true if the git status was clean or it was dirty and the user
	 *         cleaned up the uncommitted changes and the previous action may
	 *         continue
	 * @throws GitAPIException
	 *             if there was an error checking the repository
	 */
	public static boolean handleUncommittedFiles(Repository repo, Shell shell)
			throws GitAPIException {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repo);
		return handleUncommittedFiles(repo, shell,
				MessageFormat.format(
						UIText.AbstractRebaseCommandHandler_cleanupDialog_title,
						repoName));
	}

	/**
	 * Checks the repository to see if there are uncommitted changes, and
	 * prompts the user to clean them up.
	 *
	 * @param repo
	 *            the repository
	 * @param shell
	 *            the parent shell for opening the dialog
	 * @param dialogTitle
	 *            the dialog title
	 * @return true if the git status was clean or it was dirty and the user
	 *         cleaned up the uncommitted changes and the previous action may
	 *         continue
	 * @throws GitAPIException
	 *             if there was an error checking the repository
	 */
	public static boolean handleUncommittedFiles(Repository repo, Shell shell,
			String dialogTitle) throws GitAPIException {
		Status status = null;
		try (Git git = new Git(repo)) {
			status = git.status().call();
		}
		if (status != null && status.hasUncommittedChanges()) {
			List<String> files = new ArrayList<>(status.getModified());
			return doDialog(repo, files, dialogTitle, shell);
		}
		return true;
	}

	private static boolean doDialog(Repository repo, List<String> files,
			String dialogTitle, Shell shell) {
		Collections.sort(files);
		CleanupUncomittedChangesDialog cleanupUncomittedChangesDialog = new CleanupUncomittedChangesDialog(
				shell, dialogTitle,
				UIText.AbstractRebaseCommandHandler_cleanupDialog_text, repo,
				files);
		cleanupUncomittedChangesDialog.open();
		return cleanupUncomittedChangesDialog.shouldContinue();
	}

	/**
	 * Checks the repository to see if there are uncommitted changes of the
	 * files modified by the given commit and prompts the user to clean them up
	 * if so.
	 *
	 * @param repo
	 *            {@link Repository} to check
	 * @param commit
	 *            {@link RevCommit} to check against
	 * @param parentIndex
	 *            if >= 0 and the {@code commit} is a merge commit, use the
	 *            given parent index to compute the changes of the commit
	 * @param title
	 *            to show in the dialog if there are uncommitted changes
	 * @param shell
	 *            to parent the dialog off
	 * @return whether to continue with the operation
	 * @throws GitAPIException
	 *             if an error occurs
	 */
	public static boolean handleUncommittedFiles(Repository repo,
			RevCommit commit, int parentIndex, String title, Shell shell)
			throws GitAPIException {
		int nofParents = commit.getParentCount();
		if (nofParents == 0) {
			return handleUncommittedFiles(repo, shell, title);
		}
		List<String> conflicts = new ArrayList<>();
		try (Git git = new Git(repo);
				DiffFormatter fmt = new DiffFormatter(
						new ByteArrayOutputStream())) {
			Status status = git.status().call();
			if (status != null && status.hasUncommittedChanges()) {
				Set<String> uncommitted = status.getUncommittedChanges();
				RevCommit parent = repo.parseCommit(commit.getParent(0));
				if (nofParents > 1 && parentIndex > 0
						&& parentIndex < nofParents) {
					parent = repo.parseCommit(commit.getParent(parentIndex));
				}
				fmt.setRepository(repo);
				for (DiffEntry diff : fmt.scan(commit, parent)) {
					switch (diff.getChangeType()) {
					case ADD:
					case COPY:
					case MODIFY:
						if (uncommitted.contains(diff.getNewPath())) {
							conflicts.add(diff.getNewPath());
						}
						break;
					case DELETE:
						if (uncommitted.contains(diff.getOldPath())) {
							conflicts.add(diff.getOldPath());
						}
						break;
					default:
						if (uncommitted.contains(diff.getNewPath())) {
							conflicts.add(diff.getNewPath());
						}
						if (uncommitted.contains(diff.getOldPath())) {
							conflicts.add(diff.getOldPath());
						}
						break;
					}
				}
			}
		} catch (IOException e) {
			return handleUncommittedFiles(repo, shell, title);
		}
		if (conflicts.isEmpty()) {
			return true;
		}
		return doDialog(repo, conflicts, title, shell);
	}

}
