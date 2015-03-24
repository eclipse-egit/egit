/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, 2013 Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2012, Gunnar Wagenknecht <gunnar@wagenknecht.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Resource utilities
 *
 */
public class ResourceUtil {

	/**
	 * Return the corresponding resource if it exists and has the Git repository
	 * provider.
	 * <p>
	 * The returned file will be relative to the most nested non-closed
	 * Git-managed project.
	 *
	 * @param location
	 *            the path to check
	 * @return the resources, or null
	 */
	public static IResource getResourceForLocation(IPath location) {
		IFile file = getFileForLocation(location);
		if (file != null) {
			return file;
		}
		return getContainerForLocation(location);
	}

	/**
	 * Return the corresponding file if it exists and has the Git repository
	 * provider.
	 * <p>
	 * The returned file will be relative to the most nested non-closed
	 * Git-managed project.
	 *
	 * @param location
	 * @return the file, or null
	 */
	public static IFile getFileForLocation(IPath location) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile file = root.getFileForLocation(location);
		if (file == null) {
			return null;
		}
		if (isValid(file)) {
			return file;
		}
		URI uri = URIUtil.toURI(location);
		return getFileForLocationURI(root, uri);
	}

	/**
	 * sort out closed, linked or not shared resources
	 *
	 * @param resource
	 * @return true if the resource is shared with git, not a link and
	 *         accessible in Eclipse
	 */
	private static boolean isValid(IResource resource) {
		return resource.isAccessible()
				&& !resource.isLinked(IResource.CHECK_ANCESTORS)
				&& isSharedWithGit(resource);
	}

	private static boolean isSharedWithGit(IResource resource) {
		return RepositoryProvider.getProvider(resource.getProject(),
				GitProvider.ID) != null;
	}

	/**
	 * Return the corresponding container if it exists and has the Git
	 * repository provider.
	 * <p>
	 * The returned container will be relative to the most nested non-closed
	 * Git-managed project.
	 *
	 * @param location
	 * @return the container, or null
	 */
	public static IContainer getContainerForLocation(IPath location) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer dir = root.getContainerForLocation(location);
		if (dir == null) {
			return null;
		}
		if (isValid(dir)) {
			return dir;
		}
		URI uri = URIUtil.toURI(location);
		return getContainerForLocationURI(root, uri);
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
	 * @return the IFile corresponding to this path, or null
	 */
	public static IFile getFileForLocation(Repository repository,
			String repoRelativePath) {
		IPath path = new Path(repository.getWorkTree().getAbsolutePath()).append(repoRelativePath);
		return getFileForLocation(path);
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
	public static IContainer getContainerForLocation(Repository repository,
			String repoRelativePath) {
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
	public static boolean isSymbolicLink(Repository repository,
			String repoRelativePath) {
		try {
			File f = new Path(repository.getWorkTree().getAbsolutePath())
					.append((repoRelativePath)).toFile();
			return FS.DETECTED.isSymLink(f);
		} catch (IOException e) {
			return false;
		}
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
	public static boolean isNonWorkspace(IResource resource) {
		return resource.getLocation() == null;
	}

	private static IFile getFileForLocationURI(IWorkspaceRoot root, URI uri) {
		IFile[] files = root.findFilesForLocationURI(uri);
		return getExistingMappedResourceWithShortestPath(files);
	}

	private static IContainer getContainerForLocationURI(IWorkspaceRoot root,
			URI uri) {
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

	private static void addPathToMap(Repository repository,
			String path, Map<Repository, Collection<String>> result) {
		if (path != null) {
			Collection<String> resourcesList = result.get(repository);
			if (resourcesList == null) {
				resourcesList = new ArrayList<String>();
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
	public static ResourceMapping[] getResourceMappings(IResource resource,
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
					final ResourceMapping[] modelMappings = model.getMappings(
							resource, context, new NullProgressMonitor());
					for (ResourceMapping mapping : modelMappings)
						mappings.add(mapping);
				}
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return mappings.toArray(new ResourceMapping[mappings.size()]);
	}

	/**
	 * Save local history.
	 *
	 * @param repository
	 */
	public static void saveLocalHistory(Repository repository) {
		IndexDiffCacheEntry indexDiffCacheEntry = org.eclipse.egit.core.Activator
				.getDefault().getIndexDiffCache()
				.getIndexDiffCacheEntry(repository);
		IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		if (indexDiffData != null) {
			Collection<IResource> changedResources = indexDiffData
					.getChangedResources();
			if (changedResources != null) {
				for (IResource changedResource : changedResources) {
					if (changedResource instanceof IFile
							&& changedResource.exists()) {
						try {
							ResourceUtil.saveLocalHistory(changedResource);
						} catch (CoreException e) {
							// Ignore error. Failure to save local history must
							// not interfere with the operation.
							Activator
									.logError(
											MessageFormat
													.format(CoreText.ResourceUtil_SaveLocalHistoryFailed,
															changedResource), e);
						}
					}
				}
			}
		}
	}

	private static void saveLocalHistory(IResource resource)
			throws CoreException {
		if (!resource.isSynchronized(IResource.DEPTH_ZERO))
			resource.refreshLocal(IResource.DEPTH_ZERO, null);
		// Dummy update to force save for local history.
		((IFile) resource).appendContents(
				new ByteArrayInputStream(new byte[0]), IResource.KEEP_HISTORY,
				null);
	}
}
