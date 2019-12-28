/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
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
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * Remove one or more existing files/folders from the Git repository.
 * <p>
 * Accepts a collection of resources (files and/or directories) which should be
 * removed from the their corresponding Git repositories. Resources in the
 * collection can be associated with multiple repositories. The operation will
 * automatically remove each resource from the correct Git repository.
 * </p>
 * <p>
 * Resources are only scheduled for removal in the index-
 * </p>
 */
public class UntrackOperation implements IEGitOperation {
	private final Collection<? extends IResource> rsrcList;
	private final Collection<IPath> locations;

	private Repository db;

	private final IdentityHashMap<Repository, DirCacheEditor> edits;

	/**
	 * Create a new operation to stop tracking existing files/folders.
	 *
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be removed from
	 *            the relevant Git repositories.
	 */
	public UntrackOperation(final Collection<? extends IResource> rsrcs) {
		rsrcList = rsrcs;
		locations = Collections.emptyList();
		edits = new IdentityHashMap<>();
	}

	/**
	 * Create a new operation to stop tracking existing files/folders.
	 *
	 * @param repository
	 *            a Git repository
	 * @param locations
	 *            collection of {@link IPath}s which should be removed from the
	 *            relevant Git repositories.
	 */
	public UntrackOperation(final Repository repository,
			final Collection<IPath> locations) {
		rsrcList = Collections.emptyList();
		this.locations = locations;
		this.db = repository;
		edits = new IdentityHashMap<>();
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, (rsrcList.size() + locations.size()) * 2);
		progress.setTaskName(CoreText.UntrackOperation_adding);

		edits.clear();

		try {
			for (IResource obj : rsrcList) {
				remove(obj);
				progress.worked(1);
			}
			for (IPath location : locations) {
				remove(location);
				progress.worked(1);
			}

			progress.setWorkRemaining(edits.size());
			for (Map.Entry<Repository, DirCacheEditor> e : edits.entrySet()) {
				final DirCacheEditor editor = e.getValue();
				progress.setTaskName(
						NLS.bind(CoreText.UntrackOperation_writingIndex,
								db.getDirectory()));
				editor.commit();
				progress.worked(1);
			}
		} catch (RuntimeException | IOException e) {
			throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, e));
		} finally {
			for (DirCacheEditor editor : edits.values()) {
				if (editor.getDirCache() != null) {
					editor.getDirCache().unlock();
				}
			}
			edits.clear();
		}
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRuleForRepositories(rsrcList.toArray(new IResource[0]));
	}

	private void remove(final IResource resource) throws CoreException {
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
		db = rm.getRepository();

		remove(resource.getLocation());
	}

	private void remove(final IPath location) throws CoreException {
		DirCacheEditor e = edits.get(db);
		if (e == null) {
			try {
				e = db.lockDirCache().editor();
			} catch (IOException err) {
				throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, err));
			}
			edits.put(db, e);
		}

		IPath dbDir = new Path(db.getWorkTree().getAbsolutePath());
		String path = location.makeRelativeTo(dbDir).toString();
		if (location.toFile().isDirectory()) {
			e.add(new DirCacheEditor.DeleteTree(path));
		} else {
			e.add(new DirCacheEditor.DeletePath(path));
		}
	}
}
