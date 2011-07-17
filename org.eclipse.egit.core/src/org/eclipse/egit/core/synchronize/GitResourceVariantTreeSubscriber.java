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

import static org.eclipse.jgit.lib.Repository.stripWorkDir;
import static org.eclipse.team.core.Team.isIgnoredHint;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.CoreText;
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
	private IResourceVariantTree remoteTree;

	/**
	 * A resource variant tree against HEAD.
	 */
	private IResourceVariantTree baseTree;

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
				&& gsds.contains(res.getProject()) && !isIgnoredHint(res);
	}

	/**
	 * Returns all members of git repository (including those that are not
	 * imported into workspace)
	 *
	 * @param res
	 */
	@Override
	public IResource[] members(IResource res) throws TeamException {
		if (res.getType() == IResource.FILE)
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
		init(monitor);
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
}