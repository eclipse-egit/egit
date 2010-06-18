/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.storage.file.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * Tell JGit to ignore changes in selected files
 */
public class AssumeUnchangedOperation implements IEGitOperation {
	private final Collection<? extends IResource> rsrcList;

	private final IdentityHashMap<Repository, DirCache> caches;

	private final IdentityHashMap<RepositoryMapping, Object> mappings;

	/**
	 * Create a new operation to ignore changes in tracked files
	 *
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be ignored when
	 *            looking for changes or committing.
	 */
	public AssumeUnchangedOperation(final Collection<? extends IResource> rsrcs) {
		rsrcList = rsrcs;
		caches = new IdentityHashMap<Repository, DirCache>();
		mappings = new IdentityHashMap<RepositoryMapping, Object>();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor m) throws CoreException {
		if (m == null)
			m = new NullProgressMonitor();

		caches.clear();
		mappings.clear();

		m.beginTask(CoreText.AssumeUnchangedOperation_adding,
				rsrcList.size() * 200);
		try {
			for (IResource resource : rsrcList) {
				assumeValid(resource);
				m.worked(200);
			}

			for (Map.Entry<Repository, DirCache> e : caches.entrySet()) {
				final Repository db = e.getKey();
				final DirCache editor = e.getValue();
				m.setTaskName(NLS.bind(
						CoreText.AssumeUnchangedOperation_writingIndex, db
								.getDirectory()));
				editor.write();
				editor.commit();
			}
		} catch (RuntimeException e) {
			throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, e));
		} catch (IOException e) {
			throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, e));
		} finally {
			for (final RepositoryMapping rm : mappings.keySet())
				rm.fireRepositoryChanged();
			caches.clear();
			mappings.clear();
			m.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		return new MultiRule(rsrcList.toArray(new IResource[rsrcList.size()]));
	}

	private void assumeValid(final IResource resource) throws CoreException {
		final IProject proj = resource.getProject();
		final GitProjectData pd = GitProjectData.get(proj);
		if (pd == null)
			return;
		final RepositoryMapping rm = pd.getRepositoryMapping(resource);
		if (rm == null)
			return;
		final Repository db = rm.getRepository();

		DirCache cache = caches.get(db);
		if (cache == null) {
			try {
				cache = DirCache.lock(db);
			} catch (IOException err) {
				throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, err));
			}
			caches.put(db, cache);
			mappings.put(rm, rm);
		}

		final String path = rm.getRepoRelativePath(resource);
		if (resource instanceof IContainer) {
			for (final DirCacheEntry ent : cache.getEntriesWithin(path))
				ent.setAssumeValid(true);
		} else {
			final DirCacheEntry ent = cache.getEntry(path);
			if (ent != null)
				ent.setAssumeValid(true);
		}
	}
}
