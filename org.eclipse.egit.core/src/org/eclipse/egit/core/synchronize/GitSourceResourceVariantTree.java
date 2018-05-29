/*******************************************************************************
 * Copyright (c) 2013 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Laurent Goubet <laurent.goubet@obeo.fr> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.SessionResourceVariantByteStore;

class GitSourceResourceVariantTree extends GitResourceVariantTree {
	public GitSourceResourceVariantTree(GitSyncCache cache,
			GitSynchronizeDataSet gsds) {
		super(new SessionResourceVariantByteStore(), cache, gsds);
	}

	@Override
	protected IResourceVariant fetchVariant(IResource resource, int depth,
			IProgressMonitor monitor) throws TeamException {
		if (resource != null) {
			GitSynchronizeData data = gsds.getData(resource.getProject());
			if (data != null && data.shouldIncludeLocal())
				return new GitLocalResourceVariant(resource);
		}

		return super.fetchVariant(resource, depth, monitor);
	}

	@Override
	protected IResourceVariant[] fetchMembers(IResourceVariant variant,
			IProgressMonitor progress) throws TeamException {
		if (variant instanceof GitLocalResourceVariant
				&& ((GitLocalResourceVariant) variant).getResource() instanceof IContainer) {
			IContainer resource = (IContainer) ((GitLocalResourceVariant) variant)
					.getResource();
			try {
				IResource[] children = resource.members();
				IResourceVariant[] result = new IResourceVariant[children.length];
				for (int i = 0; i < children.length; i++)
					result[i] = new GitLocalResourceVariant(children[i]);
				return result;
			} catch (CoreException e) {
				// fall back to using remote data
			}
		}
		return super.fetchMembers(variant, progress);
	}

	@Override
	protected ObjectId getObjectId(ThreeWayDiffEntry diffEntry) {
		return diffEntry.getLocalId().toObjectId();
	}

	@Override
	protected RevCommit getCommitId(GitSynchronizeData gsd) {
		return gsd.getSrcRevCommit();
	}
}
