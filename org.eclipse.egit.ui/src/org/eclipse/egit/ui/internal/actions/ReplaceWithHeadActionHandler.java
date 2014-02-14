/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Benjamin Muskalla (Tasktop Technologies Inc.) - support for model scoping
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * Replace with HEAD revision action handler
 */
public class ReplaceWithHeadActionHandler extends DiscardChangesActionHandler {

	@Override
	protected String gatherRevision(ExecutionEvent event) {
		return Constants.HEAD;
	}

	@Override
	public boolean isEnabled() {
		return super.isEnabled() && isAtLeastOneTracked();
	}

	private boolean isAtLeastOneTracked() {
		Repository[] repositories = getRepositories();
		IResource[] selectedResources = getSelectedResources();
		List<IResource> notUntracked = new ArrayList<IResource>(
				Arrays.asList(selectedResources));
		for (Repository repository : repositories) {
			IndexDiffData indexDiff = org.eclipse.egit.core.Activator
					.getDefault().getIndexDiffCache()
					.getIndexDiffCacheEntry(repository).getIndexDiff();
			if (indexDiff != null) {
				for (IResource selectedResource : selectedResources) {
					String repoRelativePath = makeRepoRelative(repository,
							selectedResource);
					if (containsPrefix(indexDiff.getUntracked(),
							repoRelativePath)) {
						notUntracked.remove(selectedResource);
					}
				}
			}
		}
		return !notUntracked.isEmpty();
	}

	private static String makeRepoRelative(Repository repo, IResource res) {
		return Repository.stripWorkDir(repo.getWorkTree(), res.getLocation()
				.toFile());
	}

	private boolean containsPrefix(Set<String> collection, String prefix) {
		for (String path : collection)
			if (path.startsWith(prefix))
				return true;
		return false;
	}

}
