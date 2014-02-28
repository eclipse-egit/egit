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
	 * @param baseTreeIndex
	 *            Index of the base tree in this tree walk.
	 * @param oursTreeIndex
	 *            Index of our tree in this tree walk.
	 * @param theirsTreeIndex
	 *            Index of their tree in this tree walk.
	 * @throws IOException
	 *             if we somehow cannot iterate over the treewalk.
	 */
	public TreeWalkResourceVariantTreeProvider(Repository repository,
			TreeWalk treeWalk, int baseTreeIndex, int oursTreeIndex,
			int theirsTreeIndex) throws IOException {
		// Record the initial state of this tree walk before iterating
		final AbstractTreeIterator[] initialTrees = new AbstractTreeIterator[treeWalk
				.getTreeCount()];
		for (int i = 0; i < treeWalk.getTreeCount(); i++)
			initialTrees[i] = treeWalk.getTree(i, AbstractTreeIterator.class);

		final GitResourceVariantCache baseCache = new GitResourceVariantCache();
		final GitResourceVariantCache theirsCache = new GitResourceVariantCache();
		final GitResourceVariantCache oursCache = new GitResourceVariantCache();

		while (treeWalk.next()) {
			final int modeBase = treeWalk.getRawMode(baseTreeIndex);
			final int modeTheirs = treeWalk.getRawMode(theirsTreeIndex);
			final int modeOurs = treeWalk.getRawMode(oursTreeIndex);
			if (modeBase == 0 && modeOurs == 0 && modeTheirs == 0)
				// untracked
				continue;

			final CanonicalTreeParser base = treeWalk.getTree(baseTreeIndex,
					CanonicalTreeParser.class);
			final CanonicalTreeParser theirs = treeWalk.getTree(
					theirsTreeIndex, CanonicalTreeParser.class);
			final CanonicalTreeParser ours = treeWalk.getTree(oursTreeIndex,
					CanonicalTreeParser.class);

			final IPath path = new Path(treeWalk.getPathString());
			final IResource resource = ResourceUtil
					.getResourceHandleForLocation(path);
			// Resource variants only make sense for IResources. Do not consider
			// files outside of the workspace or otherwise non accessible.
			if (resource == null || resource.getProject().isAccessible()) {
				if (modeBase != 0)
					baseCache.setVariant(resource,
							TreeParserResourceVariant.create(repository, base));
				if (modeTheirs != 0)
					theirsCache.setVariant(resource,
							TreeParserResourceVariant.create(repository, theirs));
				if (modeOurs != 0)
					oursCache.setVariant(resource,
							TreeParserResourceVariant.create(repository, ours));
			}

			if (treeWalk.isSubtree())
				treeWalk.enterSubtree();
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

	public IResourceVariantTree getBaseTree() {
		return baseTree;
	}

	public IResourceVariantTree getRemoteTree() {
		return theirsTree;
	}

	public IResourceVariantTree getSourceTree() {
		return oursTree;
	}

	public Set<IResource> getKnownResources() {
		return knownResources;
	}

	public Set<IResource> getRoots() {
		return roots;
	}
}
