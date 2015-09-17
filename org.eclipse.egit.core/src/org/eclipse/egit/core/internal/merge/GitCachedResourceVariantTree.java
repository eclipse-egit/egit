/*******************************************************************************
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantTree;

/**
 * An immutable resource variant tree backed by a
 * {@link GitResourceVariantCache}. This will never contact the server.
 * <p>
 * This will not react to refreshing calls and shouldn't be used for
 * synchronization purposes.
 * </p>
 */
/*
 * Illegal implementation of IResourceVariantTree : we could also extend the
 * AbstractResourceVariantTree... but since we don't react to refreshing calls
 * anyway, we do not need the extra logic it provides.
 */
class GitCachedResourceVariantTree implements IResourceVariantTree {
	private final GitResourceVariantCache cache;

	public GitCachedResourceVariantTree(GitResourceVariantCache cache) {
		this.cache = cache;
	}

	@Override
	public IResource[] roots() {
		final Set<IResource> roots = cache.getRoots();
		return roots.toArray(new IResource[roots.size()]);
	}

	@Override
	public IResource[] members(IResource resource) throws TeamException {
		return cache.members(resource);
	}

	@Override
	public IResourceVariant getResourceVariant(IResource resource)
			throws TeamException {
		return cache.getVariant(resource);
	}

	@Override
	public boolean hasResourceVariant(IResource resource) throws TeamException {
		return cache.getVariant(resource) != null;
	}

	@Override
	public IResource[] refresh(IResource[] resources, int depth,
			IProgressMonitor monitor) throws TeamException {
		// This does not react to refresh calls
		return new IResource[0];
	}

	@Override
	public void flushVariants(IResource resource, int depth)
			throws TeamException {
		// Empty implementation
	}
}
