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

import static org.eclipse.core.resources.IResource.ALLOW_MISSING_LOCAL;
import static org.eclipse.core.resources.IResource.DEPTH_ONE;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
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
		Set<ResourceTraversal> result = new LinkedHashSet<>();

		final GitSynchronizeDataSet dataSet;
		if (context instanceof GitSubscriberResourceMappingContext)
			dataSet = ((GitSubscriberResourceMappingContext) context)
					.getSyncData();
		else
			dataSet = null;

		for (GitModelObject child : children) {
			if (child.isContainer())
				result.addAll(createTraversalForContainer(child, dataSet));
			else
				result.add(createTraversalForFile(child, dataSet));
		}

		return result.toArray(new ResourceTraversal[result.size()]);
	}

	private Set<ResourceTraversal> createTraversalForContainer(
			GitModelObject child, GitSynchronizeDataSet dataSet) {
		GitModelObject[] containerChildren = child.getChildren();
		Set<ResourceTraversal> result = new LinkedHashSet<>();
		for (GitModelObject aChild : containerChildren) {
			if(aChild.isContainer())
				result.addAll(createTraversalForContainer(aChild, dataSet));
			else {
				ResourceTraversal traversal = createTraversalForFile(aChild,
						dataSet);
				if (traversal != null)
					result.add(traversal);
			}
		}
		return result;
	}

	private ResourceTraversal createTraversalForFile(GitModelObject aChild, GitSynchronizeDataSet dataSet) {
		IPath childLocation = aChild.getLocation();
		IFile file = ROOT.getFileForLocation(childLocation);

		if (file == null) {
			file = ROOT.getFile(childLocation);
		}

		ResourceTraversal traversal = null;
		if (dataSet == null)
			traversal = new ResourceTraversal(new IResource[] { file },
					DEPTH_ONE, ALLOW_MISSING_LOCAL);
		else if (file != null && dataSet.shouldBeIncluded(file))
			traversal = new ResourceTraversal(new IResource[] { file },
					DEPTH_ONE, ALLOW_MISSING_LOCAL);

		return traversal;
	}

}
