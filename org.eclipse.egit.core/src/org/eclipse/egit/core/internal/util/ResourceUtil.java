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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
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
			if (repositoryMapping != null) {
				Repository repository = repositoryMapping.getRepository();
				Collection<String> resourcesList = result.get(repository);
				if (resourcesList == null) {
					resourcesList = new ArrayList<String>();
					result.put(repository, resourcesList);
				}
				resourcesList.add(repositoryMapping
						.getRepoRelativePath(resource));
			}
		}
		return result;
	}
}
