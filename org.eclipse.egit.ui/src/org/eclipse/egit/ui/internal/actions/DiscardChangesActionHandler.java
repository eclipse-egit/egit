/*******************************************************************************
 * Copyright (C) 2010, 2019 Roland Grunberg <rgrunber@redhat.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Benjamin Muskalla (Tasktop Technologies Inc.) - support for model scoping
 *   François Rey <eclipse.org_@_francois_._rey_._name> - handling of linked resources
 *   Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777, 546194
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Checkout all selected dirty files.
 */
public class DiscardChangesActionHandler extends RepositoryActionHandler {

	private boolean hasDirectories;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// capture selection from active part as long as we have context
		mySelection = getSelection(event);
		try {
			IWorkbenchPart part = getPart(event);
			DiscardChangesOperation operation = createOperation(part, event);
			if (operation == null) {
				return null;
			}
			Map<Repository, Collection<String>> paths = operation
					.getPathsPerRepository();
			if (haveChanges(paths)) {
				String question = UIText.DiscardChangesAction_confirmActionMessage;
				String launch = LaunchFinder
						.getRunningLaunchConfiguration(paths.keySet(), null);
				if (launch != null) {
					question = MessageFormat.format(question,
							"\n\n" + MessageFormat.format( //$NON-NLS-1$
									UIText.LaunchFinder_RunningLaunchMessage,
									launch));
				} else {
					question = MessageFormat.format(question, ""); //$NON-NLS-1$
				}
				if (!openConfirmationDialog(event, question)) {
					return null;
				}
			} else if (LaunchFinder.shouldCancelBecauseOfRunningLaunches(
					paths.keySet(), null)) {
				return null;
			}
			JobUtil.scheduleUserWorkspaceJob(operation,
					UIText.DiscardChangesAction_discardChanges,
					JobFamilies.DISCARD_CHANGES);
			return null;
		} finally {
			// cleanup mySelection to avoid side effects later after execution
			mySelection = null;
		}
	}

	private boolean haveChanges(Map<Repository, Collection<String>> paths) {
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
			if (diff == null || hasChanges(diff, entry.getValue())) {
				return true;
			}
		}
		return false;
	}

	private boolean hasChanges(@NonNull IndexDiffData diff,
			Collection<String> paths) {
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
		if (hasDirectories) {
			return containsAnyDirectory(repoPaths, diff.getAdded())
					|| containsAnyDirectory(repoPaths, diff.getChanged())
					|| containsAnyDirectory(repoPaths, diff.getModified())
					|| containsAnyDirectory(repoPaths, diff.getRemoved());
		}
		return false;
	}

	private boolean containsAny(Set<String> repoPaths,
			Collection<String> files) {
		return files.stream().anyMatch(repoPaths::contains);
	}

	private boolean containsAnyDirectory(Set<String> repoPaths,
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
			for (int i = directory.indexOf('/'); i > 0; i = directory.indexOf(
					'/', i + 1)) {
				if (repoPaths.contains(directory.substring(0, i))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean openConfirmationDialog(ExecutionEvent event,
			String question) throws ExecutionException {
		MessageDialog dlg = new MessageDialog(getShell(event),
				UIText.DiscardChangesAction_confirmActionTitle, null, question,
				MessageDialog.CONFIRM,
				new String[] {
						UIText.DiscardChangesAction_discardChangesButtonText,
						IDialogConstants.CANCEL_LABEL },
				0);
		return dlg.open() == Window.OK;
	}

	@Override
	public boolean isEnabled() {
		Repository[] repositories = getRepositories();
		if (repositories.length == 0)
			return false;
		for (Repository repository : repositories) {
			if (!repository.getRepositoryState().equals(RepositoryState.SAFE))
				return false;
		}
		return true;
	}

	private DiscardChangesOperation createOperation(IWorkbenchPart part,
			ExecutionEvent event) throws ExecutionException {
		IResource[] selectedResources = gatherResourceToOperateOn(event);
		String revision;
		try {
			revision = gatherRevision(event);
		} catch (OperationCanceledException e) {
			return null;
		}

		IResource[] resourcesInScope;
		try {
			resourcesInScope = GitScopeUtil.getRelatedChanges(part,
					selectedResources);
		} catch (InterruptedException e) {
			// ignore, we will not discard the files in case the user
			// cancels the scope operation
			return null;
		}
		hasDirectories = Stream.of(resourcesInScope)
				.anyMatch(rsc -> rsc.getType() != IResource.FILE);
		return new DiscardChangesOperation(resourcesInScope, revision);
	}

	/**
	 * @param event
	 * @return set of resources to operate on
	 * @throws ExecutionException
	 */
	protected IResource[] gatherResourceToOperateOn(ExecutionEvent event)
			throws ExecutionException {
		return getSelectedResources(event);
	}

	/**
	 * @param event
	 * @return the revision to use
	 * @throws ExecutionException
	 */
	protected String gatherRevision(ExecutionEvent event)
			throws ExecutionException {
		return null;
	}
}
