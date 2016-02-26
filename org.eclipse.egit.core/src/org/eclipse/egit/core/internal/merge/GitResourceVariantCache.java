/*******************************************************************************
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.variants.IResourceVariant;

/**
 * Caches the resource variants corresponding to local IResources.
 */
class GitResourceVariantCache {
	private final Map<IResource, IResourceVariant> cache = new LinkedHashMap<IResource, IResourceVariant>();

	private final Map<IResource, Set<IResource>> members = new LinkedHashMap<IResource, Set<IResource>>();

	private final Set<IResource> roots = new LinkedHashSet<IResource>();

	/**
	 * Sets the variant associated with the given resource in this cache.
	 *
	 * @param resource
	 *            The resource for which we need to cache a variant.
	 * @param variant
	 *            Variant for the resource.
	 */
	public void setVariant(IResource resource, IResourceVariant variant) {
		cache.put(resource, variant);

		IProject project = resource.getProject();
		if (project == null) {
			return;
		}
		roots.add(project);

		members.put(resource, new LinkedHashSet<IResource>());

		final IResource parent = resource.getParent();
		Set<IResource> parentMembers = members.get(parent);
		if (parentMembers == null) {
			parentMembers = new LinkedHashSet<IResource>();
			members.put(parent, parentMembers);
		}
		parentMembers.add(resource);
	}

	/**
	 * @param resource
	 *            The resource which variant we need.
	 * @return The variant associated with this resource in this cache.
	 */
	public IResourceVariant getVariant(IResource resource) {
		return cache.get(resource);
	}

	/**
	 * @return The known roots of the tree we were populated from.
	 */
	public Set<IResource> getRoots() {
		return Collections.unmodifiableSet(roots);
	}

	/**
	 * @return All resources for which this cache holds variants.
	 */
	public Set<IResource> getKnownResources() {
		return Collections.unmodifiableSet(cache.keySet());
	}

	/**
	 * Returns all members of the given resource for which we hold variants.
	 *
	 * @param resource
	 *            The resource which members we need.
	 * @return All members of the given resource for which we hold variants; an
	 *         empty array if none.
	 */
	public IResource[] members(IResource resource) {
		final Set<IResource> children = members.get(resource);
		return children.toArray(new IResource[children.size()]);
	}
}
