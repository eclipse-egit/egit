/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import static org.eclipse.core.resources.IResource.ALLOW_MISSING_LOCAL;
import static org.eclipse.core.resources.IResource.DEPTH_INFINITE;
import static org.eclipse.core.resources.IResource.DEPTH_ONE;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObjectContainer;

class GitContainerMapping extends GitObjectMapping {

	private final GitModelObject[] children;

	public GitContainerMapping(GitModelObjectContainer gitCommit) {
		super(gitCommit);
		children = gitCommit.getChildren();
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		List<ResourceTraversal> result = new ArrayList<ResourceTraversal>();

		for (GitModelObject child : children) {
			ResourceTraversal traversal;
			IPath location = child.getLocation();

			if (child.isContainer()) {
				IContainer container = root.getContainerForLocation(location);
				if (container == null)
					continue;

				traversal = new ResourceTraversal(
						new IResource[] { container }, DEPTH_INFINITE,
						ALLOW_MISSING_LOCAL);
			} else {
				IFile file = root.getFileForLocation(location);
				if (file == null)
					continue;

				traversal = new ResourceTraversal(new IResource[] { file },
						DEPTH_ONE, ALLOW_MISSING_LOCAL);
			}

			result.add(traversal);
		}

		return result.toArray(new ResourceTraversal[result.size()]);
	}

}
