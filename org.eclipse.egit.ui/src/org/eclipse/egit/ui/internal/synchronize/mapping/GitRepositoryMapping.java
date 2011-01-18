/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import static org.eclipse.core.resources.IResource.DEPTH_INFINITE;
import static org.eclipse.core.resources.IResource.NONE;

import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;

class GitRepositoryMapping extends GitObjectMapping {

	private ResourceTraversal[] travelsals;

	protected GitRepositoryMapping(GitModelRepository gitRepo) {
		super(gitRepo);
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		if (travelsals == null)
			travelsals = new ResourceTraversal[] { new ResourceTraversal(
					getProjects(), DEPTH_INFINITE, NONE) };

		return travelsals;
	}

}
