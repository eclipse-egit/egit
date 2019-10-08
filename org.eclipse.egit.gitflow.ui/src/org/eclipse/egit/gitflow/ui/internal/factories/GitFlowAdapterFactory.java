/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.factories;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;

/**
 * Get JGit repository for element selected in Git Flow UI.
 */
public class GitFlowAdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (Repository.class.equals(adapterType)) {
			Repository repository = null;
			if (adaptableObject instanceof IResource) {
				IResource resource = (IResource) adaptableObject;
				repository = getRepository(resource);
			} else if (adaptableObject instanceof ISelection) {
				IStructuredSelection structuredSelection = SelectionUtils
						.getStructuredSelection((ISelection) adaptableObject);
				repository = SelectionUtils.getRepository(structuredSelection);
			} else {
				return null;
			}

			return adapterType.cast(repository);
		}
		return null;
	}

	private Repository getRepository(IResource resource) {
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		return mapping != null ? mapping.getRepository() : null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { Repository.class };
	}
}
