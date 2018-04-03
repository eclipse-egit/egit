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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.branch.CleanupUncomittedChangesDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
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
			return showCleanupDialog(repo, files, dialogTitle, shell);
		}
		return true;
	}

	/**
	 * Displays a dialog giving the user the opportunity to commit, stash, or
	 * reset uncommitted changes before doing a repository operation, or to
	 * abandon the operation.
	 *
	 * @param repo
	 *            {@link Repository} we're working on
	 * @param files
	 *            with uncommitted changes
	 * @param title
	 *            for the dialog
	 * @param shell
	 *            to parent the dialog off
	 * @return whether to continue the operation
	 */
	public static boolean showCleanupDialog(Repository repo, List<String> files,
			String title, Shell shell) {
		Collections.sort(files);
		CleanupUncomittedChangesDialog cleanupUncomittedChangesDialog = new CleanupUncomittedChangesDialog(
				shell, title,
				UIText.AbstractRebaseCommandHandler_cleanupDialog_text, repo,
				files);
		cleanupUncomittedChangesDialog.open();
		return cleanupUncomittedChangesDialog.shouldContinue();
	}

}
