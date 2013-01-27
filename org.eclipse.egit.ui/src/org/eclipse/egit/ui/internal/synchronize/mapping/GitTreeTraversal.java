/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

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
		IResource[] result = new IResource[children.length];

		for (int i = 0; i < children.length; i++) {
			IResource resource = null;
			IPath childPath = children[i].getLocation();
			if (children[i].isContainer())
				resource = ROOT.getContainerForLocation(childPath);
			else
				resource = ROOT.getFileForLocation(childPath);

			if (resource != null)
				result[i] = resource;
		}

		return result;

	}

}
