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
package org.eclipse.egit.ui.internal.filediff;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.commit.DiffViewer;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.diff.DiffEntry;

/**
 * Opens the "new" version of a {@link FileDiff}.
 */
public class FileDiffOpenPreviousHandler extends AbstractFileDiffHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		List<FileDiff> diffs = getDiffs(selection, d -> !d.isSubmodule()
				&& !DiffEntry.ChangeType.ADD.equals(d.getChange()));
		if (!diffs.isEmpty()) {
			for (FileDiff diff : diffs) {
				DiffViewer.openInEditor(diff, DiffEntry.Side.OLD, -1);
			}
		}
		return null;
	}

}
