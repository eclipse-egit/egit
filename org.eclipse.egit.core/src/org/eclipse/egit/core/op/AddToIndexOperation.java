/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.GitIndex.Entry;

/**
 */
public class AddToIndexOperation implements IWorkspaceRunnable {
	private final Collection rsrcList;

	private final IdentityHashMap<RepositoryMapping, Object> mappings;

	/**
	 * Create a new operation to add files to the Git index
	 *
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be added to the
	 *            relevant Git repositories.
	 */
	public AddToIndexOperation(final Collection rsrcs) {
		rsrcList = rsrcs;
		mappings = new IdentityHashMap<RepositoryMapping, Object>();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime
	 * .IProgressMonitor)
	 */
	public void run(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;
		Collection<GitIndex> changedIndexes = new ArrayList<GitIndex>();
		// GitIndex can not be updated if it contains staged entries
		Collection<GitIndex> indexesWithStagedEntries = new ArrayList<GitIndex>();
		try {
			for (Object obj : rsrcList) {
				obj = ((IAdaptable) obj).getAdapter(IResource.class);
				if (obj instanceof IResource)
					addToIndex((IResource) obj, changedIndexes,
							indexesWithStagedEntries);
				monitor.worked(200);
			}
			if (!changedIndexes.isEmpty()) {
				for (GitIndex idx : changedIndexes) {
					idx.write();
				}

			}
		} catch (RuntimeException e) {
			throw Activator.error(CoreText.AddToIndexOperation_failed, e);
		} catch (IOException e) {
			throw Activator.error(CoreText.AddToIndexOperation_failed, e);
		} finally {
			for (final RepositoryMapping rm : mappings.keySet())
				rm.fireRepositoryChanged();
			mappings.clear();
			monitor.done();
		}
	}

	private void addToIndex(IResource resource,
			Collection<GitIndex> changedIndexes,
			Collection<GitIndex> indexesWithStagedEntries) throws IOException {
		IProject project = resource.getProject();
		RepositoryMapping map = RepositoryMapping.getMapping(project);
		Repository repo = map.getRepository();
		GitIndex index = null;
		index = repo.getIndex();
		Entry entry = index.getEntry(map.getRepoRelativePath(resource));
		if (entry == null)
			return;
		if (indexesWithStagedEntries.contains(index)) {
			return;
		} else {
			if (!canUpdateIndex(index)) {
				indexesWithStagedEntries.add(index);
				return;
			}
		}
		if (entry.isModified(map.getWorkDir())) {
			entry.update(new File(map.getWorkDir(), entry.getName()));
			if (!changedIndexes.contains(index))
				changedIndexes.add(index);
		}
	}

	/**
	 * The method checks if the given index can be updated. The index can be
	 * updated if it does not contain staged entries.
	 *
	 * @param index
	 * @return true if the given index can be updated
	 */
	private static boolean canUpdateIndex(GitIndex index) {
		Entry[] members = index.getMembers();
		for (int i = 0; i < members.length; i++) {
			if (members[i].getStage() != 0)
				return false;
		}
		return true;
	}

}
