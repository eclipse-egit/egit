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

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.internal.commit.DiffViewer;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Opens the "new" version of a {@link FileDiff}.
 */
public class FileDiffOpenWorkingTreeHandler extends AbstractFileDiffHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		List<FileDiff> diffs = getDiffs(selection,
				Predicate.not(FileDiff::isSubmodule));
		if (!diffs.isEmpty()) {
			for (FileDiff diff : diffs) {
				String relativePath = diff.getPath();
				File file = new Path(
						diff.getRepository().getWorkTree().getAbsolutePath())
								.append(relativePath).toFile();
				DiffViewer.openFileInEditor(file, -1);
			}
		}
		return null;
	}

}
