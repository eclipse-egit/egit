/*******************************************************************************
 * Copyright (C) 2010, 2012 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.egit.ui.internal.synchronize.GitChangeSetModelProvider;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObjectContainer;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.egit.ui.internal.synchronize.model.HasProjects;

/**
 * Maps Git's objects onto proper {@link ResourceMapping} instants. It allows
 * traverse over repository content provides resources for Git ChangeSet.
 */
public abstract class GitObjectMapping extends ResourceMapping {

	private final GitModelObject object;

	/**
	 * @param object
	 * @return resource mapping
	 */
	public static ResourceMapping create(GitModelObject object) {
		if (object instanceof GitModelBlob)
			return new GitBlobMapping((GitModelBlob) object);
		if (object instanceof GitModelTree)
			return new GitTreeMapping((GitModelTree) object);
		if (object instanceof GitModelObjectContainer)
			return new GitContainerMapping((GitModelObjectContainer) object);

		return null;
	}

	/**
	 * @param parent instance of parent object
	 */
	protected GitObjectMapping(GitModelObject parent) {
		this.object = parent;
	}

	@Override
	public boolean contains(ResourceMapping mapping) {
		if (mapping.getModelProviderId().equals(getModelProviderId())) {
			GitModelObject obj = (GitModelObject) mapping.getModelObject();
			return obj.repositoryHashCode() == object.repositoryHashCode();
		}

		return false;
	}

	@Override
	public Object getModelObject() {
		return object;
	}

	@Override
	public String getModelProviderId() {
		return GitChangeSetModelProvider.ID;
	}

	private IProject getProject(final IResource resource) {
		return resource != null ? resource.getProject() : null;
	}

	@Override
	public IProject[] getProjects() {
		IProject[] projects = null;
		if (!object.isContainer()) {
			IProject project = getProject(ResourcesPlugin.getWorkspace()
					.getRoot().getFileForLocation(object.getLocation()));
			if (project != null)
				projects = new IProject[] { project };
		} else if (object instanceof GitModelTree) {
			IProject project = getProject(ResourcesPlugin.getWorkspace()
					.getRoot().getContainerForLocation(object.getLocation()));
			if (project != null)
				projects = new IProject[] { project };
		} else if (object instanceof HasProjects)
			projects = ((HasProjects) object).getProjects();

		return projects != null ? projects : new IProject[0];
	}
}
