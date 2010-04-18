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

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantTree;

class GitBranchResourceVariantTreeSubscriber extends
		GitResourceVariantTreeSubscriber {

	/**
	 * A resource variant tree of the remote branch(es).
	 */
	private IResourceVariantTree remoteTree;

	/**
	 * A resource variant tree against HEAD.
	 */
	private IResourceVariantTree baseTree;

	private Map<Repository, String> branches;

	GitBranchResourceVariantTreeSubscriber(Map<Repository, String> branches,
			IResource[] roots) {
		this.branches = branches;
		setRoots(roots);
	}

	void reset(Map<Repository, String> branches, IResource[] roots) {
		this.branches = branches;
		setRoots(roots);

		baseTree = null;
		remoteTree = null;
	}

	@Override
	protected IResourceVariantTree getBaseTree() {
		if (baseTree == null) {
			baseTree = new GitHeadResourceVariantTree(roots());
		}
		return baseTree;
	}

	@Override
	protected IResourceVariantTree getRemoteTree() {
		if (remoteTree == null) {
			remoteTree = new GitBranchResourceVariantTree(branches, roots());
		}
		return remoteTree;
	}

	@Override
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote) throws TeamException {
		GitSyncInfo info = new GitSyncInfo(local, base, remote,
				getResourceComparator());
		info.init();
		return info;
	}

	@Override
	public String getName() {
		return "Git Branches";
	}

}
