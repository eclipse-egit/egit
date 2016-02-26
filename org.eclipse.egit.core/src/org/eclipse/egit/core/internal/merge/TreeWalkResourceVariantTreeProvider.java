/*******************************************************************************
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.storage.TreeParserResourceVariant;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.team.core.variants.IResourceVariantTree;

/**
 * This will populate its three {@link IResourceVariantTree} by walking over a
 * tree walk and caching the IResources it spans.
 * <p>
 * Files that are not located within the workspace will be ignored and thus will
 * not be accessible through the trees created by this provider.
 * </p>
 */
public class TreeWalkResourceVariantTreeProvider implements
		GitResourceVariantTreeProvider {
	private final IResourceVariantTree baseTree;

	private final IResourceVariantTree oursTree;

	private final IResourceVariantTree theirsTree;

	private final Set<IResource> roots;

	private final Set<IResource> knownResources;

	/**
	 * Constructs the resource variant trees by iterating over the given tree
	 * walk. This TreeWalk must contain at least three trees corresponding to
	 * the three "sides" we need.
	 * <p>
	 * The tree walk will be reset to its initial state when we are done with
	 * the iteration.
	 * </p>
	 *
	 * @param repository
	 *            The repository this tree walk has been created for.
	 * @param treeWalk
	 *            The tree walk to iterate over.
	 * @param baseIndex
	 *            Index of the ancestor tree in the given TreeWalk (value
	 *            returned by {@link TreeWalk#addTree(AbstractTreeIterator)})
	 * @param ourIndex
	 *            Index of our tree in the given TreeWalk (value returned by
	 *            {@link TreeWalk#addTree(AbstractTreeIterator)})
	 * @param theirIndex
	 *            Index of their tree in the given TreeWalk (value returned by
	 *            {@link TreeWalk#addTree(AbstractTreeIterator)})
	 * @throws IOException
	 *             if we somehow cannot iterate over the treewalk.
	 */
	public TreeWalkResourceVariantTreeProvider(Repository repository,
			TreeWalk treeWalk, int baseIndex, int ourIndex, int theirIndex)
			throws IOException {
		// Record the initial state of this tree walk before iterating
		final AbstractTreeIterator[] initialTrees = new AbstractTreeIterator[treeWalk
				.getTreeCount()];
		for (int i = 0; i < treeWalk.getTreeCount(); i++) {
			initialTrees[i] = treeWalk.getTree(i, AbstractTreeIterator.class);
		}

		final GitResourceVariantCache baseCache = new GitResourceVariantCache();
		final GitResourceVariantCache theirsCache = new GitResourceVariantCache();
		final GitResourceVariantCache oursCache = new GitResourceVariantCache();

		while (treeWalk.next()) {
			final int modeBase = treeWalk.getRawMode(baseIndex);
			final int modeOurs = treeWalk.getRawMode(ourIndex);
			final int modeTheirs = treeWalk.getRawMode(theirIndex);
			if (modeBase == 0 && modeOurs == 0 && modeTheirs == 0) {
				// untracked
				continue;
			}

			final CanonicalTreeParser base = treeWalk.getTree(baseIndex,
					CanonicalTreeParser.class);
			final CanonicalTreeParser ours = treeWalk.getTree(ourIndex,
					CanonicalTreeParser.class);
			final CanonicalTreeParser theirs = treeWalk.getTree(theirIndex,
					CanonicalTreeParser.class);

			final IPath path = new Path(treeWalk.getPathString());
			final IResource resource = ResourceUtil
					.getResourceHandleForLocation(path);
			// Resource variants only make sense for IResources. Do not consider
			// files outside of the workspace or otherwise non accessible.
			if (resource.getProject() != null
					&& resource.getProject().isAccessible()) {
				if (modeBase != 0) {
					baseCache.setVariant(resource,
							TreeParserResourceVariant.create(repository, base));
				}
				if (modeOurs != 0) {
					oursCache.setVariant(resource,
							TreeParserResourceVariant.create(repository, ours));
				}
				if (modeTheirs != 0) {
					theirsCache.setVariant(resource,
							TreeParserResourceVariant.create(repository, theirs));
				}
			}

			if (treeWalk.isSubtree()) {
				treeWalk.enterSubtree();
			}
		}

		// TODO any better way to reset the tree walk after an iteration?
		treeWalk.reset();
		for (int i = 0; i < initialTrees.length; i++) {
			initialTrees[i].reset();
			treeWalk.addTree(initialTrees[i]);
		}

		baseTree = new GitCachedResourceVariantTree(baseCache);
		theirsTree = new GitCachedResourceVariantTree(theirsCache);
		oursTree = new GitCachedResourceVariantTree(oursCache);

		roots = new LinkedHashSet<IResource>();
		roots.addAll(baseCache.getRoots());
		roots.addAll(oursCache.getRoots());
		roots.addAll(theirsCache.getRoots());

		knownResources = new LinkedHashSet<IResource>();
		knownResources.addAll(baseCache.getKnownResources());
		knownResources.addAll(oursCache.getKnownResources());
		knownResources.addAll(theirsCache.getKnownResources());
	}

	@Override
	public IResourceVariantTree getBaseTree() {
		return baseTree;
	}

	@Override
	public IResourceVariantTree getRemoteTree() {
		return theirsTree;
	}

	@Override
	public IResourceVariantTree getSourceTree() {
		return oursTree;
	}

	@Override
	public Set<IResource> getKnownResources() {
		return knownResources;
	}

	@Override
	public Set<IResource> getRoots() {
		return roots;
	}
}
