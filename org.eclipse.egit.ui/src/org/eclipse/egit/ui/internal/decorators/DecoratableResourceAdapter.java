/*******************************************************************************
 * Copyright (C) 2007, IBM Corporation and others
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.decorators;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.AdaptableFileTreeIterator;
import org.eclipse.egit.core.ContainerTreeIterator;
import org.eclipse.egit.core.ContainerTreeIterator.ResourceEntry;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

class DecoratableResourceAdapter implements IDecoratableResource {

	private final IResource resource;

	private final RepositoryMapping mapping;

	private final Repository repository;

	private final ObjectId headId;

	private final IPreferenceStore store;

	private final String branch;

	private final String repositoryName;

	private boolean tracked = false;

	private boolean ignored = false;

	private boolean dirty = false;

	private boolean conflicts = false;

	private boolean assumeValid = false;

	private Staged staged = Staged.NOT_STAGED;

	private final boolean trace;

	static final int T_HEAD = 0;

	static final int T_INDEX = 1;

	static final int T_WORKSPACE = 2;

	@SuppressWarnings("fallthrough")
	public DecoratableResourceAdapter(IResource resourceToWrap)
			throws IOException {
		trace = GitTraceLocation.DECORATION.isActive();
		resource = resourceToWrap;
		long start = 0;
		if (trace) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.DECORATION.getLocation(),
					"Decorate " + resource.getFullPath()); //$NON-NLS-1$
			start = System.currentTimeMillis();
		}
		try {
			mapping = RepositoryMapping.getMapping(resource);
			repository = mapping.getRepository();
			headId = repository.resolve(Constants.HEAD);

			store = Activator.getDefault().getPreferenceStore();
			String repoName = Activator.getDefault().getRepositoryUtil().getRepositoryName(repository);
			RepositoryState state = repository.getRepositoryState();
			if (state != RepositoryState.SAFE)
				repositoryName = repoName + '|' + state.getDescription();
			else
				repositoryName = repoName;

			branch = getShortBranch();

			TreeWalk treeWalk = createThreeWayTreeWalk();
			if (treeWalk == null)
				return;

			switch (resource.getType()) {
			case IResource.FILE:
				if (!treeWalk.next())
					return;
				extractResourceProperties(treeWalk);
				break;
			case IResource.PROJECT:
				tracked = true;
			case IResource.FOLDER:
				extractContainerProperties(treeWalk);
				break;
			}
		} finally {
			if (trace)
				GitTraceLocation
						.getTrace()
						.trace(GitTraceLocation.DECORATION.getLocation(),
								"Decoration took " + (System.currentTimeMillis() - start) //$NON-NLS-1$
										+ " ms"); //$NON-NLS-1$
		}
	}

	private String getShortBranch() throws IOException {
		Ref head = repository.getRef(Constants.HEAD);
		if (head != null && !head.isSymbolic()) {
			String refString = Activator.getDefault().getRepositoryUtil()
					.mapCommitToRef(repository, repository.getFullBranch(),
							false);
			if (refString != null) {
				return repository.getFullBranch().substring(0, 7)
						+ "... (" + refString + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			} else
				return repository.getFullBranch().substring(0, 7) + "..."; //$NON-NLS-1$
		}

		return repository.getBranch();
	}

	private void extractResourceProperties(TreeWalk treeWalk) throws IOException {
		final ContainerTreeIterator workspaceIterator = treeWalk.getTree(
				T_WORKSPACE, ContainerTreeIterator.class);
		final ResourceEntry resourceEntry = workspaceIterator != null ? workspaceIterator
				.getResourceEntry() : null;

		if (resourceEntry == null)
			return;

		if (workspaceIterator != null && workspaceIterator.isEntryIgnored()) {
			ignored = true;
			return;
		}

		final int mHead = treeWalk.getRawMode(T_HEAD);
		final int mIndex = treeWalk.getRawMode(T_INDEX);

		if (mHead == FileMode.MISSING.getBits()
				&& mIndex == FileMode.MISSING.getBits())
			return;

		tracked = true;

		if (mHead == FileMode.MISSING.getBits()) {
			staged = Staged.ADDED;
		} else if (mIndex == FileMode.MISSING.getBits()) {
			staged = Staged.REMOVED;
		} else if (mHead != mIndex
				|| (mIndex != FileMode.TREE.getBits() && !treeWalk.idEqual(
						T_HEAD, T_INDEX))) {
			staged = Staged.MODIFIED;
		} else {
			staged = Staged.NOT_STAGED;
		}

		final DirCacheIterator indexIterator = treeWalk.getTree(T_INDEX,
				DirCacheIterator.class);
		final DirCacheEntry indexEntry = indexIterator != null ? indexIterator
				.getDirCacheEntry() : null;

		if (indexEntry == null)
			return;

		if (indexEntry.getStage() > 0)
			conflicts = true;

		if (indexEntry.isAssumeValid()) {
			dirty = false;
			assumeValid = true;
		} else {
			if (workspaceIterator != null
					&& workspaceIterator.isModified(indexEntry, true, config_filemode(),
							repository.getFS()))
				dirty = true;
		}
	}

	private boolean config_filemode() {
		StoredConfig config = repository.getConfig();
		Boolean filemode = Boolean.valueOf(config.getBoolean("core", null, //$NON-NLS-1$
				"filemode", true)); //$NON-NLS-1$
		return filemode.booleanValue();
	}

	private class RecursiveStateFilter extends TreeFilter {

		private int filesChecked = 0;

		private int targetDepth = -1;

		private final int recurseLimit;

		public RecursiveStateFilter() {
			recurseLimit = store
					.getInt(UIPreferences.DECORATOR_RECURSIVE_LIMIT);
		}

		@Override
		public boolean include(TreeWalk treeWalk)
				throws MissingObjectException, IncorrectObjectTypeException,
				IOException {
			if (trace)
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.DECORATION.getLocation(),
						treeWalk.getPathString());
			final WorkingTreeIterator workingTreeIterator = treeWalk.getTree(
					T_WORKSPACE, WorkingTreeIterator.class);
			if (workingTreeIterator != null) {
				if (workingTreeIterator instanceof ContainerTreeIterator) {
					final ContainerTreeIterator workspaceIterator =
						(ContainerTreeIterator) workingTreeIterator;
					ResourceEntry resourceEntry = workspaceIterator
							.getResourceEntry();
					if (resource.equals(resourceEntry.getResource())
							&& workspaceIterator.isEntryIgnored()) {
						ignored = true;
						return false;
					}
					if (resource.getFullPath().isPrefixOf(
							resourceEntry.getResource().getFullPath())
							&& treeWalk.getFileMode(T_HEAD) == FileMode.MISSING
							&& treeWalk.getFileMode(T_INDEX) == FileMode.MISSING) {
						// we reached the folder to decorate (or are beyond)
						// we can cut if the current entry does not
						// exist in head and index
						if (trace)
							GitTraceLocation.getTrace().trace(
									GitTraceLocation.DECORATION.getLocation(),
									"CUT"); //$NON-NLS-1$
						return false;
					}

				} else {
					// For the project resource, it's still the
					// AdaptableFileTreeIterator. So we have to compare the path
					// of the resource with path of the iterator
					IPath wdPath = new Path(repository.getWorkTree()
							.getAbsolutePath()).append(workingTreeIterator
							.getEntryPathString());
					IPath resPath = resource.getLocation();
					if (wdPath.equals(resPath)
							&& workingTreeIterator.isEntryIgnored()) {
						ignored = true;
						return false;

					}
					if (resPath.isPrefixOf(wdPath)
							&& treeWalk.getFileMode(T_HEAD) == FileMode.MISSING
							&& treeWalk.getFileMode(T_INDEX) == FileMode.MISSING) {
						// we reached the folder to decorate (or are beyond)
						// we can cut if the current entry does not
						// exist in head and index
						if (trace)
							GitTraceLocation.getTrace().trace(
									GitTraceLocation.DECORATION.getLocation(),
									"CUT"); //$NON-NLS-1$
						return false;
					}
				}
			}

			if (FileMode.TREE.equals(treeWalk.getRawMode(T_WORKSPACE)))
				return shouldRecurse(treeWalk);

			// Backup current state so far
			Staged wasStaged = staged;
			boolean wasDirty = dirty;
			boolean hadConflicts = conflicts;

			extractResourceProperties(treeWalk);
			filesChecked++;

			// Merge results with old state
			ignored = false;
			assumeValid = false;
			dirty = wasDirty || dirty;
			conflicts = hadConflicts || conflicts;
			if (staged != wasStaged && filesChecked > 1)
				staged = Staged.MODIFIED;

			return false;
		}

		private boolean shouldRecurse(TreeWalk treeWalk) throws IOException {
			final WorkingTreeIterator workspaceIterator = treeWalk.getTree(
					T_WORKSPACE, WorkingTreeIterator.class);

			if (workspaceIterator instanceof AdaptableFileTreeIterator)
				return true;

			ResourceEntry resourceEntry = null;
			if (workspaceIterator != null)
				resourceEntry = ((ContainerTreeIterator) workspaceIterator)
						.getResourceEntry();

			if (resourceEntry == null)
				return true;

			IResource visitingResource = resourceEntry.getResource();
			if (targetDepth == -1) {
				if (visitingResource.equals(resource)
						|| visitingResource.getParent().equals(resource))
					targetDepth = treeWalk.getDepth();
				else
					return true;
			}

			if ((treeWalk.getDepth() - targetDepth) >= recurseLimit) {
				if (visitingResource.equals(resource))
					extractResourceProperties(treeWalk);

				return false;
			}

			return true;
		}

		@Override
		public TreeFilter clone() {
			RecursiveStateFilter clone = new RecursiveStateFilter();
			clone.filesChecked = this.filesChecked;
			return clone;
		}

		@Override
		public boolean shouldBeRecursive() {
			return true;
		}
	}

	private void extractContainerProperties(TreeWalk treeWalk) throws IOException {
		treeWalk.setFilter(AndTreeFilter.create(treeWalk.getFilter(),
				new RecursiveStateFilter()));
		treeWalk.setRecursive(true);

		treeWalk.next();
	}

	/**
	 * Adds a filter to the specified tree walk limiting the results to only
	 * those matching the resource specified by <code>resourceToFilterBy</code>
	 * <p>
	 * If the resource does not exists in the current repository, no filter is
	 * added and the method returns <code>false</code>. If the resource is a
	 * project, no filter is added, but the operation is considered a success.
	 *
	 * @param treeWalk
	 *            the tree walk to add the filter to
	 * @param resourceToFilterBy
	 *            the resource to filter by
	 *
	 * @return <code>true</code> if the filter could be added,
	 *         <code>false</code> otherwise
	 */
	private boolean addResourceFilter(final TreeWalk treeWalk,
			final IResource resourceToFilterBy) {
		Set<String> repositoryPaths = Collections.singleton(mapping
				.getRepoRelativePath(resourceToFilterBy));
		if (repositoryPaths.isEmpty())
			return false;

		if (repositoryPaths.contains("")) //$NON-NLS-1$
			return true; // Project filter

		treeWalk.setFilter(PathFilterGroup.createFromStrings(repositoryPaths));
		return true;
	}

	/**
	 * Helper method to create a new tree walk between the repository, the
	 * index, and the working tree.
	 *
	 * @return the created tree walk, or null if it could not be created
	 * @throws IOException
	 *             if there were errors when creating the tree walk
	 */
	private TreeWalk createThreeWayTreeWalk() throws IOException {
		final TreeWalk treeWalk = new TreeWalk(repository);
		if (!addResourceFilter(treeWalk, resource))
			return null;

		treeWalk.setRecursive(treeWalk.getFilter().shouldBeRecursive());
		treeWalk.reset();

		// Repository
		if (headId != null)
			treeWalk.addTree(new RevWalk(repository).parseTree(headId));
		else
			treeWalk.addTree(new EmptyTreeIterator());

		// Index
		treeWalk.addTree(new DirCacheIterator(repository.readDirCache()));

		// Working directory
		IProject project = resource.getProject();
		IWorkspaceRoot workspaceRoot = resource.getWorkspace().getRoot();
		File repoRoot = repository.getWorkTree();

		if (repoRoot.equals(project.getLocation().toFile()))
			treeWalk.addTree(new ContainerTreeIterator(project));
		else if (repoRoot.equals(workspaceRoot.getLocation().toFile()))
			treeWalk.addTree(new ContainerTreeIterator(workspaceRoot));
		else
			treeWalk.addTree(new AdaptableFileTreeIterator(repoRoot,
					workspaceRoot));

		return treeWalk;
	}

	public String getName() {
		return resource.getName();
	}

	public int getType() {
		return resource.getType();
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public String getBranch() {
		return branch;
	}

	public boolean isTracked() {
		return tracked;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public boolean isDirty() {
		return dirty;
	}

	public Staged staged() {
		return staged;
	}

	public boolean hasConflicts() {
		return conflicts;
	}

	public boolean isAssumeValid() {
		return assumeValid;
	}
}
