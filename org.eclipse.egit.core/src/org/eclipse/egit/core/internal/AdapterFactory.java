/*******************************************************************************
 * Copyright (c) 2018, 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.egit.core.info.GitInfo;
import org.eclipse.egit.core.info.GitItemState;
import org.eclipse.egit.core.internal.info.GitItemStateFactory;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * An {@link IAdapterFactory} for core items.
 */
public class AdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == Repository.class) {
			if (adaptableObject instanceof GitFileRevision) {
				return adapterType.cast(
						((GitFileRevision) adaptableObject).getRepository());
			}
		} else if (adapterType == GitInfo.class) {
			if (adaptableObject instanceof IResource
					&& ((IResource) adaptableObject)
							.getType() != IResource.ROOT) {
				return adapterType
						.cast(new GitAccessor((IResource) adaptableObject));
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { Repository.class, GitInfo.class };
	}

	private static class GitAccessor implements GitInfo {

		@NonNull
		private final IResource resource;

		GitAccessor(@NonNull IResource resource) {
			this.resource = resource;
		}

		@Override
		public Repository getRepository() {
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			return mapping != null ? mapping.getRepository() : null;
		}

		@Override
		public String getGitPath() {
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			return mapping != null ? mapping.getRepoRelativePath(resource)
					: null;
		}

		@Override
		public Source getSource() {
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			return mapping == null ? null : Source.WORKING_TREE;
		}

		@Override
		public AnyObjectId getCommitId() {
			return null;
		}

		@Override
		public GitItemState getGitState() {
			return GitItemStateFactory.getInstance().get(resource);
		}
	}
}
