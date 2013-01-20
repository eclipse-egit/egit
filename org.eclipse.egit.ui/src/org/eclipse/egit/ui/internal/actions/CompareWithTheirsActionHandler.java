/*******************************************************************************
 * Copyright (C) 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.util.Map;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

/**
 * Action handler for "Compare with theirs". Note that the actual label depends
 * on the repository state.
 */
public class CompareWithTheirsActionHandler extends
		CompareWithIndexStageActionHandler implements IElementUpdater {

	@Override
	protected int getStage() {
		return DirCacheEntry.STAGE_3;
	}

	public void updateElement(UIElement element, Map parameters) {
		Repository repo = getRepository();
		if (repo == null)
			return;

		String text = getNameForState(repo);
		element.setText(text);
	}

	private String getNameForState(Repository repo) {
		RepositoryState state = repo.getRepositoryState();
		if (state == RepositoryState.REBASING)
			return UIText.CompareWithTheirsActionHandler_LabelWhenRebasing;
		else if (state == RepositoryState.CHERRY_PICKING)
			return UIText.CompareWithTheirsActionHandler_LabelWhenCherryPicking;
		else
			return UIText.CompareWithTheirsActionHandler_LabelWhenMerging;
	}
}
