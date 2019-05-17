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

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommandConfirmation;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
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
			if (!CommandConfirmation.confirmCheckout(getShell(event), paths,
					!hasDirectories)) {
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
