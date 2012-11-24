/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    François Rey - refactoring as part of gracefully ignoring linked resources
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.merge.GitMergeEditorInput;
import org.eclipse.egit.ui.internal.merge.MergeModeDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;

/**
 * Action to start the Merge Tool.
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
		Repository  repo = getRepository();
		if (repo == null)
			return false;
		switch (repo.getRepositoryState()) {
		case MERGING:
			// fall through
		case CHERRY_PICKING:
			// fall through
		case REBASING:
			// fall through
		case REBASING_INTERACTIVE:
			// fall through
		case REBASING_MERGE:
			// fall through
		case REBASING_REBASING:
			return true;
		default:
			return false;
		}
	}
}
