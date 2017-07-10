/*******************************************************************************
 * Copyright (C) 2007, IBM Corporation and others
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Factored out from DecoratableResourceAdapter
 *                                           and GitLightweightDecorator
 *******************************************************************************/
package org.eclipse.egit.ui.internal.resources;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.internal.resources.IResourceState.StagingState;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * Factory for creating {@link IResourceState}s.
 */
public class ResourceStateFactory {

	/**
	 * {@link IResourceState} returned when no information can be retrieved. All
	 * boolean getters return {@code false}, and the
	 * {@link IResourceState.StagingState StagingState} is
	 * {@link IResourceState.StagingState#NOT_STAGED NOT_STAGED}.
	 */
	@NonNull
	public static final IResourceState UNKNOWN_STATE = new ResourceState();

	/**
	 * Singleton {@link IResourceState} returned for ignored files.
	 */
	@NonNull
	private static final IResourceState IGNORED = new ResourceState() {

		@Override
		public boolean isIgnored() {
			return true;
		}
	};

	@NonNull
	private static final ResourceStateFactory INSTANCE = new ResourceStateFactory();

	/**
	 * Retrieves the singleton instance of the {@link ResourceStateFactory}.
	 *
	 * @return the factory singleton
	 */
	@NonNull
	public static ResourceStateFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns the {@link IndexDiffData} for a given {@link IResource}, provided
	 * the resource exists and belongs to a git-tracked project.
	 *
	 * @param resource
	 *            context to get the repository to get the index diff data from
	 * @return the IndexDiffData, or {@code null} if none.
	 */
	@Nullable
	public IndexDiffData getIndexDiffDataOrNull(@Nullable IResource resource) {
		if (resource == null || resource.getType() == IResource.ROOT
				|| !ResourceUtil.isSharedWithGit(resource)) {
			return null;
		}
		Repository repository = ResourceUtil.getRepository(resource);
		return getIndexDiffDataOrNull(repository);
	}

	/**
	 * Returns the {@link IndexDiffData} for a given {@link File}, provided the
	 * file is in a git repository working tree.
	 *
	 * @param file
	 *            context to get the repository to get the index diff data from
	 * @return the IndexDiffData, or {@code null} if none.
	 */
	@Nullable
	public IndexDiffData getIndexDiffDataOrNull(@Nullable File file) {
		if (file == null) {
			return null;
		}
		File absoluteFile = file.getAbsoluteFile();
		IPath path = new org.eclipse.core.runtime.Path(absoluteFile.getPath());
		Repository repository = ResourceUtil.getRepository(path);
		return getIndexDiffDataOrNull(repository);
	}

	/**
	 * Returns the {@link IndexDiffData} for a given {@link Repository}.
	 *
	 * @param repository
	 *            to get the index diff data from
	 * @return the IndexDiffData, or {@code null} if none.
	 */
	@Nullable
	private IndexDiffData getIndexDiffDataOrNull(
			@Nullable Repository repository) {
		if (repository == null) {
			return null;
		} else if (repository.isBare()) {
			// For bare repository just return empty data
			return new IndexDiffData();
		}
		IndexDiffCacheEntry diffCacheEntry = Activator.getDefault()
				.getIndexDiffCache().getIndexDiffCacheEntry(repository);
		if (diffCacheEntry == null) {
			return null;
		}
		return diffCacheEntry.getIndexDiff();
	}

	/**
	 * Determines the repository state of the given {@link IResource}.
	 *
	 * @param resource
	 *            to get the state for
	 * @return the state, {@link #UNKNOWN_STATE} if none can be determined.
	 */
	@NonNull
	public IResourceState get(@Nullable IResource resource) {
		IndexDiffData indexDiffData = getIndexDiffDataOrNull(resource);
		if (indexDiffData == null || resource == null) {
			return UNKNOWN_STATE;
		}
		return get(indexDiffData, resource);
	}

	/**
	 * Determines the repository state of the given {@link File}.
	 *
	 * @param file
	 *            to get the state for
	 * @return the state, {@link #UNKNOWN_STATE} if none can be determined.
	 */
	@NonNull
	public IResourceState get(@Nullable File file) {
		IndexDiffData indexDiffData = getIndexDiffDataOrNull(file);
		if (indexDiffData == null || file == null) {
			return UNKNOWN_STATE;
		}
		return get(indexDiffData, file);
	}

	/**
	 * Computes an {@link IResourceState} for the given {@link IResource} from
	 * the given {@link IndexDiffData}.
	 *
	 * @param indexDiffData
	 *            to compute the state from
	 * @param resource
	 *            to get the state of
	 * @return the state
	 */
	@NonNull
	public IResourceState get(@NonNull IndexDiffData indexDiffData,
			@NonNull IResource resource) {
		IPath path = resource.getLocation();
		if (path != null) {
			return get(indexDiffData, new ResourceItem(resource));
		}
		return UNKNOWN_STATE;
	}

	/**
	 * Computes an {@link IResourceState} for the given {@link File} from the
	 * given {@link IndexDiffData}.
	 *
	 * @param indexDiffData
	 *            to compute the state from
	 * @param file
	 *            to get the state of
	 * @return the state
	 */
	@NonNull
	public IResourceState get(@NonNull IndexDiffData indexDiffData,
			@NonNull File file) {
		return get(indexDiffData, new FileItem(file));
	}

	/**
	 * Computes an {@link IResourceState} for the given {@link FileSystemItem}
	 * from the given {@link IndexDiffData}.
	 *
	 * @param indexDiffData
	 *            to compute the state from
	 * @param file
	 *            to get the state of
	 * @return the state
	 */
	@NonNull
	private IResourceState get(@NonNull IndexDiffData indexDiffData,
			@NonNull FileSystemItem file) {
		IPath path = file.getAbsolutePath();
		if (path == null) {
			return UNKNOWN_STATE;
		}
		Repository repository = file.getRepository();
		if (repository == null || repository.isBare()) {
			return UNKNOWN_STATE;
		}
		File workTree = repository.getWorkTree();
		String repoRelativePath = path.makeRelativeTo(
				new org.eclipse.core.runtime.Path(workTree.getAbsolutePath()))
				.toString();
		if (repoRelativePath.equals(path.toString())) {
			// Could not be made relative.
			return UNKNOWN_STATE;
		}
		if (file.isContainer()) {
			if (!repoRelativePath.endsWith("/")) { //$NON-NLS-1$
				repoRelativePath += '/';
			}
			if (ResourceUtil.isSymbolicLink(repository, repoRelativePath)) {
				// The Eclipse resource model handles a symlink to a folder like
				// the container it refers to but git status handles the symlink
				// source like a special file.
				return extractFileProperties(indexDiffData, repoRelativePath);
			} else {
				return extractContainerProperties(indexDiffData,
						repoRelativePath, file);
			}
		} else {
			return extractFileProperties(indexDiffData, repoRelativePath);
		}
	}

	private @NonNull IResourceState extractFileProperties(
			@NonNull IndexDiffData indexDiffData,
			@NonNull String repoRelativePath) {
		Set<String> ignoredFiles = indexDiffData.getIgnoredNotInIndex();
		boolean ignored = ignoredFiles.contains(repoRelativePath)
				|| containsPrefixPath(ignoredFiles, repoRelativePath);
		if (ignored) {
			// Leave the rest at the default (false, NOT_STAGED)
			return IGNORED;
		}
		ResourceState state = new ResourceState();
		Set<String> untracked = indexDiffData.getUntracked();
		state.setTracked(!untracked.contains(repoRelativePath));

		Set<String> added = indexDiffData.getAdded();
		Set<String> removed = indexDiffData.getRemoved();
		Set<String> changed = indexDiffData.getChanged();
		if (added.contains(repoRelativePath)) {
			state.setStagingState(StagingState.ADDED);
		} else if (removed.contains(repoRelativePath)) {
			state.setStagingState(StagingState.REMOVED);
		} else if (changed.contains(repoRelativePath)) {
			state.setStagingState(StagingState.MODIFIED);
		} else {
			state.setStagingState(StagingState.NOT_STAGED);
		}

		// conflicting
		Set<String> conflicting = indexDiffData.getConflicting();
		state.setConflicts(conflicting.contains(repoRelativePath));

		// locally modified
		Set<String> modified = indexDiffData.getModified();
		state.setDirty(modified.contains(repoRelativePath));

		// locally deleted
		Set<String> missing = indexDiffData.getMissing();
		state.setMissing(missing.contains(repoRelativePath));

		Set<String> assumeUnchanged = indexDiffData.getAssumeUnchanged();
		state.setAssumeUnchanged(assumeUnchanged.contains(repoRelativePath));
		return state;
	}

	private @NonNull IResourceState extractContainerProperties(
			@NonNull IndexDiffData indexDiffData,
			@NonNull String repoRelativePath,
			@NonNull FileSystemItem directory) {
		Set<String> ignoredFiles = indexDiffData.getIgnoredNotInIndex();
		boolean ignored = containsPrefixPath(ignoredFiles, repoRelativePath)
				|| !directory.hasContainerAnyFiles();
		if (ignored) {
			return IGNORED;
		}
		ResourceState state = new ResourceState();
		Set<String> untrackedFolders = indexDiffData.getUntrackedFolders();
		state.setTracked(
				!containsPrefixPath(untrackedFolders, repoRelativePath));

		// containers are marked as staged whenever file was added, removed or
		// changed
		Set<String> changed = new HashSet<>(indexDiffData.getChanged());
		changed.addAll(indexDiffData.getAdded());
		changed.addAll(indexDiffData.getRemoved());
		if (containsPrefix(changed, repoRelativePath)) {
			state.setStagingState(StagingState.MODIFIED);
		} else {
			state.setStagingState(StagingState.NOT_STAGED);
		}
		// conflicting
		Set<String> conflicting = indexDiffData.getConflicting();
		state.setConflicts(containsPrefix(conflicting, repoRelativePath));

		// locally modified / untracked
		Set<String> modified = indexDiffData.getModified();
		Set<String> untracked = indexDiffData.getUntracked();
		Set<String> missing = indexDiffData.getMissing();
		state.setDirty(containsPrefix(modified, repoRelativePath)
				|| containsPrefix(untracked, repoRelativePath)
				|| containsPrefix(missing, repoRelativePath));
		return state;
	}

	private boolean containsPrefix(Set<String> collection, String prefix) {
		// when prefix is empty we are handling repository root, therefore we
		// should return true whenever collection isn't empty
		if (prefix.length() == 1 && !collection.isEmpty())
			return true;

		for (String path : collection)
			if (path.startsWith(prefix))
				return true;
		return false;
	}

	private boolean containsPrefixPath(Set<String> collection, String path) {
		for (String entry : collection) {
			String entryPath;
			if (entry.endsWith("/")) //$NON-NLS-1$
				entryPath = entry;
			else
				entryPath = entry + "/"; //$NON-NLS-1$
			if (path.startsWith(entryPath))
				return true;
		}
		return false;
	}

	private interface FileSystemItem {
		boolean hasContainerAnyFiles();

		boolean isContainer();

		@Nullable
		IPath getAbsolutePath();

		@Nullable
		Repository getRepository();
	}

	private static class FileItem implements FileSystemItem {

		@NonNull
		private final File file;

		public FileItem(@NonNull File file) {
			this.file = file;
		}

		@Override
		@NonNull
		public IPath getAbsolutePath() {
			return new org.eclipse.core.runtime.Path(file.getAbsolutePath());
		}

		@Override
		public Repository getRepository() {
			return ResourceUtil.getRepository(getAbsolutePath());
		}

		@Override
		public boolean isContainer() {
			return file.isDirectory();
		}

		@Override
		public boolean hasContainerAnyFiles() {
			if (!isContainer()) {
				throw new IllegalArgumentException("Container expected"); //$NON-NLS-1$
			}
			try {
				final boolean[] result = new boolean[] { false };
				final Path dotGit = Paths.get(Constants.DOT_GIT);
				Files.walkFileTree(file.toPath(), new FileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir,
							BasicFileAttributes attrs) throws IOException {
						if (dotGit.equals(dir.getFileName())) {
							return FileVisitResult.SKIP_SUBTREE;
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path path,
							BasicFileAttributes attrs) throws IOException {
						if (!attrs.isDirectory()) {
							result[0] = true;
							return FileVisitResult.TERMINATE;
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path path,
							IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir,
							IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				});
				return result[0];
			} catch (IOException e) {
				// if can't get any info, treat as with file
				return true;
			}
		}
	}

	private static class ResourceItem implements FileSystemItem {

		@NonNull
		private final IResource resource;

		public ResourceItem(@NonNull IResource resource) {
			this.resource = resource;
		}

		@Override
		@Nullable
		public IPath getAbsolutePath() {
			return resource.getLocation();
		}

		@Override
		public Repository getRepository() {
			return ResourceUtil.getRepository(resource);
		}

		@Override
		public boolean isContainer() {
			return isContainer(resource);
		}

		@Override
		public boolean hasContainerAnyFiles() {
			return containsFiles(resource);
		}

		private boolean isContainer(IResource rsc) {
			int type = rsc.getType();
			return type == IResource.FOLDER || type == IResource.PROJECT
					|| type == IResource.ROOT;
		}

		private boolean containsFiles(IResource rsc) {
			if (rsc instanceof IContainer) {
				IContainer container = (IContainer) rsc;
				try {
					return anyFile(container.members());
				} catch (CoreException e) {
					// if can't get any info, treat as with file
					return true;
				}
			}
			throw new IllegalArgumentException(
					"Expected a container resource."); //$NON-NLS-1$
		}

		private boolean anyFile(IResource[] members) {
			for (IResource member : members) {
				if (member.getType() == IResource.FILE) {
					return true;
				} else if (isContainer(member) && containsFiles(member)) {
					return true;
				}
			}
			return false;
		}
	}
}
