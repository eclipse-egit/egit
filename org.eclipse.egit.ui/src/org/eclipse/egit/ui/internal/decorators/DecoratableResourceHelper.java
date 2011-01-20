// TODO add license header
package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.ContainerTreeIterator;
import org.eclipse.egit.core.ContainerTreeIterator.ResourceEntry;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.decorators.IDecoratableResource.Staged;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

class DecoratableResourceHelper {

	static final int T_HEAD = 0;

	static final int T_INDEX = 1;

	static final int T_WORKSPACE = 2;

	public static IDecoratableResource[] createDecoratableResources(
			final IResource[] resources) {
		// Use first (available) resource to get repository mapping
		int i = 0;
		while (resources[i] == null) {
			i++;
			if (i >= resources.length)
				// Array only contains nulls
				return null;
		}
		final RepositoryMapping mapping = RepositoryMapping
				.getMapping(resources[i]);

		ArrayList<String> resourcePaths = new ArrayList<String>();
		for (final IResource r : resources) {
			if (r != null)
				resourcePaths.add(mapping.getRepoRelativePath(r));
			else
				resourcePaths.add(null);
		}
		// Check resource paths before proceeding
		boolean containsAtLeastOneValidPath = false;
		for (final String s : resourcePaths) {
			if (s != null) {
				containsAtLeastOneValidPath = true;
				break;
			}
		}
		if (!containsAtLeastOneValidPath)
			return null;

		final IDecoratableResource[] decoratableResources = new IDecoratableResource[resources.length];
		try {
			final TreeWalk treeWalk = createThreeWayTreeWalk(mapping,
					resourcePaths);
			if (treeWalk != null)
				while (treeWalk.next()) {
					String path = treeWalk.getPathString();
					if (path.equals(".project")) //$NON-NLS-1$
						path = ""; //$NON-NLS-1$
					final int index = resourcePaths.indexOf(path);
					if (index != -1) {
						final IResource resource = resources[index];
						DecoratableResource decoratableResource = null;
						switch (resource.getType()) {
						case IResource.FILE:
							decoratableResource = decorateResource(
									new DecoratableResource(resource), treeWalk);
							break;
						case IResource.PROJECT:
							decoratableResource = new DecoratableResourceAdapter(
									resource);
							// TODO still uses old code for project
							decoratableResource.tracked = true;
							break;
						case IResource.FOLDER:
							decoratableResource = new DecoratableResourceAdapter(
									resource);
							// TODO still uses old code for folders
							break;
						}
						decoratableResources[index] = decoratableResource;
					}
				}
		} catch (IOException e) {
			// TODO
		}
		return decoratableResources;
	}

	private static TreeWalk createThreeWayTreeWalk(
			final RepositoryMapping mapping, ArrayList<String> resourcePaths)
			throws IOException {
		final Repository repository = mapping.getRepository();
		final TreeWalk treeWalk = new TreeWalk(repository);

		// Do not apply any filter if decoration for project node (empty path)
		// is requested
		if (!resourcePaths.contains("")) { //$NON-NLS-1$
			// Remove nulls from list
			while (resourcePaths.remove(null))
				;
			treeWalk.setFilter(PathFilterGroup.createFromStrings(resourcePaths));
		}

		treeWalk.setRecursive(treeWalk.getFilter().shouldBeRecursive());
		treeWalk.reset();

		// Repository
		final ObjectId headId = repository.resolve(Constants.HEAD);
		if (headId != null)
			treeWalk.addTree(new RevWalk(repository).parseTree(headId));
		else
			treeWalk.addTree(new EmptyTreeIterator());

		// Index
		treeWalk.addTree(new DirCacheIterator(repository.getDirCache()));

		// Working directory
		treeWalk.addTree(IteratorService.createInitialIterator(repository));

		return treeWalk;
	}

	private static DecoratableResource decorateResource(
			final DecoratableResource decoratableResource,
			final TreeWalk treeWalk) throws IOException {
		final ContainerTreeIterator workspaceIterator = treeWalk.getTree(
				T_WORKSPACE, ContainerTreeIterator.class);
		final ResourceEntry resourceEntry = workspaceIterator != null ? workspaceIterator
				.getResourceEntry() : null;

		if (resourceEntry == null)
			return null;

		if (workspaceIterator != null && workspaceIterator.isEntryIgnored()) {
			decoratableResource.ignored = true;
			return decoratableResource;
		}

		final int mHead = treeWalk.getRawMode(T_HEAD);
		final int mIndex = treeWalk.getRawMode(T_INDEX);

		if (mHead == FileMode.MISSING.getBits()
				&& mIndex == FileMode.MISSING.getBits())
			return decoratableResource;

		decoratableResource.tracked = true;

		if (mHead == FileMode.MISSING.getBits()) {
			decoratableResource.staged = Staged.ADDED;
		} else if (mIndex == FileMode.MISSING.getBits()) {
			decoratableResource.staged = Staged.REMOVED;
		} else if (mHead != mIndex
				|| (mIndex != FileMode.TREE.getBits() && !treeWalk.idEqual(
						T_HEAD, T_INDEX))) {
			decoratableResource.staged = Staged.MODIFIED;
		} else {
			decoratableResource.staged = Staged.NOT_STAGED;
		}

		final DirCacheIterator indexIterator = treeWalk.getTree(T_INDEX,
				DirCacheIterator.class);
		final DirCacheEntry indexEntry = indexIterator != null ? indexIterator
				.getDirCacheEntry() : null;

		if (indexEntry == null)
			return decoratableResource;

		if (indexEntry.getStage() > 0)
			decoratableResource.conflicts = true;

		if (indexEntry.isAssumeValid()) {
			decoratableResource.dirty = false;
			decoratableResource.assumeValid = true;
		} else {
			if (workspaceIterator != null
					&& workspaceIterator.isModified(indexEntry, true))
				decoratableResource.dirty = true;
		}
		return decoratableResource;
	}
}
