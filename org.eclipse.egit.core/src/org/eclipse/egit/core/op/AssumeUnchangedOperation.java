/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * Tell JGit to ignore changes in selected files
 */
public class AssumeUnchangedOperation implements IEGitOperation {
	private final Collection<? extends IResource> rsrcList;
	private final Collection<IPath> locations;

	private Repository db;

	private final IdentityHashMap<Repository, DirCache> caches;

	private final boolean assumeUnchanged;

	/**
	 * Create a new operation to ignore changes in tracked files
	 *
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be ignored when
	 *            looking for changes or committing.
	 * @param assumeUnchanged
	 *            {@code true} to set the assume-valid flag in the git index
	 *            entry, {@code false} to unset the assume-valid flag in the git
	 *            index entry
	 */
	public AssumeUnchangedOperation(
			final Collection<? extends IResource> rsrcs, boolean assumeUnchanged) {
		rsrcList = rsrcs;
		locations = Collections.emptyList();
		caches = new IdentityHashMap<>();
		this.assumeUnchanged = assumeUnchanged;
	}

	/**
	 * Create a new operation to ignore changes in tracked files
	 *
	 * @param repository
	 *            a Git repository
	 * @param locations
	 *            collection of {@link IPath}s which should be ignored when
	 *            looking for changes or committing.
	 * @param assumeUnchanged
	 *            {@code true} to set the assume-valid flag in the git index
	 *            entry, {@code false} to unset the assume-valid flag in the git
	 *            index entry
	 */
	public AssumeUnchangedOperation(final Repository repository,
			final Collection<IPath> locations,
			boolean assumeUnchanged) {
		this.db = repository;
		this.locations = locations;
		this.rsrcList = Collections.emptyList();
		caches = new IdentityHashMap<>();
		this.assumeUnchanged = assumeUnchanged;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, (rsrcList.size() + locations.size()) * 2);
		progress.setTaskName(CoreText.AssumeUnchangedOperation_adding);

		caches.clear();

		try {
			for (IResource resource : rsrcList) {
				assumeValid(resource);
				progress.worked(1);
			}
			for (IPath location : locations) {
				assumeValid(location);
				progress.worked(1);
			}

			progress.setWorkRemaining(caches.size());
			for (Map.Entry<Repository, DirCache> e : caches.entrySet()) {
				final Repository db = e.getKey();
				final DirCache editor = e.getValue();
				progress.setTaskName(NLS.bind(
						CoreText.AssumeUnchangedOperation_writingIndex, db
								.getDirectory()));
				editor.write();
				editor.commit();
				progress.worked(1);
			}
		} catch (RuntimeException e) {
			throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, e));
		} catch (IOException e) {
			throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, e));
		} finally {
			for (DirCache cache : caches.values()) {
				cache.unlock();
			}
			caches.clear();
		}
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRuleForRepositories(rsrcList.toArray(new IResource[rsrcList.size()]));
	}

	private void assumeValid(final IResource resource) throws CoreException {
		final IProject proj = resource.getProject();
		if (proj == null) {
			return;
		}
		final GitProjectData pd = GitProjectData.get(proj);
		if (pd == null) {
			return;
		}
		final RepositoryMapping rm = pd.getRepositoryMapping(resource);
		if (rm == null) {
			return;
		}
		this.db = rm.getRepository();
		assumeValid(resource.getLocation());
	}

	private void assumeValid(final IPath location) throws CoreException {
		DirCache cache = caches.get(db);
		if (cache == null) {
			try {
				cache = db.lockDirCache();
			} catch (IOException err) {
				throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, err));
			}
			caches.put(db, cache);
		}

		IPath dbDir = new Path(db.getWorkTree().getAbsolutePath());
		final String path = location.makeRelativeTo(dbDir).toString();
		if (location.toFile().isDirectory()) {
			for (DirCacheEntry ent : cache.getEntriesWithin(path)) {
				ent.setAssumeValid(assumeUnchanged);
			}
		} else {
			DirCacheEntry ent = cache.getEntry(path);
			if (ent != null) {
				ent.setAssumeValid(assumeUnchanged);
			}
		}
	}
}
