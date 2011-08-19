/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.ObjectId.zeroId;
import static org.eclipse.jgit.lib.Repository.stripWorkDir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.CoreText;
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

	private final GitSynchronizeDataSet gsds;

	private final Map<IResource, IResourceVariant> cache = new HashMap<IResource, IResourceVariant>();


	GitResourceVariantTree(ResourceVariantByteStore store,
			GitSyncCache gitCache, GitSynchronizeDataSet gsds) {
		super(store);
		this.gsds = gsds;
		this.gitCache = gitCache;
	}

	public IResource[] roots() {
		Set<IResource> roots = new HashSet<IResource>();
		for (GitSynchronizeData gsd : gsds)
			if (gsd.getPathFilter() == null)
				roots.addAll(gsd.getProjects());
			else
				for (IContainer container : gsd.getIncludedPaths())
					roots.add(container.getProject());

		return roots.toArray(new IResource[roots.size()]);
	}

	@Override
	protected IResourceVariant fetchVariant(IResource resource, int depth,
			IProgressMonitor monitor) throws TeamException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		if (resource == null || resource.getLocation() == null) {
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

		if (cache.containsKey(resource))
			return cache.get(resource);

		GitSynchronizeData gsd = gsds.getData(resource.getProject());
		if (gsd == null)
			return null;

		Repository repo = gsd.getRepository();
		String path = getPath(resource, repo);

		GitSyncObjectCache syncCache = gitCache.get(repo);
		GitSyncObjectCache cachedData = syncCache.get(path);
		if (cachedData == null)
			return null;

		ObjectId objectId;
		if (cachedData.getDiffEntry() != null)
			objectId = getObjectId(cachedData.getDiffEntry());
		else
			return null;

		IResourceVariant variant = null;
		if (!objectId.equals(zeroId())) {
			if (resource.getType() == IResource.FILE)
				variant = new GitRemoteFile(repo, getCommitId(gsd), objectId,
						path);
			else
				variant = new GitRemoteFolder(repo, cachedData,
						getCommitId(gsd), objectId, path);

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

	public IResourceVariant getResourceVariant(final IResource resource)
			throws TeamException {
		return fetchVariant(resource, 0, null);
	}

	private String getPath(final IResource resource, Repository repo) {
		return stripWorkDir(repo.getWorkTree(), resource.getLocation().toFile());
	}

}
