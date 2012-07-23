/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;

/**
 * Resource utilities
 *
 */
public class ResourceUtil {

	/**
	 * The method splits the given resources by their repository. For each
	 * occurring repository a list is built containing the repository relative
	 * paths of the related resources.
	 *
	 * @param resources
	 * @return a map containing a list of repository relative paths for each
	 *         occurring repository
	 */
	public static Map<Repository, Collection<String>> splitResourcesByRepository(
			IResource[] resources) {
		Map<Repository, Collection<String>> result = new HashMap<Repository, Collection<String>>();
		for (IResource resource : resources) {
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(resource);
			if (repositoryMapping == null)
				continue;
			Repository repository = repositoryMapping.getRepository();
			Collection<String> resourcesList = result.get(repository);
			if (resourcesList == null) {
				resourcesList = new ArrayList<String>();
				result.put(repository, resourcesList);
			}
			String path = repositoryMapping.getRepoRelativePath(resource);
			if (path != null && path.length() > 0)
				resourcesList.add(path);
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

	/**
	 * Get all the resources behind the model element.
	 *
	 * @param element
	 * @return the resources (may be empty)
	 */
	public static Collection<IResource> getMappedResources(Object element) {
		ResourceMapping mapping = AdapterUtils.adapt(element, ResourceMapping.class);
		if (mapping != null) {
			try {
				Collection<IResource> result = new ArrayList<IResource>();
				ResourceTraversal[] traversals = mapping.getTraversals(
						ResourceMappingContext.LOCAL_CONTEXT, null);
				for (ResourceTraversal traversal : traversals) {
					IResource[] resources = traversal.getResources();
					result.addAll(Arrays.asList(resources));
				}
				return result;
			} catch (CoreException e) {
				return Collections.emptyList();
			}
		}

		IResource resource = AdapterUtils.adapt(element, IResource.class);
		if (resource != null)
			return Arrays.asList(resource);

		return Collections.emptyList();
	}
}
