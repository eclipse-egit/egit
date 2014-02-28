/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *     François Rey - gracefully ignore linked resources
 *     Laurent Goubet <laurent.goubet@obeo.fr> - 403363
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.merge.EmptyResourceVariantTreeProvider;
import org.eclipse.egit.core.internal.merge.GitResourceVariantTreeProvider;
import org.eclipse.egit.core.internal.merge.GitSyncInfoToDiffConverter;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.IResourceVariantTree;
import org.eclipse.team.core.variants.ResourceVariantTreeSubscriber;

/**
 *
 */
@SuppressWarnings("restriction")
public class GitResourceVariantTreeSubscriber extends
		ResourceVariantTreeSubscriber {
	/**
	 * The {@link #variantTreeProvider} cannot be <code>null</code> since this
	 * is also used during asynchronous refresh calls from workspace listeners
	 * installed by Team during the creation of merge or synchronization
	 * context. However, {@link #dispose()} and
	 * {@link #reset(GitSynchronizeDataSet)} are called specifically to clean
	 * this provider. This specific, empty resource variant tree provider will
	 * be used as a "null object" to prevent NullPointerExceptions during these
	 * asynchronous calls after we've been disposed.
	 */
	private static final GitResourceVariantTreeProvider EMPTY_TREE_PROVIDER = new EmptyResourceVariantTreeProvider();

	private GitResourceVariantTreeProvider variantTreeProvider;

	private GitSynchronizeDataSet gsds;

	private GitSyncCache cache;

	private GitSyncInfoToDiffConverter syncInfoConverter;

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
			variantTreeProvider = new SynchronizeDataTreeProvider(cache, gsds);
			syncInfoConverter = new GitSyncInfoToDiffConverter(
					variantTreeProvider);
		} finally {
			monitor.done();
		}
	}

	@Override
	public boolean isSupervised(IResource res) throws TeamException {
		return IResource.FILE == res.getType()
				&& gsds.contains(res.getProject())
				&& gsds.shouldBeIncluded(res);
	}

	/**
	 * Returns all members of git repository (including those that are not
	 * imported into workspace)
	 *
	 * @param res
	 */
	@Override
	public IResource[] members(IResource res) throws TeamException {
		if (res.getType() == IResource.FILE || !gsds.shouldBeIncluded(res))
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
				cache = GitSyncCache.getAllData(gsds, monitor);
				variantTreeProvider = new SynchronizeDataTreeProvider(cache,
						gsds);
				syncInfoConverter = new GitSyncInfoToDiffConverter(
						variantTreeProvider);
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
			GitSyncCache.mergeAllDataIntoCache(updateRequests, monitor, cache);

			// and reset the variant tree (variants are lazily fetched
			// on-demand)
			variantTreeProvider = new SynchronizeDataTreeProvider(cache, gsds);
			syncInfoConverter = new GitSyncInfoToDiffConverter(
					variantTreeProvider);
		}

		super.refresh(resources, depth, monitor);
	}

	@Override
	public IResource[] roots() {
		final Set<IResource> roots = variantTreeProvider.getRoots();
		return roots.toArray(new IResource[roots.size()]);
	}

	/**
	 * @param data
	 */
	public void reset(GitSynchronizeDataSet data) {
		gsds = data;
		variantTreeProvider = EMPTY_TREE_PROVIDER;
		syncInfoConverter = null;
	}

	/**
	 * Disposes nested resources
	 */
	public void dispose() {
		if (variantTreeProvider != EMPTY_TREE_PROVIDER) {
			if (variantTreeProvider.getBaseTree() instanceof GitResourceVariantTree)
				((GitResourceVariantTree) variantTreeProvider.getBaseTree())
						.dispose();
			if (variantTreeProvider.getRemoteTree() instanceof GitResourceVariantTree)
				((GitResourceVariantTree) variantTreeProvider.getRemoteTree())
						.dispose();
			if (variantTreeProvider.getSourceTree() instanceof GitResourceVariantTree)
				((GitResourceVariantTree) variantTreeProvider.getSourceTree())
						.dispose();
			variantTreeProvider = EMPTY_TREE_PROVIDER;
		}
		gsds.dispose();
	}

	@Override
	public IDiff getDiff(IResource resource) throws CoreException {
		final GitSynchronizeData syncData = gsds.getData(resource.getProject());
		if (syncData == null || syncData.shouldIncludeLocal())
			return super.getDiff(resource);

		SyncInfo info = getSyncInfo(resource);
		if (info == null || info.getKind() == SyncInfo.IN_SYNC)
			return null;
		return syncInfoConverter.getDeltaFor(info);
	}

	@Override
	public String getName() {
		return CoreText.GitBranchResourceVariantTreeSubscriber_gitRepository;
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return new GitResourceVariantComparator(gsds);
	}

	/**
	 * As opposed to the other repository providers, EGit allows for
	 * synchronization between three remote branches. This will return the
	 * "source" tree for such synchronization use cases.
	 *
	 * @return The source tree of this subscriber.
	 * @since 3.0
	 */
	protected IResourceVariantTree getSourceTree() {
		return variantTreeProvider.getSourceTree();
	}

	/**
	 * This can be used to retrieve the version of the given resource
	 * corresponding to the source tree of this subscriber.
	 *
	 * @param resource
	 *            The resource for which we need a variant.
	 * @return The revision of the given resource cached in the source tree of
	 *         this subscriber.
	 * @throws TeamException
	 * @since 3.0
	 */
	public IFileRevision getSourceFileRevision(IFile resource)
			throws TeamException {
		return syncInfoConverter.getLocalFileRevision(resource);
	}

	@Override
	protected IResourceVariantTree getBaseTree() {
		return variantTreeProvider.getBaseTree();
	}

	@Override
	protected IResourceVariantTree getRemoteTree() {
		return variantTreeProvider.getRemoteTree();
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
