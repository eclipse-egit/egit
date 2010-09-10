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
import static org.eclipse.core.resources.IResource.DEPTH_ZERO;

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
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;

class GitCacheMapping extends GitObjectMapping {

	private final GitModelCache gitIndex;

	public GitCacheMapping(GitModelCache gitIndex) {
		super(gitIndex);
		this.gitIndex = gitIndex;
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		List<ResourceTraversal> result = new ArrayList<ResourceTraversal>();

		for (GitModelObject child : gitIndex.getChildren()) {
			ResourceTraversal traversal;

			IPath location = child.getLocation();
			if (child instanceof GitModelBlob) {
				IFile file = root.getFileForLocation(location);

				if (file == null)
					continue;

				traversal = new ResourceTraversal(new IResource[] { file },
						DEPTH_ZERO, ALLOW_MISSING_LOCAL);
			} else {
				IContainer container = root.getContainerForLocation(location);

				if (container == null)
					continue;

				traversal = new ResourceTraversal(
						new IResource[] { container },
						IResource.DEPTH_INFINITE, ALLOW_MISSING_LOCAL);
			}

			result.add(traversal);
		}

		return result.toArray(new ResourceTraversal[result.size()]);
	}

}
