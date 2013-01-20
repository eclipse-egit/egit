/*******************************************************************************
 * Copyright (C) 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.util.Set;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Base class for actions that want to compare with an index entry for a
 * particular stage.
 */
public abstract class CompareWithIndexStageActionHandler extends
		RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IPath[] locations = getSelectedLocations(event);

		if (locations.length == 1 && locations[0].toFile().isFile()) {
			final IPath baseLocation = locations[0];
			final ITypedElement left = CompareUtils
					.getFileTypedElement(baseLocation);
			final ITypedElement right = CompareUtils.getIndexTypedElement(
					baseLocation, getStage());
			final ITypedElement ancestor = CompareUtils.getIndexTypedElement(
					baseLocation, DirCacheEntry.STAGE_1);
			final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
					left, right, ancestor, null);
			IWorkbenchPage workBenchPage = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
			CompareUtils.openInCompare(workBenchPage, in);
		}
		return null;
	}

	/**
	 * @return the stage which should be used when getting the index entry for
	 *         the right side of the compare input
	 */
	protected abstract int getStage();

	@Override
	public boolean isEnabled() {
		IPath[] paths = getSelectedLocations();
		if (paths.length == 1) {
			IPath path = paths[0];
			RepositoryMapping mapping = RepositoryMapping.getMapping(path);
			if (mapping != null) {
				Set<String> conflictingFiles = getConflictingFiles(mapping
						.getRepository());
				String relativePath = mapping.getRepoRelativePath(path);
				return conflictingFiles.contains(relativePath);
			}
		}
		return false;
	}
}
