/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.core.variants.ResourceVariantTree;

abstract class GitResourceVariantTree extends ResourceVariantTree {

	private final GitSynchronizeDataSet gsds;

	GitResourceVariantTree(ResourceVariantByteStore store,
			GitSynchronizeDataSet gsds) {
		super(store);
		this.gsds = gsds;
	}

	public IResource[] roots() {
		Set<IResource> roots = new HashSet<IResource>();
		for (GitSynchronizeData gsd : gsds)
			roots.addAll(gsd.getProjects());

		return roots.toArray(new IResource[roots.size()]);
	}

	@Override
	protected IResourceVariant fetchVariant(IResource resource, int depth,
			IProgressMonitor monitor) throws TeamException {
		return null;
	}

	@Override
	protected IResourceVariant[] fetchMembers(IResourceVariant variant,
			IProgressMonitor progress) throws TeamException {
		if (variant == null || !(variant instanceof GitFolderResourceVariant))
			return new IResourceVariant[0];

		GitFolderResourceVariant gitVariant = (GitFolderResourceVariant) variant;

		try {
			return gitVariant.getMembers(progress);
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.GitResourceVariantTree_couldNotFetchMembers,
					gitVariant), e);
		}
	}

	public IResourceVariant getResourceVariant(final IResource resource)
			throws TeamException {
		return fetchVariant(resource, 0, null);
	}

	/**
	 *
	 * @param gsd
	 * @return instance of {@link RevTree} for given {@link GitSynchronizeData}
	 *         or <code>null</code> if rev tree was not found
	 * @throws TeamException
	 */
	protected abstract RevCommit getRevCommit(GitSynchronizeData gsd)
			throws TeamException;
}
