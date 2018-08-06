/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
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
 *     François Rey - gracefully ignore linked resources
 *     Laurent Goubet <laurent.goubet@obeo.fr> - 403363
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.Repository.stripWorkDir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.storage.WorkspaceFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.core.diff.provider.ThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.provider.ResourceDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.IResourceVariantTree;
import org.eclipse.team.core.variants.ResourceVariantTreeSubscriber;
import org.eclipse.team.internal.core.mapping.ResourceVariantFileRevision;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;

/**
 *
 */
@SuppressWarnings("restriction")
public class GitResourceVariantTreeSubscriber extends
		ResourceVariantTreeSubscriber {

	/** A resource variant tree of the source branch. */
	private GitSourceResourceVariantTree sourceTree;

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

	private GitSyncInfoToDiffConverter syncInfoConverter = new GitSyncInfoToDiffConverter();

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
		return gsds.contains(res.getProject()) && gsds.shouldBeIncluded(res);
	}

	/**
	 * Returns all members of the given resource as recorded by git. Resources
	 * ignored by git via .gitignore will not be returned, even if they exist in
	 * the workspace.
	 *
	 * @param res
	 *            the resource to get the members of
	 * @return the resources, which may or may not exist in the workspace
	 */
	@Override
	public IResource[] members(IResource res) throws TeamException {
		if (res.getType() == IResource.FILE || !gsds.shouldBeIncluded(res)) {
			return new IResource[0];
		}
		GitSynchronizeData gsd = gsds.getData(res.getProject());
		Repository repo = gsd.getRepository();
		GitSyncObjectCache repoCache = cache.get(repo);

		Collection<IResource> allMembers = new ArrayList<>();
		Map<String, IResource> existingMembers = new HashMap<>();

		String path = stripWorkDir(repo.getWorkTree(), res.getLocation().toFile());
		GitSyncObjectCache cachedMembers = repoCache.get(path);
		// A normal synchronizer would just return the union of existing
		// resources and non-existing ones that exist only in git. For git,
		// however, we want to ignore .gitignored resources completely, and
		// include untracked files only if the preference to do so is set
		// (in which case the cache will contain them already). So we add
		// only the existing ones that are also recorded in the git 3-way
		// cache, plus those recorded only in git, plus the git recorded
		// one if it's a file vs.folder conflict.
		try {
			IContainer container = (IContainer) res;
			// Existing resources
			if (container.exists()) {
				for (IResource member : container.members()) {
					existingMembers.put(member.getName(), member);
				}
			}

			// Now add the ones from git
			if (cachedMembers != null) {
				Collection<GitSyncObjectCache> members = cachedMembers
						.members();
				if (members != null) {
					for (GitSyncObjectCache gitMember : members) {
						String name = gitMember.getName();
						IResource existing = existingMembers.get(name);
						if (existing != null) {
							allMembers.add(existing);
						}
						if (existing == null || (existing
								.getType() != IResource.FILE) != gitMember
										.getDiffEntry().isTree()) {
							// Non-existing, or file vs. folder
							IPath localPath = new Path(name);
							if (gitMember.getDiffEntry().isTree()) {
								allMembers.add(container.getFolder(localPath));
							} else {
								allMembers.add(container.getFile(localPath));
							}
						}
					}
				}
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}

		return allMembers.toArray(new IResource[0]);
	}

	@Override
	public void refresh(IResource[] resources, int depth,
			IProgressMonitor monitor) throws TeamException {
		for (IResource resource : resources) {
			// check to see if there is a full refresh
			if (resource.getType() == IResource.ROOT) {
				// refresh entire cache
				cache = GitSyncCache.getAllData(gsds, monitor);
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
		}

		super.refresh(resources, depth, monitor);
	}

	@Override
	public IResource[] roots() {
		if (roots == null)
			roots = gsds.getAllProjects();
		if (roots == null)
			return new IResource[0];
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
		sourceTree = null;
		baseTree = null;
		remoteTree = null;
	}

	/**
	 * Disposes nested resources
	 */
	public void dispose() {
		if (sourceTree != null)
			sourceTree.dispose();
		if (baseTree != null)
			baseTree.dispose();
		if (remoteTree != null)
			remoteTree.dispose();
		gsds.dispose();
	}

	/**
	 * Provide the synchronize data set.
	 *
	 * @return The {@link GitSynchronizeDataSet} used by this subscriber.
	 */
	protected GitSynchronizeDataSet getDataSet() {
		return gsds;
	}

	/**
	 * Provide the synchronization cache.
	 *
	 * @return The {@link GitSyncCache} used by this subscriber.
	 */
	protected GitSyncCache getCache() {
		return cache;
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

	/**
	 * The default implementation of SyncInfoToDiffConverter uses inaccurate
	 * information with regards to some of EGit features.
	 * <p>
	 * SyncInfoToDiffConverter#asFileRevision(IResourceVariant) is called when a
	 * user double-clicks a revision from the synchronize view (among others).
	 * However, the default implementation returns an IFileRevision with no
	 * comment, author or timestamp information (this can be observed by
	 * commenting this implementation out and launching
	 * HistoryTest#queryHistoryThroughTeam()).
	 * </p>
	 * <p>
	 * SyncInfoToDiffConverter#getDeltaFor(SyncInfo) had been originally thought
	 * by Team to be used for synchronizations that considered local changes.
	 * This is not always the case with EGit. For example, a user might try and
	 * compare two refs together from the Git repository explorer (right click >
	 * synchronize with each other). In such a case, the local files must not be
	 * taken into account (i.e. we must respect the value of our
	 * GitSynchronizeData#shouldIncludeLocal(). Most of the private methods here
	 * were copy/pasted from the super implementation.
	 * </p>
	 */
	private class GitSyncInfoToDiffConverter extends SyncInfoToDiffConverter {
		@Override
		public IDiff getDeltaFor(SyncInfo info) {
			if (info.getComparator().isThreeWay()) {
				ITwoWayDiff local = getLocalDelta(info);
				ITwoWayDiff remote = getRemoteDelta(info);
				return new ThreeWayDiff(local, remote);
			} else {
				if (info.getKind() != SyncInfo.IN_SYNC) {
					IResourceVariant remote = info.getRemote();
					IResource local = info.getLocal();

					int kind;
					if (remote == null)
						kind = IDiff.REMOVE;
					else if (!local.exists())
						kind = IDiff.ADD;
					else
						kind = IDiff.CHANGE;

					if (local.getType() == IResource.FILE) {
						IFileRevision after = asFileState(remote);
						IFileRevision before = getLocalFileRevision((IFile) local);
						return new ResourceDiff(info.getLocal(), kind, 0,
								before, after);
					}
					// For folders, we don't need file states
					return new ResourceDiff(info.getLocal(), kind);
				}
				return null;
			}
		}

		private ITwoWayDiff getLocalDelta(SyncInfo info) {
			int direction = SyncInfo.getDirection(info.getKind());
			if (direction == SyncInfo.OUTGOING
					|| direction == SyncInfo.CONFLICTING) {
				IResourceVariant ancestor = info.getBase();
				IResource local = info.getLocal();

				int kind;
				if (ancestor == null)
					kind = IDiff.ADD;
				else if (!local.exists())
					kind = IDiff.REMOVE;
				else
					kind = IDiff.CHANGE;

				if (local.getType() == IResource.FILE) {
					IFileRevision before = asFileState(ancestor);
					IFileRevision after = getLocalFileRevision((IFile) local);
					return new ResourceDiff(info.getLocal(), kind, 0, before,
							after);
				}
				// For folders, we don't need file states
				return new ResourceDiff(info.getLocal(), kind);

			}
			return null;
		}

		/**
		 * Depending on the Synchronize data, this will return either the local
		 * file or the "source" revision.
		 *
		 * @param local
		 *            The local file.
		 * @return The file revision that should be considered for the local
		 *         (left) side a delta
		 */
		protected IFileRevision getLocalFileRevision(IFile local) {
			final GitSynchronizeData data = gsds.getData(local.getProject());
			if (data.shouldIncludeLocal())
				return new WorkspaceFileRevision(local);

			try {
				return asFileState(getSourceTree().getResourceVariant(local));
			} catch (TeamException e) {
				String error = NLS
						.bind(CoreText.GitResourceVariantTreeSubscriber_CouldNotFindSourceVariant,
								local.getName());
				Activator.logError(error, e);
				// fall back to the working tree version
				return new WorkspaceFileRevision(local);
			}
		}

		/*
		 * copy-pasted from the private implementation in
		 * SyncInfoToDiffConverter
		 */
		private ITwoWayDiff getRemoteDelta(SyncInfo info) {
			int direction = SyncInfo.getDirection(info.getKind());
			if (direction == SyncInfo.INCOMING
					|| direction == SyncInfo.CONFLICTING) {
				IResourceVariant ancestor = info.getBase();
				IResourceVariant remote = info.getRemote();

				int kind;
				if (ancestor == null)
					kind = IDiff.ADD;
				else if (remote == null)
					kind = IDiff.REMOVE;
				else
					kind = IDiff.CHANGE;

				// For folders, we don't need file states
				if (info.getLocal().getType() == IResource.FILE) {
					IFileRevision before = asFileState(ancestor);
					IFileRevision after = asFileState(remote);
					return new ResourceDiff(info.getLocal(), kind, 0, before,
							after);
				}

				return new ResourceDiff(info.getLocal(), kind);
			}
			return null;
		}

		/*
		 * copy-pasted from the private implementation in
		 * SyncInfoToDiffConverter
		 */
		private IFileRevision asFileState(final IResourceVariant variant) {
			if (variant == null)
				return null;
			return asFileRevision(variant);
		}

		@Override
		protected ResourceVariantFileRevision asFileRevision(
				IResourceVariant variant) {
			return new GitResourceVariantFileRevision(variant);
		}
	}

	/**
	 * The default implementation of ResourceVariantFileRevision has no author,
	 * comment, timestamp... or any information that could be provided by the
	 * Git resource variant. This implementation uses the variant's information.
	 */
	private static class GitResourceVariantFileRevision extends
			ResourceVariantFileRevision {
		private final IResourceVariant variant;

		public GitResourceVariantFileRevision(IResourceVariant variant) {
			super(variant);
			this.variant = variant;
		}

		@Override
		public String getContentIdentifier() {
			// Use the same ID as would CommitFileRevision
			if (variant instanceof GitRemoteResource)
				return ((GitRemoteResource) variant).getCommitId().getId()
						.getName();

			return super.getContentIdentifier();
		}

		@Override
		public long getTimestamp() {
			if (variant instanceof GitRemoteResource) {
				final PersonIdent author = ((GitRemoteResource) variant)
						.getCommitId().getAuthorIdent();
				if (author != null)
					return author.getWhen().getTime();
			}

			return super.getTimestamp();
		}

		@Override
		public String getAuthor() {
			if (variant instanceof GitRemoteResource) {
				final PersonIdent author = ((GitRemoteResource) variant)
						.getCommitId().getAuthorIdent();
				if (author != null)
					return author.getName();
			}

			return super.getAuthor();
		}

		@Override
		public String getComment() {
			if (variant instanceof GitRemoteResource)
				return ((GitRemoteResource) variant).getCommitId()
						.getFullMessage();

			return super.getComment();
		}
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
		if (sourceTree == null)
			sourceTree = new GitSourceResourceVariantTree(cache, gsds);

		return sourceTree;
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
		return getSyncInfo(local, base, remote, repo);
	}

	/**
	 * Provide a new and initialized {@link SyncInfo} for the given 'local'
	 * resource from a known repository.
	 *
	 * @param local
	 * @param base
	 * @param remote
	 * @param repo
	 *            Repository to load data from
	 * @return This implementation returns a new instance of {@link GitSyncInfo}
	 * @throws TeamException
	 */
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote, Repository repo) throws TeamException {
		SyncInfo info = new GitSyncInfo(local, base, remote,
				getResourceComparator(), cache.get(repo), repo);
		info.init();
		return info;
	}

}
