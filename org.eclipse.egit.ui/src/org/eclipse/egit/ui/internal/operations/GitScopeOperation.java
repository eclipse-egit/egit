/*******************************************************************************
 * Copyright (C) 2011, 2013 Tasktop Technologies Inc and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Benjamin Muskalla (benjamin.muskalla@tasktop.com) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.ui.synchronize.ModelOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Builds the scope for a git operation by asking all relevant model providers.
 */
public class GitScopeOperation extends ModelOperation {

	/**
	 * @param part
	 * @param manager
	 */
	public GitScopeOperation(IWorkbenchPart part,
			ISynchronizationScopeManager manager) {
		super(part, manager);
	}

	@Override
	protected void execute(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		// Do nothing since we only want to build the scope
	}

	/**
	 * Collects all resources from the given synchronization scope.
	 *
	 * @return list of {@link IResource}s
	 */
	List<IResource> getRelevantResources() {
		List<IResource> resourcesInScope = new ArrayList<>();
		ResourceTraversal[] traversals = getScope().getTraversals();
		for (ResourceTraversal resourceTraversal : traversals)
			resourcesInScope.addAll(Arrays.asList(resourceTraversal
					.getResources()));
		return resourcesInScope;
	}

	@Override
	protected boolean promptForInputChange(String requestPreviewMessage,
			IProgressMonitor monitor) {
		List<IResource> relevantResources = getRelevantResources();
		Map<Repository, Collection<String>> pathsByRepo = ResourceUtil
				.splitResourcesByRepository(relevantResources);
		for (Map.Entry<Repository, Collection<String>> entry : pathsByRepo
				.entrySet()) {
			Repository repository = entry.getKey();
			Collection<String> paths = entry.getValue();
			IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
			if (cache == null)
				continue;

			IndexDiffCacheEntry cacheEntry = cache.getIndexDiffCacheEntry(repository);
			if (cacheEntry == null)
				continue;

			IndexDiffData indexDiff = cacheEntry.getIndexDiff();
			if (indexDiff == null)
				continue;

			if (hasAnyPathChanged(paths, indexDiff))
				return super.promptForInputChange(requestPreviewMessage,
						monitor);
		}
		return false;
	}

	private static boolean hasAnyPathChanged(Collection<String> paths,
			IndexDiffData indexDiff) {
		for (String path : paths) {
			boolean hasChanged = indexDiff.getAdded().contains(path)
					|| indexDiff.getChanged().contains(path)
					|| indexDiff.getModified().contains(path)
					|| indexDiff.getRemoved().contains(path)
					|| indexDiff.getUntracked().contains(path);
			if (hasChanged)
				return true;
		}
		return false;
	}

}
