/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.egit.ui.internal.synchronize.GitChangeSetModelProvider;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;

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
		if (object instanceof GitModelCommit)
			return new GitCommitMapping((GitModelCommit) object);
		if (object instanceof GitModelCache)
			return new GitCommitMapping((GitModelCache) object);
		if (object instanceof GitModelRepository)
			return new GitRepositoryMapping((GitModelRepository) object);

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
			return obj.getRepository().equals(object.getRepository());
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

	@Override
	public IProject[] getProjects() {
		return object.getProjects();
	}

}
