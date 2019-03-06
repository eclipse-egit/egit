/*******************************************************************************
 * Copyright (c) 2010, 2019 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Christian W. Damus - bug 544395
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.merge.GitMergeEditorInput;
import org.eclipse.egit.ui.internal.merge.MergeModeDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;

/**
 * Action for selecting a commit and merging it with the current branch.
 */
public class MergeToolActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		int mergeMode = Activator.getDefault().getPreferenceStore().getInt(
				UIPreferences.MERGE_MODE);
		IPath[] locations = getSelectedLocations(event);
		CompareEditorInput input;
		if (mergeMode == 0) {
			MergeModeDialog dlg = new MergeModeDialog(getShell(event));
			if (dlg.open() != Window.OK)
				return null;
			input = new GitMergeEditorInput(dlg.useWorkspace(), locations);
		} else {
			boolean useWorkspace = mergeMode == 1;
			input = new GitMergeEditorInput(useWorkspace, locations);
		}
		CompareUI.openCompareEditor(input);
		return null;
	}

	@Override
	public boolean isEnabled() {
		IPath[] paths = getSelectedLocations();
		Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
				.splitPathsByRepository(Arrays.asList(paths));

		if (pathsByRepository.size() != 1)
			return false;

		Entry<Repository, Collection<String>> pathsEntry = pathsByRepository
				.entrySet().iterator().next();
		Repository repo = pathsEntry.getKey();
		Collection<String> selectedRepoPaths = pathsEntry.getValue();
		if (selectedRepoPaths.isEmpty())
			return false;

		IndexDiffCache cache = org.eclipse.egit.core.Activator.getDefault().getIndexDiffCache();
		if (cache == null)
			return false;

		IndexDiffCacheEntry entry = cache.getIndexDiffCacheEntry(repo);
		if (entry == null || entry.getIndexDiff() == null)
			return false;

		Set<String> conflictingFiles = entry.getIndexDiff().getConflicting();
		if (conflictingFiles.isEmpty())
			return false;

		for (String selectedRepoPath : selectedRepoPaths) {
			Path selectedPath = new Path(selectedRepoPath);

			for (String conflictingFile : conflictingFiles)
				if (selectedPath.isPrefixOf(new Path(conflictingFile)))
					return true;
		}

		return false;
	}

	/**
	 * Git status of a selected resource can change independently of the
	 * workbench selection, so always check enabled.
	 *
	 * @return {@code true}, always
	 */
	@Override
	protected boolean alwaysCheckEnabled() {
		return true;
	}
}
