/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCacheTree;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;

/**
 * Allows traverse {@link GitModelCacheTree} objects.
 */
class GitCacheTreeTraveral extends ResourceTraversal {

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace().getRoot();

	public GitCacheTreeTraveral(GitModelCacheTree cacheTree) {
		super(getResources(cacheTree),
				IResource.DEPTH_INFINITE, IResource.NONE);
	}

	private static IResource[] getResources(GitModelCacheTree cacheTree) {
		List<IResource> result = new ArrayList<IResource>();

		for (GitModelObject object : cacheTree.getChildren()) {
			IPath location = object.getLocation();

			IResource resource;
			if (object.isContainer())
				resource = ROOT.getContainerForLocation(location);
			else
				resource = ROOT.getFileForLocation(location);

			result.add(resource);
		}

		return result.toArray(new IResource[result.size()]);
	}

}
