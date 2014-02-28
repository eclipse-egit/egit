/*******************************************************************************
 * Copyright (C) 2014, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.merge.GitResourceVariantTreeProvider;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.team.core.variants.IResourceVariantTree;

/**
 * This will populate its three {@link IResourceVariantTree} by looking up
 * information within a given {@link GitSynchronizeDataSet}.
 */
public class SynchronizeDataTreeProvider implements
		GitResourceVariantTreeProvider {
	private final IResourceVariantTree baseTree;

	private final IResourceVariantTree remoteTree;

	private final IResourceVariantTree sourceTree;

	private final Set<IResource> roots;

	private final Set<IResource> includedResources;

	/**
	 * @param gitCache
	 * @param gsds
	 */
	public SynchronizeDataTreeProvider(GitSyncCache gitCache,
			GitSynchronizeDataSet gsds) {
		this.baseTree = new GitBaseResourceVariantTree(gitCache, gsds);
		this.remoteTree = new GitRemoteResourceVariantTree(gitCache, gsds);
		this.sourceTree = new GitSourceResourceVariantTree(gitCache, gsds);

		final IProject[] projects = gsds.getAllProjects();
		roots = new LinkedHashSet<IResource>(Arrays.asList(projects));

		includedResources = new LinkedHashSet<IResource>();
		for (IProject project : projects) {
			final GitSynchronizeData data = gsds.getData(project);
			if (data.getIncludedResources() != null)
				includedResources.addAll(data.getIncludedResources());
		}
	}

	public IResourceVariantTree getBaseTree() {
		return baseTree;
	}

	public IResourceVariantTree getRemoteTree() {
		return remoteTree;
	}

	public IResourceVariantTree getSourceTree() {
		return sourceTree;
	}

	public Set<IResource> getRoots() {
		return roots;
	}

	public Set<IResource> getKnownResources() {
		return includedResources;
	}

}
