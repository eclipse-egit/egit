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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;

class GitTreeMapping extends GitObjectMapping {

	private final GitModelTree tree;

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace()
			.getRoot();

	protected GitTreeMapping(GitModelTree object) {
		super(object);
		tree = object;
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		GitModelObject[] objects = tree.getChildren();
		ResourceTraversal[] result = new ResourceTraversal[objects.length];

		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof GitModelTree)
				result[i] = new GitTreeTraversal((GitModelTree) objects[i]);
			else {
				IResource[] resources = getResources((GitModelBlob) objects[i]);
				result[i] = new ResourceTraversal(resources,
						IResource.DEPTH_ZERO, IResource.ALLOW_MISSING_LOCAL);
			}
		}

		return result;
	}

	private IResource[] getResources(GitModelBlob modelBlob) {
		IFile file = ROOT.getFileForLocation(modelBlob.getLocation());
		if (file == null)
			file = ROOT.getFile(modelBlob.getLocation());

		return new IResource[] { file };
	}

}
