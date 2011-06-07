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

	private final IdentityHashMap<Repository, DirCacheEditor> edits;

	private final IdentityHashMap<RepositoryMapping, Object> mappings;

	/**
	 * Create a new operation to stop tracking existing files/folders.
	 *
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be removed from
	 *            the relevant Git repositories.
	 */
	public UntrackOperation(final Collection<? extends IResource> rsrcs) {
		rsrcList = rsrcs;
		edits = new IdentityHashMap<Repository, DirCacheEditor>();
		mappings = new IdentityHashMap<RepositoryMapping, Object>();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		edits.clear();
		mappings.clear();

		monitor.beginTask(CoreText.UntrackOperation_adding, rsrcList.size() * 200);
		try {
			for (IResource obj : rsrcList) {
				remove(obj);
				monitor.worked(200);
			}

			for (Map.Entry<Repository, DirCacheEditor> e : edits.entrySet()) {
				final Repository db = e.getKey();
				final DirCacheEditor editor = e.getValue();
				monitor.setTaskName(NLS.bind(CoreText.UntrackOperation_writingIndex, db.getDirectory()));
				editor.commit();
			}
		} catch (RuntimeException e) {
			throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, e));
		} catch (IOException e) {
			throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, e));
		} finally {
			for (final RepositoryMapping rm : mappings.keySet())
				rm.fireRepositoryChanged();
			edits.clear();
			mappings.clear();
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		return new MultiRule(rsrcList.toArray(new IResource[rsrcList.size()]));
	}

	private void remove(final IResource path) throws CoreException {
		if (path.isLinked(IResource.CHECK_ANCESTORS))
			return;
		final IProject proj = path.getProject();
		final GitProjectData pd = GitProjectData.get(proj);
		if (pd == null)
			return;
		final RepositoryMapping rm = pd.getRepositoryMapping(path);
		if (rm == null)
			return;
		final Repository db = rm.getRepository();

		DirCacheEditor e = edits.get(db);
		if (e == null) {
			try {
				e = db.lockDirCache().editor();
			} catch (IOException err) {
				throw new CoreException(Activator.error(CoreText.UntrackOperation_failed, err));
			}
			edits.put(db, e);
			mappings.put(rm, rm);
		}

		if (path instanceof IContainer)
			e.add(new DirCacheEditor.DeleteTree(rm.getRepoRelativePath(path)));
		else
			e.add(new DirCacheEditor.DeletePath(rm.getRepoRelativePath(path)));
	}
}
