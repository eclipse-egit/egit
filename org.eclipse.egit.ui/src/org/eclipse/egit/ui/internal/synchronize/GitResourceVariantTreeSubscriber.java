/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.ResourceVariantTreeSubscriber;

abstract class GitResourceVariantTreeSubscriber extends
		ResourceVariantTreeSubscriber {

	private IResource[] roots;

	void setRoots(IResource[] roots) {
		this.roots = roots;
	}

	@Override
	public boolean isSupervised(IResource resource) throws TeamException {
		return true;
	}

	@Override
	public IResource[] roots() {
		if (roots != null) {
			return roots;
		}

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		List<IResource> resourceRoots = new ArrayList<IResource>(
				projects.length);

		for (IProject project : projects) {
			if (project.isOpen()) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(project);
				if (mapping != null) {
					resourceRoots.add(project);
				}
			}
		}

		roots = resourceRoots.toArray(new IResource[resourceRoots.size()]);
		return roots;
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return new GitResourceVariantComparator();
	}

}