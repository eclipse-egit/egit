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
import static org.eclipse.core.resources.IResource.DEPTH_ONE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace().getRoot();

	public GitContainerMapping(GitModelObjectContainer gitCommit) {
		super(gitCommit);
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		GitModelObject[] children = ((GitModelObjectContainer) getModelObject())
				.getChildren();
		List<ResourceTraversal> result = new ArrayList<ResourceTraversal>();

		for (GitModelObject child : children) {
			if (child.isContainer())
				result.addAll(createTraversalForContainer(child));
			else
				result.add(createTraversalForFile(child));
		}
		result.removeAll(Collections.singleton(null));
		return result.toArray(new ResourceTraversal[result.size()]);
	}

	private List<ResourceTraversal> createTraversalForContainer(GitModelObject child) {
		GitModelObject[] containerChildren = child.getChildren();
		List<ResourceTraversal> result = new ArrayList<ResourceTraversal>();
		for (GitModelObject aChild : containerChildren) {
			if(aChild.isContainer())
				result.addAll(createTraversalForContainer(aChild));
			else
				result.add(createTraversalForFile(aChild));
		}
		return result;
	}

	private ResourceTraversal createTraversalForFile(GitModelObject aChild) {
		IPath childLocation = aChild.getLocation();
		IFile file = ROOT.getFileForLocation(childLocation);

		if (file == null) {
			file = ROOT.getFile(childLocation);
		}
		ResourceTraversal traversal = new ResourceTraversal(
				new IResource[] { file }, DEPTH_ONE, ALLOW_MISSING_LOCAL);
		return traversal;
	}

}
