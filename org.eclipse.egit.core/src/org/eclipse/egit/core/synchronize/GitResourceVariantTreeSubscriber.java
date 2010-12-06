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

import static java.util.Arrays.copyOf;
import static org.eclipse.team.core.Team.isIgnoredHint;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.IResourceVariantTree;
import org.eclipse.team.core.variants.ResourceVariantTreeSubscriber;

/**
 *
 */
public class GitResourceVariantTreeSubscriber extends
		ResourceVariantTreeSubscriber {

	/**
	 * A resource variant tree of the remote branch(es).
	 */
	private IResourceVariantTree remoteTree;

	/**
	 * A resource variant tree against HEAD.
	 */
	private IResourceVariantTree baseTree;

	private GitSynchronizeDataSet gsds;

	/**
	 * @param data
	 */
	public GitResourceVariantTreeSubscriber(GitSynchronizeDataSet data) {
		this.gsds = data;
	}

	private IResource[] roots;

	@Override
	public boolean isSupervised(IResource resource) throws TeamException {
		return gsds.contains(resource.getProject()) && !isIgnoredHint(resource);
	}

	@Override
	public IResource[] roots() {
		if (roots == null)
			roots = gsds.getAllProjects();

		return copyOf(roots, roots.length);
	}

	/**
	 * @param data
	 */
	public void reset(GitSynchronizeDataSet data) {
		gsds = data;

		roots = null;
		baseTree = null;
		remoteTree = null;
	}

	@Override
	public String getName() {
		return CoreText.GitBranchResourceVariantTreeSubscriber_gitRepository;
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return new GitResourceVariantComparator(gsds);
	}

	@Override
	protected IResourceVariantTree getBaseTree() {
		if (baseTree == null) {
			baseTree = new GitBaseResourceVariantTree(gsds);
		}
		return baseTree;
	}

	@Override
	protected IResourceVariantTree getRemoteTree() {
		if (remoteTree == null) {
			remoteTree = new GitRemoteResourceVariantTree(gsds);
		}
		return remoteTree;
	}

	@Override
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote) throws TeamException {
		GitSynchronizeData gsd = gsds.getData(local.getProject());

		SyncInfo info;
		if (gsd.shouldIncludeLocal())
			info = new SyncInfo(local, base, remote, getResourceComparator());
		else
			info = new GitSyncInfo(local, base, remote, getResourceComparator(), gsd);

		info.init();
		return info;
	}

}