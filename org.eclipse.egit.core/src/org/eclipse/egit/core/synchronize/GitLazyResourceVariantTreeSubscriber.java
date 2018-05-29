/*******************************************************************************
 * Copyright (C) 2017 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;

/**
 * Extension of {@link GitResourceVariantTreeSubscriber} that is able to lazily
 * load data from resources that were out of the initial scope defined by the
 * provided {@link GitSynchronizeDataSet}. This is supposed to be used by
 * logical model aware {@link RemoteResourceMappingContext}s to allow loading
 * the variants of resources included in logical models.
 */
public class GitLazyResourceVariantTreeSubscriber
		extends GitResourceVariantTreeSubscriber {

	private boolean isLoaded;

	private boolean isValid;

	/**
	 * @param gsds
	 */
	public GitLazyResourceVariantTreeSubscriber(GitSynchronizeDataSet gsds) {
		super(gsds);
	}

	@Override
	public boolean isSupervised(IResource res) throws TeamException {
		return true;
	}

	@Override
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote) throws TeamException {
		if (getDataSet().shouldBeIncluded(local)) {
			return super.getSyncInfo(local, base, remote);
		}
		// Otherwise, the given resource was not in the original scope this
		// subscriber was built. However, we want to access it anyway.

		IProject project = local.getProject();
		Repository repo = ResourceUtil.getRepository(local);
		if (repo == null) {
			return null;
		}
		GitSynchronizeData data = getDataSet().getData(project);
		if (data == null) {
			for (GitSynchronizeData sd : getDataSet()) {
				if (repo.equals(sd.getRepository())) {
					data = sd;
					break;
				}
			}
		}
		if (data == null) {
			return null;
		}
		return getSyncInfo(local, base, remote, data);
	}

	/**
	 * Provide a {@link SyncInfo} for the given resource that is not part of the
	 * resources with which the given {@link GitSynchronizeData} was created
	 * (which implies that the synchronization data for this resource is not yet
	 * cached).
	 *
	 * @param local
	 * @param base
	 * @param remote
	 * @param gsd
	 *            Data that describe the synchronization to perform
	 * @return a new and initialized instance of {@link GitSyncInfo} for the
	 *         given resource that was not part of the resources to synchronize.
	 *         Returns <code>null</code> if anything goes wrong (corrupted repo,
	 *         IO problem, ...)
	 */
	private SyncInfo getSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote, @NonNull GitSynchronizeData gsd) {
		GitSyncObjectCache repoCache = getCache().get(gsd.getRepository());
		if (repoCache == null) {
			return null;
		}
		if (!isLoaded) {
			isLoaded = true;
			isValid = GitSyncCache.loadDataFromGit(gsd, null, repoCache);
		}
		if (isValid) {
			try {
				return getSyncInfo(local, base, remote, gsd.getRepository());
			} catch (TeamException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return CoreText.GitLazyResourceVariantTreeSubscriber_name;
	}

	@Override
	public IResource[] roots() {
		List<IResource> projects = new ArrayList<>();
		try {
			for (GitSynchronizeData data : getDataSet()) {
				projects.addAll(Arrays.asList(ProjectUtil
						.getValidOpenProjects(data.getRepository())));
			}
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
		}
		return projects.toArray(new IResource[projects.size()]);
	}

}
