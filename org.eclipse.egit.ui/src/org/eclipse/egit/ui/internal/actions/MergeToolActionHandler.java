/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
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

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		int mergeMode = Activator.getDefault().getPreferenceStore().getInt(
				UIPreferences.MERGE_MODE);
		CompareEditorInput input;
		if (mergeMode == 0) {
			MergeModeDialog dlg = new MergeModeDialog(getShell(event));
			if (dlg.open() != Window.OK)
				return null;
			input = new GitMergeEditorInput(dlg.useWorkspace(),
					getSelectedResources(event));
		} else {
			boolean useWorkspace = mergeMode == 1;
			input = new GitMergeEditorInput(useWorkspace,
					getSelectedResources(event));
		}
		CompareUI.openCompareEditor(input);
		return null;
	}

	public boolean isEnabled() {
		IResource[] resources = getSelectedResources();
		Map<Repository, Collection<String>> pathsByRepository = ResourceUtil.splitResourcesByRepository(resources);

		Set<Repository> repos = pathsByRepository.keySet();

		if (repos.size() != 1)
			return false;

		Repository repo = repos.iterator().next();
		Collection<String> selectedRepoPaths = pathsByRepository.get(repo);
		if (selectedRepoPaths.isEmpty())
			return false;

		Set<String> conflictingFiles = getConflictingFiles(repo);

		for (String selectedRepoPath : selectedRepoPaths) {
			Path selectedPath = new Path(selectedRepoPath);

			for (String conflictingFile : conflictingFiles)
				if (selectedPath.isPrefixOf(new Path(conflictingFile)))
					return true;
		}

		return false;
	}
}
