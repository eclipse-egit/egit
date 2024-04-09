/******************************************************************************
 *  Copyright (c) 2024 Thomas Wolf <twolf@apache.org> and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommandConfirmation;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Checks out the "new" version of selected {@link FileDiff}s, which are assumed
 * to be all from the same repository.
 */
public class FileDiffCheckoutNewHandler extends AbstractHistoryCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		List<FileDiff> diffs = getDiffs(selection);
		if (!diffs.isEmpty()) {
			FileDiff first = diffs.get(0);
			Repository repository = first.getRepository();
			String revision = first.getCommit().getName();
			List<String> paths = diffs.stream().map(FileDiff::getNewPath)
					.collect(Collectors.toList());
			DiscardChangesOperation operation = new DiscardChangesOperation(
					repository, paths);
			operation.setRevision(revision);
			if (CommandConfirmation.confirmCheckout(
					HandlerUtil.getActiveShellChecked(event),
					operation.getPathsPerRepository(), true)) {
				JobUtil.scheduleUserWorkspaceJob(operation,
						UIText.DiscardChangesAction_discardChanges,
						JobFamilies.DISCARD_CHANGES);
			}
		}
		return null;
	}

	private List<FileDiff> getDiffs(IStructuredSelection selection) {
		List<FileDiff> result = new ArrayList<>();
		Iterator<?> items = selection.iterator();
		while (items.hasNext()) {
			FileDiff diff = Adapters.adapt(items.next(), FileDiff.class);
			if (diff != null && !diff.isSubmodule()
					&& !ChangeType.DELETE.equals(diff.getChange())) {
				result.add(diff);
			}
		}
		return result;
	}

}
