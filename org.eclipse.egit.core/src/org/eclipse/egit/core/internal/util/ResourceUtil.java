/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, 2015 Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2012, Gunnar Wagenknecht <gunnar@wagenknecht.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.IgnoreInGitSynchronizations;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Resource utilities
 *
 */
public class ResourceUtil {
	// The id used to associate a provider with a project, see
	// TeamPlugin.PROVIDER_PROP_KEY
	private final static QualifiedName PROVIDER_PROP_KEY = new QualifiedName(
			"org.eclipse.team.core", "repository"); //$NON-NLS-1$ //$NON-NLS-2$

	// Our own session property to cache the provider ID of the configured
	// repository provider.
	private final static QualifiedName PROVIDER_ID = new QualifiedName(
			"org.eclipse.egit.core", "repositoryProviderID"); //$NON-NLS-1$ //$NON-NLS-2$

	// Value for PROVIDER_ID to mark unshared projects.
	private final static Object PROJECT_IS_UNSHARED = new Object();

	/**
	 * Return the corresponding resource if it exists and has the Git repository
	 * provider.
	 * <p>
	 * The returned file will be relative to the most nested non-closed
	 * Git-managed project.
	 *
	 * @param location
	 *            the path to check
	 * @param innerMost
	 *            check if there are multiple candidates in the workspace and
	 *            return innermost resource. <b>Note</b>, this check is
	 *            expensive and should not be used in performance critical code.
	 * @return the resources, or null
	 */
	@Nullable
	public static IResource getResourceForLocation(@NonNull IPath location, boolean innerMost) {
		IFile file = getFileForLocation(location, innerMost);
		if (file != null) {
			return file;
		}
		return getContainerForLocation(location, innerMost);
	}

	/**
	 * Return the corresponding file if it exists and has the Git repository
	 * provider.
	 * <p>
	 * If checkNested argument is true, the returned file will be relative to
	 * the most nested non-closed Git-managed project.
	 *
	 * @param location
	 * @param innerMost
	 *            check if there are multiple candidates in the workspace and
	 *            return innermost resource. <b>Note</b>, this check is
	 *            expensive and should not be used in performance critical code.
	 * @return the file, or null
	 */
	@Nullable
	public static IFile getFileForLocation(@NonNull IPath location,
			boolean innerMost) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile file = root.getFileForLocation(location);
		if (file == null) {
			return null;
		}
		if (!innerMost && isValid(file)) {
			return file;
		}
		URI uri = URIUtil.toURI(location);
		IFile file2 = getFileForLocationURI(root, uri);
		if (file2 == null && isValid(file)) {
			return file;
		}
		return file2;
	}

	/**
	 * sort out closed, linked or not shared resources
	 *
	 * @param resource
	 * @return true if the resource is shared with git, not a link and
	 *         accessible in Eclipse
	 */
	private static boolean isValid(@NonNull IResource resource) {
		return resource.isAccessible()
				&& !resource.isLinked(IResource.CHECK_ANCESTORS)
				&& isSharedWithGit(resource);
	}

	/**
	 * @param resource
	 *            non null
	 * @return true if the project is configured with git provider
	 */
	public static boolean isSharedWithGit(@NonNull IResource resource) {
		IProject project = resource.getProject();
		if (project == null || !project.isAccessible()) {
			return false;
		}
		try {
			// Look for an existing provider
			GitProvider provider = lookupProviderProp(project);
			if (provider != null || MappingJob.isKnownGitProject(project)) {
				// Is mapped or in the process of being mapped.
				return true;
			} else if (isMarkedAsNotSharedWithGit(project)) {
				return false;
			}
			// There isn't one so check the persistent property
			String existingID = project
					.getPersistentProperty(PROVIDER_PROP_KEY);
			if (existingID == null) {
				markAsUnshared(project);
			} else {
				markAsShared(project, existingID);
			}
			boolean isGitProvider = GitProvider.ID.equals(existingID);
			if (isGitProvider) {
				MappingJob.initProviderAsynchronously(project);
			}
			return isGitProvider;
		} catch (CoreException e) {
			Activator.getDefault().getLog().log(e.getStatus());
			return false;
		}
	}

	private static class MappingJob extends Job {

		private final static MappingJob INSTANCE = new MappingJob();

		public static void initProviderAsynchronously(
				@NonNull IProject project) {
			synchronized (INSTANCE.projects) {
				if (!INSTANCE.projects.add(project)) {
					return;
				}
			}
			INSTANCE.schedule();
		}

		public static boolean isKnownGitProject(@NonNull IProject project) {
			synchronized (INSTANCE.projects) {
				return INSTANCE.projects.contains(project);
			}
		}

		HashSet<IProject> projects = new LinkedHashSet<>();

		public MappingJob() {
			super(CoreText.ResourceUtil_mapProjectJob);
			setSystem(true);
			setUser(false);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			HashSet<IProject> work;
			synchronized (projects) {
				work = new LinkedHashSet<>(projects);
			}

			for (IProject project : work) {
				if (monitor.isCanceled()) {
					break;
				}
				// this will instantiate and map the provider (can lock!)
				RepositoryProvider.getProvider(project, GitProvider.ID);
			}

			synchronized (projects) {
				if (monitor.isCanceled()) {
					projects.clear();
				} else {
					projects.removeAll(work);
				}
				if (!projects.isEmpty()) {
					schedule();
				}
			}
			return Status.OK_STATUS;
		}
	}

	/**
	 * Returns git provider if associated with the given project or
	 * <code>null</code> if the project is not associated with a provider or the
	 * provider is not fully loaded yet. To check if the git provider is
	 * associated with the project, use {@link #isSharedWithGit(IResource)}.
	 *
	 * @param project
	 *            the project to query for a provider
	 * @return the repository provider or null
	 */
	@Nullable
	final public static GitProvider getGitProvider(@NonNull IProject project) {
		if (!project.isAccessible()) {
			return null;
		}
		try {
			// Look for an existing provider
			GitProvider provider = lookupProviderProp(project);
			if (provider != null) {
				return provider;
			}
			if (MappingJob.isKnownGitProject(project)
					|| isMarkedAsNotSharedWithGit(project)) {
				// Is in the process of being mapped, but isn't mapped yet, or
				// isn't shared with us at all.
				return null;
			}
			String existingID = project
					.getPersistentProperty(PROVIDER_PROP_KEY);
			if (existingID == null) {
				markAsUnshared(project);
			} else {
				markAsShared(project, existingID);
				boolean isGitProvider = GitProvider.ID.equals(existingID);
				if (isGitProvider) {
					MappingJob.initProviderAsynchronously(project);
				}
			}
			// not loaded yet, but we can't load it because it will use locks
			// or not a GitProvider
			return null;
		} catch (CoreException e) {
			Activator.getDefault().getLog().log(e.getStatus());
		}
		return null;
	}

	/*
	 * Return the provider mapped to project, or null if none;
	 */
	@Nullable
	private static GitProvider lookupProviderProp(IProject project)
			throws CoreException {
		Object provider = project.getSessionProperty(PROVIDER_PROP_KEY);
		if (provider != null) {
			if (provider instanceof RepositoryProvider) {
				markAsShared(project, ((RepositoryProvider) provider).getID());
				if (provider instanceof GitProvider) {
					return (GitProvider) provider;
				}
			} else {
				// Must be the RepositoryProvider's NOT_MAPPED marker
				markAsUnshared(project);
			}
		}
		return null;
	}

	/**
	 * Sets the session property {@link #PROVIDER_ID} to
	 * {@link #PROJECT_IS_UNSHARED} to indicate that the project is not shared
	 * at all.
	 *
	 * @param project
	 *            to mark
	 */
	private static void markAsUnshared(@NonNull IProject project) {
		try {
			project.setSessionProperty(PROVIDER_ID, PROJECT_IS_UNSHARED);
		} catch (CoreException e) {
			// Ignore since this is "only" an optimization
		}
	}

	/**
	 * Sets the session property {@link #PROVIDER_ID} to the given
	 * {@code providerId}, or removes the property if the id is {@code null}.
	 *
	 * @param project
	 *            to mark
	 * @param providerId
	 *            Id of the {@link RepositoryProvider} associated with the
	 *            project, if known, or {@code null} otherwise.
	 */
	private static void markAsShared(@NonNull IProject project,
			@Nullable String providerId) {
		try {
			project.setSessionProperty(PROVIDER_ID, providerId);
		} catch (CoreException e) {
			// Ignore since this is "only" an optimization
		}
	}

	/**
	 * Tests the session property {@link #PROVIDER_ID}.
	 *
	 * @param project
	 *            to test
	 * @return {@code true} is the project is marked as known to be unshared
	 */
	private static boolean isMarkedAsNotSharedWithGit(
			@NonNull IProject project) {
		try {
			Object property = project.getSessionProperty(PROVIDER_ID);
			if (property == PROJECT_IS_UNSHARED) {
				return true;
			} else if (property instanceof String
					&& !GitProvider.ID.equals(property)) {
				return true;
			}
		} catch (CoreException e) {
			// Ignore and fall through
		}
		return false;
	}

	/**
	 * Return the corresponding container if it exists and has the Git
	 * repository provider.
	 * <p>
	 * The returned container will be relative to the most nested non-closed
	 * Git-managed project.
	 *
	 * @param location
	 * @param innerMost
	 *            check if there are multiple candidates in the workspace and
	 *            return innermost resource. <b>Note</b>, this check is
	 *            expensive and should not be used in performance critical code.
	 * @return the container, or null
	 */
	@Nullable
	public static IContainer getContainerForLocation(@NonNull IPath location,
			boolean innerMost) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer dir = root.getContainerForLocation(location);
		if (dir == null) {
			return null;
		}
		if (!innerMost && isValid(dir)) {
			return dir;
		}
		URI uri = URIUtil.toURI(location);
		IContainer dir2 = getContainerForLocationURI(root, uri);
		if (dir2 == null && isValid(dir)) {
			return dir;
		}
		return dir2;
	}

	/**
	 * Get the {@link IFile} corresponding to the arguments if it exists and has
	 * the Git repository provider.
	 * <p>
	 * The returned file will be relative to the most nested non-closed
	 * Git-managed project.
	 *
	 * @param repository
	 *            the repository of the file
	 * @param repoRelativePath
	 *            the repository-relative path of the file to search for
	 * @param innerMost
	 *            check if there are multiple candidates in the workspace and
	 *            return innermost resource. <b>Note</b>, this check is
	 *            expensive and should not be used in performance critical code.
	 * @return the IFile corresponding to this path, or null
	 */
	@Nullable
	public static IFile getFileForLocation(@NonNull Repository repository,
			@NonNull String repoRelativePath, boolean innerMost) {
		IPath path = new Path(repository.getWorkTree().getAbsolutePath()).append(repoRelativePath);
		return getFileForLocation(path, innerMost);
	}

	/**
	 * Get the {@link IContainer} corresponding to the arguments, using
	 * {@link IWorkspaceRoot#getContainerForLocation(org.eclipse.core.runtime.IPath)}
	 * .
	 *
	 * @param repository
	 *            the repository
	 * @param repoRelativePath
	 *            the repository-relative path of the container to search for
	 * @return the IContainer corresponding to this path, or null
	 */
	@Nullable
	public static IContainer getContainerForLocation(
			@NonNull Repository repository, @NonNull String repoRelativePath) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IPath path = new Path(repository.getWorkTree().getAbsolutePath()).append(repoRelativePath);
		return root.getContainerForLocation(path);
	}

	/**
	 * Checks if the path relative to the given repository refers to a symbolic
	 * link
	 *
	 * @param repository
	 *            the repository of the file
	 * @param repoRelativePath
	 *            the repository-relative path of the file to search for
	 * @return {@code true} if the path in the given repository refers to a
	 *         symbolic link
	 */
	public static boolean isSymbolicLink(@NonNull Repository repository,
			@NonNull String repoRelativePath) {
		try {
			File f = new Path(repository.getWorkTree().getAbsolutePath())
					.append((repoRelativePath)).toFile();
			return FS.DETECTED.isSymLink(f);
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Returns a resource handle for this path in the workspace. Note that
	 * neither the resource nor the result need exist in the workspace : this
	 * may return inexistant or otherwise non-accessible IResources.
	 *
	 * @param path
	 *            Path for which we need a resource handle.
	 * @return The resource handle for the given path in the workspace.
	 */
	@NonNull
	public static IResource getResourceHandleForLocation(@NonNull IPath path) {
		final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
				.getRoot();

		final IResource resource;
		if (path.segmentCount() > 1)
			resource = workspaceRoot.getFile(path);
		else
			resource = workspaceRoot.getProject(path.toString());
		return resource;
	}

	/**
	 * The method splits the given resources by their repository. For each
	 * occurring repository a list is built containing the repository relative
	 * paths of the related resources.
	 * <p>
	 * When one of the passed resources corresponds to the working directory,
	 * <code>""</code> will be returned as part of the collection.
	 *
	 * @param resources
	 * @return a map containing a list of repository relative paths for each
	 *         occurring repository
	 */
	public static Map<Repository, Collection<String>> splitResourcesByRepository(
			Collection<IResource> resources) {
		Map<Repository, Collection<String>> result = new HashMap<Repository, Collection<String>>();
		for (IResource resource : resources) {
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(resource);
			if (repositoryMapping == null)
				continue;
			String path = repositoryMapping.getRepoRelativePath(resource);
			addPathToMap(repositoryMapping.getRepository(), path, result);
		}
		return result;
	}

	/**
	 * @see #splitResourcesByRepository(Collection)
	 * @param resources
	 * @return a map containing a list of repository relative paths for each
	 *         occurring repository
	 */
	public static Map<Repository, Collection<String>> splitResourcesByRepository(
			IResource[] resources) {
		return splitResourcesByRepository(Arrays.asList(resources));
	}

	/**
	 * The method splits the given paths by their repository. For each occurring
	 * repository a list is built containing the repository relative paths of
	 * the related resources.
	 * <p>
	 * When one of the passed paths corresponds to the working directory,
	 * <code>""</code> will be returned as part of the collection.
	 *
	 * @param paths
	 * @return a map containing a list of repository relative paths for each
	 *         occurring repository
	 */
	public static Map<Repository, Collection<String>> splitPathsByRepository(
			Collection<IPath> paths) {
		RepositoryCache repositoryCache = Activator.getDefault()
				.getRepositoryCache();
		Map<Repository, Collection<String>> result = new HashMap<Repository, Collection<String>>();
		for (IPath path : paths) {
			Repository repository = repositoryCache.getRepository(path);
			if (repository != null) {
				IPath repoPath = new Path(repository.getWorkTree()
						.getAbsolutePath());
				IPath repoRelativePath = path.makeRelativeTo(repoPath);
				addPathToMap(repository, repoRelativePath.toString(), result);
			}
		}
		return result;
	}

	/**
	 * Determine if given resource is imported into workspace or not
	 *
	 * @param resource
	 * @return {@code true} when given resource is not imported into workspace,
	 *         {@code false} otherwise
	 */
	public static boolean isNonWorkspace(@NonNull IResource resource) {
		return resource.getLocation() == null;
	}

	private static IFile getFileForLocationURI(@NonNull IWorkspaceRoot root,
			@NonNull URI uri) {
		IFile[] files = root.findFilesForLocationURI(uri);
		return getExistingMappedResourceWithShortestPath(files);
	}

	private static IContainer getContainerForLocationURI(IWorkspaceRoot root,
			@NonNull URI uri) {
		IContainer[] containers = root.findContainersForLocationURI(uri);
		return getExistingMappedResourceWithShortestPath(containers);
	}

	private static <T extends IResource> T getExistingMappedResourceWithShortestPath(
			T[] resources) {
		int shortestPathSegmentCount = Integer.MAX_VALUE;
		T shortestPath = null;
		for (T resource : resources) {
			if (!resource.exists()) {
				continue;
			}
			if (!isSharedWithGit(resource)) {
				continue;
			}
			IPath fullPath = resource.getFullPath();
			int segmentCount = fullPath.segmentCount();
			if (segmentCount < shortestPathSegmentCount) {
				shortestPath = resource;
				shortestPathSegmentCount = segmentCount;
			}
		}
		return shortestPath;
	}

	private static void addPathToMap(@NonNull Repository repository,
			@Nullable String path, Map<Repository, Collection<String>> result) {
		if (path != null) {
			Collection<String> resourcesList = result.get(repository);
			if (resourcesList == null) {
				resourcesList = new ArrayList<>();
				result.put(repository, resourcesList);
			}
			resourcesList.add(path);
		}
	}

	/**
	 * This will query all model providers for those that are enabled on the
	 * given resource and list all mappings available for that resource.
	 *
	 * @param resource
	 *            The resource for which we need the associated resource
	 *            mappings.
	 * @param context
	 *            Context from which remote content could be retrieved.
	 * @return All mappings available for that file.
	 */
	public static ResourceMapping[] getResourceMappings(
			@NonNull IResource resource,
			ResourceMappingContext context) {
		final IModelProviderDescriptor[] modelDescriptors = ModelProvider
				.getModelProviderDescriptors();

		final Set<ResourceMapping> mappings = new LinkedHashSet<ResourceMapping>();
		for (IModelProviderDescriptor candidate : modelDescriptors) {
			try {
				final IResource[] resources = candidate
						.getMatchingResources(new IResource[] { resource, });
				if (resources.length > 0) {
					// get mappings from model provider if there are matching resources
					final ModelProvider model = candidate.getModelProvider();
					IgnoreInGitSynchronizations adapter = model
							.getAdapter(IgnoreInGitSynchronizations.class);
					if (adapter != null) {
						continue;
					}
					final ResourceMapping[] modelMappings = model.getMappings(
							resource, context, new NullProgressMonitor());
					for (ResourceMapping mapping : modelMappings)
						mappings.add(mapping);
				}
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return mappings.toArray(new ResourceMapping[0]);
	}

	/**
	 * Save local history.
	 *
	 * @param repository
	 */
	public static void saveLocalHistory(@NonNull Repository repository) {
		IndexDiffCacheEntry indexDiffCacheEntry = org.eclipse.egit.core.Activator
				.getDefault().getIndexDiffCache()
				.getIndexDiffCacheEntry(repository);
		if (indexDiffCacheEntry == null) {
			return;
		}
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		if (indexDiffData != null) {
			Collection<IResource> changedResources = indexDiffData
					.getChangedResources();
			for (IResource changedResource : changedResources) {
				if (changedResource instanceof IFile
						&& changedResource.exists()) {
					try {
						ResourceUtil.saveLocalHistory(changedResource);
					} catch (CoreException e) {
						// Ignore error. Failure to save local history must
						// not interfere with the operation.
						Activator.logError(MessageFormat.format(
								CoreText.ResourceUtil_SaveLocalHistoryFailed,
								changedResource), e);
					}
				}
			}
		}
	}

	private static void saveLocalHistory(@NonNull IResource resource)
			throws CoreException {
		if (!resource.isSynchronized(IResource.DEPTH_ZERO))
			resource.refreshLocal(IResource.DEPTH_ZERO, null);
		// Dummy update to force save for local history.
		((IFile) resource).appendContents(
				new ByteArrayInputStream(new byte[0]), IResource.KEEP_HISTORY,
				null);
	}

	/**
	 * Determines the repository containing the resource.
	 *
	 * @param resource
	 *            to get the repository for
	 * @return the {@link Repository}, or {@code null} if none found.
	 */
	@Nullable
	public static Repository getRepository(@NonNull IResource resource) {
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		if (mapping != null) {
			return mapping.getRepository();
		}
		return Activator.getDefault().getRepositoryCache()
				.getRepository(resource);
	}

	/**
	 * Determines the repository containing the given {@link IPath}.
	 *
	 * @param path
	 *            to get the repository for
	 * @return the {@link Repository}, or {@code null} if none found.
	 */
	@Nullable
	public static Repository getRepository(@NonNull IPath path) {
		return Activator.getDefault().getRepositoryCache().getRepository(path);
	}

	/**
	 * Makes a given path relative to the working directory of the given
	 * repository. If the repository is bare or the path is {@code null} or is
	 * not in that working directory, {@code null} is returned. Returns an empty
	 * path if the given path <em>is</em> the working directory.
	 *
	 * @param path
	 *            to make relative
	 * @param repository
	 *            to make the path relative to
	 * @return the repository-relative path, or {@code null} if the path is not
	 *         inside the repository's working directory.
	 */
	@Nullable
	public static IPath getRepositoryRelativePath(@Nullable IPath path,
			@NonNull Repository repository) {
		if (path == null || repository.isBare()) {
			return null;
		}
		java.nio.file.Path workingDirectory = repository.getWorkTree().toPath();
		java.nio.file.Path toRelativize = path.toFile().toPath();
		if (toRelativize.startsWith(workingDirectory)) {
			int n = workingDirectory.getNameCount();
			int m = toRelativize.getNameCount();
			if (n == m) {
				return new Path(""); //$NON-NLS-1$
			}
			return Path.fromOSString(toRelativize.subpath(n, m).toString());
		}
		return null;
	}

	/**
	 * Returns the {@link IResource}s contained in the {@link ResourceMapping}.
	 *
	 * @param mapping
	 *            the resource mapping from which we extract the resources
	 *
	 * @return the possibly empty list of all contained {@link IResource}s.
	 */
	public static List<IResource> extractResourcesFromMapping(
			@Nullable ResourceMapping mapping) {
		if (mapping == null) {
			return Collections.emptyList();
		}

		ResourceTraversal[] traversals;
		try {
			traversals = mapping.getTraversals(null, null);
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
			return Collections.emptyList();
		}
		if (traversals.length == 0) {
			return Collections.emptyList();
		}

		List<IResource> result = new ArrayList<>();
		for (ResourceTraversal traversal : traversals) {
			IResource[] resources = traversal.getResources();
			result.addAll(Arrays.asList(resources));
		}
		return result;
	}
}
