/*******************************************************************************
 * Copyright (c) 2016, Matthias Sohn <matthias.sohn@sap.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - use the abstract super class
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 477248
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog to confirm a potentially destructive command
 */
public class CommandConfirmation {

	/**
	 * Ask the user to confirm hard reset. Warns the user if a running launch
	 * could be affected by the reset.
	 *
	 * @param shell
	 * @param repo
	 * @return {@code true} if the user confirmed hard reset
	 */
	public static boolean confirmHardReset(Shell shell, final Repository repo) {
		String question = UIText.ResetTargetSelectionDialog_ResetConfirmQuestion;
		String launch = LaunchFinder
				.getRunningLaunchConfiguration(Collections.singleton(repo),
						null);
		if (launch != null) {
			question += "\n\n" + MessageFormat.format( //$NON-NLS-1$
					UIText.LaunchFinder_RunningLaunchMessage, launch);
		}

		MessageDialog messageDialog = new MessageDialog(shell,
				UIText.ResetTargetSelectionDialog_ResetConfirmTitle, null,
				question, MessageDialog.QUESTION,
				new String[] {
						UIText.CommandConfirmationHardResetDialog_resetButtonLabel,
						IDialogConstants.CANCEL_LABEL },
				0);

		return messageDialog.open() == Window.OK;
	}

	/**
	 * Ask the user to confirm an operation that might lose uncommitted changes.
	 *
	 * @param shell
	 *            to use as parent for the dialog asking the user. If
	 *            {@code null} a suitable parent is determined automatically.
	 * @param repository
	 *            to check
	 * @return {@code true} if the user confirmed the operation, {@code false}
	 *         otherwise
	 */
	public static boolean confirmCheckout(Shell shell, Repository repository) {
		return confirmCheckout(shell,
				Collections.singletonMap(repository, Collections.emptyList()),
				true);
	}

	/**
	 * Ask the user to confirm an operation that might lose uncommitted changes.
	 *
	 * @param shell
	 *            to use as parent for the dialog asking the user. If
	 *            {@code null} a suitable parent is determined automatically.
	 * @param repoAndPaths
	 *            to check
	 * @param filesOnly
	 *            {@code true} if the paths are all for files; {@code false} if
	 *            there may be directories among the paths
	 * @return {@code true} if the user confirmed the operation, {@code false}
	 *         otherwise
	 */
	public static boolean confirmCheckout(Shell shell,
			Map<Repository, Collection<String>> repoAndPaths,
			boolean filesOnly) {
		if (haveChanges(repoAndPaths, filesOnly)) {
			String question = UIText.DiscardChangesAction_confirmActionMessage;
			String launch = LaunchFinder
					.getRunningLaunchConfiguration(repoAndPaths.keySet(), null);
			if (launch != null) {
				question += "\n\n" + MessageFormat.format( //$NON-NLS-1$
						UIText.LaunchFinder_RunningLaunchMessage, launch);
			}
			Shell parent = shell != null ? shell
					: PlatformUI.getWorkbench().getModalDialogShellProvider()
							.getShell();
			MessageDialog messageDialog = new MessageDialog(parent,
					UIText.DiscardChangesAction_confirmActionTitle, null,
					question, MessageDialog.CONFIRM,
					new String[] {
							UIText.DiscardChangesAction_discardChangesButtonText,
							IDialogConstants.CANCEL_LABEL },
					0);
			return messageDialog.open() == Window.OK;
		} else {
			return !LaunchFinder.shouldCancelBecauseOfRunningLaunches(
					repoAndPaths.keySet(), null);
		}
	}

	private static boolean haveChanges(
			Map<Repository, Collection<String>> paths,
			boolean filesOnly) {
		IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
		for (Map.Entry<Repository, Collection<String>> entry : paths
				.entrySet()) {
			Repository repo = entry.getKey();
			Assert.isNotNull(repo);
			IndexDiffCacheEntry indexDiff = cache.getIndexDiffCacheEntry(repo);
			if (indexDiff == null) {
				return true; // No info, assume worst case
			}
			IndexDiffData diff = indexDiff.getIndexDiff();
			if (diff == null || hasChanges(diff, entry.getValue(), filesOnly)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasChanges(@NonNull IndexDiffData diff,
			Collection<String> paths, boolean filesOnly) {
		if (paths.isEmpty()) {
			// We're going to do _something_, but it's unknown which paths will
			// be affected
			return diff.hasChanges();
		}
		Set<String> repoPaths = new HashSet<>(paths);
		// Untracked files are ignored and won't be removed.
		if (repoPaths.contains("")) { //$NON-NLS-1$
			// Working tree root included
			return diff.hasChanges();
		}
		// Do the directories later to avoid having to do all the (potentially
		// expensive) substrings if a plain file already matches.
		if (containsAny(repoPaths, diff.getAdded())
				|| containsAny(repoPaths, diff.getChanged())
				|| containsAny(repoPaths, diff.getModified())
				|| containsAny(repoPaths, diff.getRemoved())) {
			return true;
		}
		if (!filesOnly) {
			return containsAnyDirectory(repoPaths, diff.getAdded())
					|| containsAnyDirectory(repoPaths, diff.getChanged())
					|| containsAnyDirectory(repoPaths, diff.getModified())
					|| containsAnyDirectory(repoPaths, diff.getRemoved());
		}
		return false;
	}

	private static boolean containsAny(Set<String> repoPaths,
			Collection<String> files) {
		return files.stream().anyMatch(repoPaths::contains);
	}

	private static boolean containsAnyDirectory(Set<String> repoPaths,
			Collection<String> files) {
		String lastDirectory = null;
		for (String file : files) {
			int j = file.lastIndexOf('/');
			if (j <= 0) {
				continue;
			}
			String directory = file.substring(0, j);
			String withTerminator = directory + '/';
			if (lastDirectory != null
					&& lastDirectory.startsWith(withTerminator)) {
				continue;
			}
			if (repoPaths.contains(directory)) {
				return true;
			}
			lastDirectory = withTerminator;
			for (int i = directory.indexOf('/'); i > 0; i = directory
					.indexOf('/', i + 1)) {
				if (repoPaths.contains(directory.substring(0, i))) {
					return true;
				}
			}
		}
		return false;
	}
}
