/*******************************************************************************
 * Copyright (c) 2010-2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *     François Rey - gracefully ignore linked resources
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.Repository.stripWorkDir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.IResourceVariantTree;
import org.eclipse.team.core.variants.ResourceVariantTreeSubscriber;

/**
 *
 */
public class GitResourceVariantTreeSubscriber extends
		ResourceVariantTreeSubscriber {

	/**
	 * A resource variant tree of the remote branch(es).
	 */
	private GitRemoteResourceVariantTree remoteTree;

	/**
	 * A resource variant tree against HEAD.
	 */
	private GitBaseResourceVariantTree baseTree;

	private GitSynchronizeDataSet gsds;

	private IResource[] roots;

	private GitSyncCache cache;

	/**
	 * @param data
	 */
	public GitResourceVariantTreeSubscriber(GitSynchronizeDataSet data) {
		this.gsds = data;
	}

	/**
	 * Initialize git subscriber. This method will pre-fetch data from git
	 * repository. This approach will reduce number of {@link TreeWalk}'s
	 * created during synchronization
	 *
	 * @param monitor
	 */
	public void init(IProgressMonitor monitor) {
		monitor.beginTask(
				CoreText.GitResourceVariantTreeSubscriber_fetchTaskName,
				gsds.size());
		try {
			cache = GitSyncCache.getAllData(gsds, monitor);
		} finally {
			monitor.done();
		}
	}

	@Override
	public boolean isSupervised(IResource res) throws TeamException {
		return IResource.FILE == res.getType()
				&& gsds.contains(res.getProject()) && shouldBeIncluded(res);
	}

	/**
	 * Returns all members of git repository (including those that are not
	 * imported into workspace)
	 *
	 * @param res
	 */
	@Override
	public IResource[] members(IResource res) throws TeamException {
		if(res.getType() == IResource.FILE || !shouldBeIncluded(res))
			return new IResource[0];

		GitSynchronizeData gsd = gsds.getData(res.getProject());
		Repository repo = gsd.getRepository();
		GitSyncObjectCache repoCache = cache.get(repo);

		Set<IResource> gitMembers = new HashSet<IResource>();
		Map<String, IResource> allMembers = new HashMap<String, IResource>();

		Set<GitSyncObjectCache> gitCachedMembers = new HashSet<GitSyncObjectCache>();
		String path = stripWorkDir(repo.getWorkTree(), res.getLocation().toFile());
		GitSyncObjectCache cachedMembers = repoCache.get(path);
		if (cachedMembers != null) {
			Collection<GitSyncObjectCache> members = cachedMembers.members();
			if (members != null)
				gitCachedMembers.addAll(members);
		}
		try {
			for (IResource member : ((IContainer) res).members())
				allMembers.put(member.getName(), member);

			for (GitSyncObjectCache gitMember : gitCachedMembers) {
				IResource member = allMembers.get(gitMember.getName());
				if (member != null)
					gitMembers.add(member);
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}

		return gitMembers.toArray(new IResource[gitMembers.size()]);
	}

	@Override
	public void refresh(IResource[] resources, int depth,
			IProgressMonitor monitor) throws TeamException {
		for (IResource resource : resources) {
			// check to see if there is a full refresh
			if (resource.getType() == IResource.ROOT) {
				// refresh entire cache
				GitSyncCache newCache = GitSyncCache.getAllData(gsds, monitor);
				cache.merge(newCache);
				super.refresh(resources, depth, monitor);
				return;
			}
		}

		// not refreshing the workspace, locate and collect target resources
		Map<GitSynchronizeData, Collection<String>> updateRequests = new HashMap<GitSynchronizeData, Collection<String>>();
		for (IResource resource : resources) {
			IProject project = resource.getProject();
			GitSynchronizeData data = gsds.getData(project.getName());
			if (data != null) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(project);
				// mapping may be null if the project has been closed
				if (mapping != null) {
					Collection<String> paths = updateRequests.get(data);
					if (paths == null) {
						paths = new ArrayList<String>();
						updateRequests.put(data, paths);
					}

					String path = mapping.getRepoRelativePath(resource);
					// null path may be returned, check for this
					if (path == null)
						// unknown, force a refresh of the whole repository
						path = ""; //$NON-NLS-1$
					paths.add(path);
				}
			}
		}

		// scan only the repositories that were affected
		if (!updateRequests.isEmpty()) {
			// refresh cache
			GitSyncCache newCache = GitSyncCache.getAllData(updateRequests,
					monitor);
			cache.merge(newCache);
		}

		super.refresh(resources, depth, monitor);
	}

	@Override
	public IResource[] roots() {
		if (roots == null)
			roots = gsds.getAllProjects();
		IResource[] result = new IResource[roots.length];
		System.arraycopy(roots, 0, result, 0, roots.length);
		return result;
	}

	/**
	 * @param data
	 */
	public void reset(GitSynchronizeDataSet data) {
		gsds = data;

		roots = null;
		baseTree = null;
		remoteTree = null;
	}

	/**
	 * Disposes nested resources
	 */
	public void dispose() {
		if (baseTree != null)
			baseTree.dispose();
		if (remoteTree != null)
			remoteTree.dispose();
		gsds.dispose();
	}

	@Override
	public String getName() {
		return CoreText.GitBranchResourceVariantTreeSubscriber_gitRepository;
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return new GitResourceVariantComparator(gsds);
	}

	@Override
	protected IResourceVariantTree getBaseTree() {
		if (baseTree == null)
			baseTree = new GitBaseResourceVariantTree(cache, gsds);

		return baseTree;
	}

	@Override
	protected IResourceVariantTree getRemoteTree() {
		if (remoteTree == null)
			remoteTree = new GitRemoteResourceVariantTree(cache, gsds);

		return remoteTree;
	}

	@Override
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote) throws TeamException {

		Repository repo = gsds.getData(local.getProject()).getRepository();
		SyncInfo info = new GitSyncInfo(local, base, remote,
				getResourceComparator(), cache.get(repo), repo);

		info.init();
		return info;
	}

	private boolean shouldBeIncluded(IResource res) {
		if (res == null || res.isLinked(IResource.CHECK_ANCESTORS))
			return false;
		final IProject proj = res.getProject();
		if (proj == null)
			return false;
		final GitSynchronizeData d = gsds.getData(proj);
		if (d == null)
			return false;
		final Set<IContainer> includedPaths = d.getIncludedPaths();
		if (includedPaths == null)
			return true;

		IPath path = res.getLocation();
		for (IContainer container : includedPaths)
			if (container.getLocation().isPrefixOf(path))
				return true;

		return false;
	}

}
