/*******************************************************************************
 * Copyright (C) 2011, Tasktop Technologies Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Benjamin Muskalla (benjamin.muskalla@tasktop.com) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
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
		List<IResource> resourcesInScope = new ArrayList<IResource>();
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
		for (IResource resource : relevantResources)
			if (hasChanged(resource))
				return super.promptForInputChange(requestPreviewMessage, monitor);
		return false;
	}

	private boolean hasChanged(IResource resource) {
		boolean hasChanged = false;
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		try {
			Repository repository = mapping.getRepository();
			Status repoStatus = new Git(repository).status().call();
			String path = resource.getFullPath().removeFirstSegments(1)
					.toOSString();
			hasChanged = repoStatus.getAdded().contains(path)
					|| repoStatus.getChanged().contains(path)
					|| repoStatus.getModified().contains(path)
					|| repoStatus.getRemoved().contains(path)
					|| repoStatus.getUntracked().contains(path);
		} catch (Exception e) {
			Activator.logError(UIText.GitScopeOperation_couldNotDetermineState,
					e);
		}
		return hasChanged;
	}

}
