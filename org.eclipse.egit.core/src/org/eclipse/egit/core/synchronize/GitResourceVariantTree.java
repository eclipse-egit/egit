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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.core.variants.ResourceVariantTree;

abstract class GitResourceVariantTree extends ResourceVariantTree {

	private final GitSynchronizeDataSet gsds;

	private final Map<IResource, IResourceVariant> cache = new HashMap<IResource, IResourceVariant>();

	GitResourceVariantTree(ResourceVariantByteStore store,
			GitSynchronizeDataSet gsds) {
		super(store);
		this.gsds = gsds;
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
			return fetchVariant(resource, subMonitor);
		} finally {
			subMonitor.done();
		}
	}

	private IResourceVariant fetchVariant(IResource resource,
			IProgressMonitor monitor) throws TeamException {
		if (cache.containsKey(resource))
			return cache.get(resource);

		GitSynchronizeData gsd = gsds.getData(resource.getProject());
		if (gsd == null)
			return null;

		Repository repo = gsd.getRepository();
		String path = getPath(resource, repo);
		RevCommit revCommit = getRevCommit(gsd);
		if (revCommit == null)
			return null;

		if (path.length() == 0)
			return handleRepositoryRoot(resource, repo, revCommit);

		try {
			if (monitor.isCanceled())
				throw new OperationCanceledException();

			TreeWalk tw = initializeTreeWalk(repo, path);

			int nth = tw.addTree(revCommit.getTree());
			IResourceVariant variant = null;
			if (resource.getType() == IResource.FILE) {
				tw.setRecursive(true);
				if (tw.next() && !tw.getObjectId(nth).equals(zeroId()))
					variant = new GitRemoteFile(repo, revCommit,
							tw.getObjectId(nth), path);
			} else {
				while (tw.next() && !path.equals(tw.getPathString())) {
					if (monitor.isCanceled())
						throw new OperationCanceledException();

					if (tw.isSubtree())
						tw.enterSubtree();
				}

				ObjectId objectId = tw.getObjectId(nth);
				if (!objectId.equals(zeroId()))
					variant = new GitRemoteFolder(repo, revCommit, objectId, path);
			}
			if (variant != null)
				cache.put(resource, variant);
			return variant;
		} catch (IOException e) {
			throw new TeamException(
					NLS.bind(
							CoreText.GitResourceVariantTree_couldNotFindResourceVariant,
							resource), e);
		}
	}

	@Override
	protected IResourceVariant[] fetchMembers(IResourceVariant variant,
			IProgressMonitor progress) throws TeamException {
		if (variant == null || !(variant instanceof GitRemoteFolder))
			return new IResourceVariant[0];

		GitRemoteFolder gitVariant = (GitRemoteFolder) variant;

		try {
			return gitVariant.members(progress);
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.GitResourceVariantTree_couldNotFetchMembers,
					gitVariant), e);
		} finally {
			progress.done();
		}
	}

	public IResourceVariant getResourceVariant(final IResource resource)
			throws TeamException {
		return fetchVariant(resource, 0, null);
	}

	/**
	 *
	 * @param gsd
	 * @return instance of {@link RevTree} for given {@link GitSynchronizeData}
	 *         or <code>null</code> if rev tree was not found
	 * @throws TeamException
	 */
	protected abstract RevCommit getRevCommit(GitSynchronizeData gsd)
			throws TeamException;

	private IResourceVariant handleRepositoryRoot(final IResource resource,
			Repository repo, RevCommit revCommit) {
		String path = RepositoryMapping.findRepositoryMapping(repo)
				.getRepoRelativePath(resource);
		return new GitRemoteFolder(repo, revCommit, revCommit.getTree(), path);
	}

	private TreeWalk initializeTreeWalk(Repository repo, String path) {
		TreeWalk tw = new TreeWalk(repo);
		tw.reset();

		tw.setFilter(PathFilter.create(path));

		return tw;
	}

	private String getPath(final IResource resource, Repository repo) {
		return stripWorkDir(repo.getWorkTree(), resource.getLocation().toFile());
	}

}
