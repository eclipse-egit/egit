/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *     Laurent Goubet <laurent.goubet@obeo.fr> - 403363
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.egit.core.internal.util.ResourceUtil.isNonWorkspace;
import static org.eclipse.jgit.lib.ObjectId.zeroId;
import static org.eclipse.jgit.lib.Repository.stripWorkDir;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.core.variants.ResourceVariantTree;

abstract class GitResourceVariantTree extends ResourceVariantTree {

	private final GitSyncCache gitCache;

	private final Map<IResource, IResourceVariant> cache = Collections
			.synchronizedMap(new WeakHashMap<IResource, IResourceVariant>());

	protected final GitSynchronizeDataSet gsds;

	GitResourceVariantTree(ResourceVariantByteStore store,
			GitSyncCache gitCache, GitSynchronizeDataSet gsds) {
		super(store);
		this.gsds = gsds;
		this.gitCache = gitCache;
	}

	@Override
	public IResource[] roots() {
		Set<IResource> roots = new HashSet<IResource>();
		for (GitSynchronizeData gsd : gsds)
			if (gsd.getPathFilter() == null)
				roots.addAll(gsd.getProjects());
			else
				for (IResource resource : gsd.getIncludedResources())
					roots.add(resource.getProject());

		return roots.toArray(new IResource[roots.size()]);
	}

	/**
	 * Disposes all nested resources
	 */
	public void dispose() {
		if (gsds != null)
			gsds.dispose();

		cache.clear();
	}

	@Override
	protected IResourceVariant fetchVariant(IResource resource, int depth,
			IProgressMonitor monitor) throws TeamException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		if (resource == null || isNonWorkspace(resource)) {
			subMonitor.done();
			return null;
		}

		subMonitor.beginTask(NLS.bind(
				CoreText.GitResourceVariantTree_fetchingVariant,
				resource.getName()), IProgressMonitor.UNKNOWN);
		try {
			return fetchVariant(resource);
		} finally {
			subMonitor.done();
		}
	}

	private IResourceVariant fetchVariant(IResource resource) {
		if (gitCache == null)
			return null;

		IResourceVariant cachedVariant = cache.get(resource);
		if (cachedVariant != null)
			return cachedVariant;

		GitSynchronizeData gsd = gsds.getData(resource.getProject());
		if (gsd == null)
			return null;

		Repository repo = gsd.getRepository();
		String path = getPath(resource, repo);

		GitSyncObjectCache syncCache = gitCache.get(repo);
		GitSyncObjectCache cachedData = syncCache.get(path);
		if (cachedData == null)
			return null;

		IResourceVariant variant = null;
		ObjectId objectId = getObjectId(cachedData.getDiffEntry());
		if (!objectId.equals(zeroId())) {
			if (resource.getType() == IResource.FILE) {
				variant = new GitRemoteFile(repo, getCommitId(gsd), objectId,
						path, cachedData.getDiffEntry().getMetadata());
			} else {
				variant = new GitRemoteFolder(repo, cachedData,
						getCommitId(gsd), objectId, path);
			}
			cache.put(resource, variant);
		}

		return variant;
	}

	protected abstract ObjectId getObjectId(ThreeWayDiffEntry diffEntry);

	protected abstract RevCommit getCommitId(GitSynchronizeData gsd);

	@Override
	protected IResourceVariant[] fetchMembers(IResourceVariant variant,
			IProgressMonitor progress) throws TeamException {
		if (variant == null || !(variant instanceof GitRemoteFolder))
			return new IResourceVariant[0];

		GitRemoteFolder gitVariant = (GitRemoteFolder) variant;

		try {
			return gitVariant.members(progress);
		} finally {
			progress.done();
		}
	}

	@Override
	public IResourceVariant getResourceVariant(final IResource resource)
			throws TeamException {
		return fetchVariant(resource, 0, null);
	}

	private String getPath(final IResource resource, Repository repo) {
		return stripWorkDir(repo.getWorkTree(), resource.getLocation().toFile());
	}

}
