/*******************************************************************************
 * Copyright (C) 2007, IBM Corporation and others
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Andre Bossert <anb0s@anbos.de> - Cleaning up the DecoratableResourceAdapter
 *******************************************************************************/

package org.eclipse.egit.ui.internal.decorators;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.resources.IResourceState;
import org.eclipse.egit.ui.internal.resources.ResourceStateFactory;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

class DecoratableResourceAdapter extends DecoratableResource {

	public DecoratableResourceAdapter(@NonNull IndexDiffData indexDiffData,
			@NonNull IResource resourceToWrap) {
		super(resourceToWrap);
		boolean trace = GitTraceLocation.DECORATION.isActive();
		long start = 0;
		if (trace) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.DECORATION.getLocation(),
					"Decorate " + resourceToWrap.getFullPath()); //$NON-NLS-1$
			start = System.nanoTime();
		}
		try {
			RepositoryMapping mapping = RepositoryMapping
					.getMapping(resourceToWrap);
			if (mapping == null) {
				return;
			}
			Repository repository = mapping.getRepository();
			if (repository == null) {
				return;
			}
			setIsRepositoryContainer(resourceToWrap.equals(mapping.getContainer()));
			IResourceState baseState = ResourceStateFactory.getInstance()
					.get(indexDiffData, resourceToWrap);
			setTracked(baseState.isTracked());
			setIgnored(baseState.isIgnored());
			setDirty(baseState.isDirty());
			setConflicts(baseState.hasConflicts());
			setAssumeUnchanged(baseState.isAssumeUnchanged());
			setStagingState(baseState.getStagingState());
			if (isRepositoryContainer() && !isIgnored()) {
				// We only need this very expensive info for for decorating
				// projects and folders that are submodule or nested repository
				// roots
				repositoryName = DecoratorRepositoryStateCache.INSTANCE
						.getRepositoryNameAndState(repository);
				branch = DecoratorRepositoryStateCache.INSTANCE
						.getCurrentBranchLabel(repository);
				branchStatus = DecoratorRepositoryStateCache.INSTANCE
						.getBranchStatus(repository);
				RevCommit headCommit = DecoratorRepositoryStateCache.INSTANCE
						.getHeadCommit(repository);
				if (headCommit != null) {
					commitMessage = headCommit.getShortMessage();
				}
			}
		} finally {
			if (trace)
				GitTraceLocation
						.getTrace()
						.trace(GitTraceLocation.DECORATION.getLocation(),
								"Decoration took " + (System.nanoTime() - start) //$NON-NLS-1$
										+ " ns"); //$NON-NLS-1$
		}
	}

	@Override
	public String toString() {
		return "DecoratableResourceAdapter[" + getName() //$NON-NLS-1$
				+ (isTracked() ? ", tracked" : "") //$NON-NLS-1$ //$NON-NLS-2$
				+ (isIgnored() ? ", ignored" : "") //$NON-NLS-1$ //$NON-NLS-2$
				+ (isDirty() ? ", dirty" : "") //$NON-NLS-1$//$NON-NLS-2$
				+ (hasConflicts() ? ",conflicts" : "")//$NON-NLS-1$//$NON-NLS-2$
				+ ", staged=" + getStagingState() //$NON-NLS-1$
				+ "]"; //$NON-NLS-1$
	}

}
