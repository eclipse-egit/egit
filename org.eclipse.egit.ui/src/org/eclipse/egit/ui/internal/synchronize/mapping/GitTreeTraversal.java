/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;

class GitTreeTraversal extends ResourceTraversal {

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace().getRoot();

	public GitTreeTraversal(GitModelTree modelTree) {
		super(getResourcesImpl(modelTree.getChildren()), IResource.DEPTH_INFINITE,
				IResource.NONE);
	}

	private static IResource[] getResourcesImpl(GitModelObject[] children) {
		List<IResource> result = new ArrayList<>(children.length);

		for (GitModelObject object : children) {
			IPath location = object.getLocation();
			IResource resource;
			if (object.isContainer())
				resource = ROOT.getContainerForLocation(location);
			else
				resource = ROOT.getFileForLocation(location);

			if (resource != null)
				result.add(resource);
		}

		return result.toArray(new IResource[result.size()]);
	}

}
