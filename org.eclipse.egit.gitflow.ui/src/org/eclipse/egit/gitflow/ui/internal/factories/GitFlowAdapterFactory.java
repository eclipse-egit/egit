/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.factories;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Repository;

/**
 * Get JGit repository for element selected in Git Flow UI.
 */
public class GitFlowAdapterFactory implements IAdapterFactory {
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		Repository repository = null;
		if (adaptableObject instanceof IResource) {
			IResource resource = (IResource) adaptableObject;
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(resource.getProject());
			repository = repositoryMapping.getRepository();
		} else if (adaptableObject instanceof PlatformObject) {
			PlatformObject platformObject = (PlatformObject) adaptableObject;
			repository = Utils.getAdapter(platformObject, Repository.class);
		} else {
			throw new IllegalStateException();
		}

		return repository;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class[] getAdapterList() {
		return new Class[] { IResource.class, RepositoryTreeNode.class };
	}

}
