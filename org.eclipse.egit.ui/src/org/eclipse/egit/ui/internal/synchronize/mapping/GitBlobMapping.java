/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;

class GitBlobMapping extends GitObjectMapping {

	private final GitModelBlob gitBlob;

	private final static IWorkspaceRoot workspaceRoot = ResourcesPlugin
			.getWorkspace().getRoot();

	GitBlobMapping(GitModelBlob gitBlob) {
		super(gitBlob);
		this.gitBlob = gitBlob;
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		IPath path = gitBlob.getLocation();
		IResource file = workspaceRoot.getFileForLocation(path);

		if (file != null)
			return new ResourceTraversal[] { new ResourceTraversal(
					new IResource[] { file }, IResource.DEPTH_ZERO,
					IResource.ALLOW_MISSING_LOCAL) };

		return new ResourceTraversal[0];
	}

}
