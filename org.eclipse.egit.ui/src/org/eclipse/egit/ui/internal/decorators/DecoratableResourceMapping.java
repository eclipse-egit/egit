/*******************************************************************************
 * Copyright (C) 2016, Stefan Dirix <sdirix@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.resources.ResourceStateFactory;
import org.eclipse.jgit.lib.Repository;

/**
 * Represents a decoratable resource mapping (i.e. a group of resources).
 */
public class DecoratableResourceMapping extends DecoratableResource {

	private static final String MULTIPLE = "*"; //$NON-NLS-1$

	/**
	 * Denotes the type of decoratable resource, used by the decoration helper.
	 */
	public static final int RESOURCE_MAPPING = 0x10;

	/**
	 * Stores the actual mapping we are currently decorating.
	 */
	private ResourceMapping mapping;

	/**
	 * Creates a decoratable resource mapping.
	 *
	 * @param mapping
	 *            the resource mapping to decorate
	 * @throws IOException
	 * @see DecoratableWorkingSet
	 */
	public DecoratableResourceMapping(ResourceMapping mapping)
			throws IOException {
		super(null); // no resource ...

		this.mapping = mapping;

		Set<Repository> repositories = new HashSet<>();
		Set<StagingState> stagingStates = new HashSet<>();

		List<IResource> extractResourcesFromMapping = extractResourcesFromMapping();

		boolean anyIsTracked = false;
		boolean anyIsConflict = false;
		boolean anyIsDirty = false;

		for (IResource mappingResource : extractResourcesFromMapping) {
			if (mappingResource == null) {
				continue;
			}

			IndexDiffData indexDiffData = ResourceStateFactory.getInstance()
					.getIndexDiffDataOrNull(mappingResource);

			if (indexDiffData == null) {
				continue;
			}

			Repository repository = ResourceUtil.getRepository(mappingResource);
			repositories.add(repository);

			DecoratableResourceAdapter adapter = new DecoratableResourceAdapter(
					indexDiffData, mappingResource);
			if (adapter.isTracked()) {
				anyIsTracked = true;
			}
			if (adapter.hasConflicts()) {
				anyIsConflict = true;
			}
			if (adapter.isDirty()) {
				anyIsDirty = true;
			}
			stagingStates.add(adapter.getStagingState());
		}

		if (anyIsTracked) {
			setTracked(true);
		}
		if (anyIsConflict) {
			setConflicts(true);
		}
		if (anyIsDirty) {
			setDirty(true);
		}

		if (stagingStates.isEmpty()) {
			setStagingState(StagingState.NOT_STAGED);
		} else if (stagingStates.size() == 1) {
			StagingState state = stagingStates.iterator().next();
			if (state != null) {
				setStagingState(state);
			}
		} else {
			// handle mixed states by setting to modified
			setStagingState(StagingState.MODIFIED);
		}

		decorateRepositoryInformation(this, repositories);
	}

	/**
	 * Decorate repositoryName, branch and branchStatus of
	 * {@link ResourceMapping}s.
	 *
	 * @param resource
	 *            the {@link DecoratableResource} to decorate.
	 * @param repositories
	 *            the collection of {@link Repository} which are affected by the
	 *            given {@link DecoratableResource}.
	 * @throws IOException
	 */
	protected static void decorateRepositoryInformation(
			DecoratableResource resource, Collection<Repository> repositories)
			throws IOException {
		// collect repository info for decoration (bug 369969)
		if (repositories.size() == 1) {
			// single repo, single branch --> [repo branch]
			Repository repository = repositories.iterator().next();
			resource.repositoryName = DecoratableResourceHelper
					.getRepositoryName(repository);
			resource.branch = DecoratableResourceHelper
					.getShortBranch(repository);
			resource.branchStatus = DecoratableResourceHelper
					.getBranchStatus(repository);
		} else if (repositories.size() > 1) {
			// collect branch names but skip branch status (doesn't make sense)
			Set<String> branches = new HashSet<>(2);
			for (Repository repository : repositories) {
				branches.add(
						DecoratableResourceHelper.getShortBranch(repository));
				if (branches.size() > 1)
					break;
			}

			// multiple repos, one branch --> [* branch]
			if (branches.size() == 1) {
				resource.repositoryName = MULTIPLE;
				resource.branch = branches.iterator().next();
			}

			// we set nothing in the following case:
			// multiple repos, multiple branches
		}
	}

	/**
	 * Returns the {@link IResource}s contained in the {@link ResourceMapping}.
	 *
	 * @return a possibly empty list of contained {@link IResource}s.
	 */
	private List<IResource> extractResourcesFromMapping() {
		ResourceTraversal[] traversals;
		try {
			traversals = mapping.getTraversals(null, null);
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
			return Collections.emptyList();
		}

		if (traversals.length == 0)
			return Collections.emptyList();

		List<IResource> result = new ArrayList<>();
		for (ResourceTraversal traversal : traversals) {
			IResource[] resources = traversal.getResources();
			result.addAll(Arrays.asList(resources));
		}
		return result;
	}

	@Override
	public int getType() {
		return RESOURCE_MAPPING;
	}

	@Override
	public String getName() {
		// this value is not used by the GitLightweightDecorator, instead a
		// label provider provides the name and the decorator only provides
		// prefixes and suffixes
		return "<unknown>"; //$NON-NLS-1$
	}
}
