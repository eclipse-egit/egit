/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.Team;

/**
 * Add one or more new files/folders to the Git repository.
 * <p>
 * Accepts a collection of resources (files and/or directories) which should be
 * added to the their corresponding Git repositories. Resources in the
 * collection can be associated with multiple repositories. The operation will
 * automatically associate each resource with the nearest containing Git
 * repository.
 * </p>
 * <p>
 * Resources are only scheduled for addition in the index.
 * </p>
 */
public class TrackOperation implements IEGitOperation {
	private final IResource[] rsrcList;

	/**
	 * Create a new operation to track additional files/folders.
	 *
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be added to the
	 *            relevant Git repositories.
	 */
	public TrackOperation(IResource[] rsrcs) {
		rsrcList = rsrcs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		return new MultiRule(rsrcList);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor m) throws CoreException {
		if (m == null) {
			m = new NullProgressMonitor();
		}

		final IdentityHashMap<RepositoryMapping, Boolean> tomerge = new IdentityHashMap<RepositoryMapping, Boolean>();
		m.beginTask(CoreText.AddOperation_adding, rsrcList.length * 200);
		try {
			for (IResource toAdd : rsrcList) {
					final RepositoryMapping rm = RepositoryMapping.getMapping(toAdd);
					final GitIndex index = rm.getRepository().getIndex();

					if (toAdd instanceof IFile) {
						String repoPath = rm.getRepoRelativePath(toAdd);
						Entry entry = index.getEntry(repoPath);
						if (entry != null) {
							if (!entry.isAssumedValid()) {
								// TODO is this the right location?
								if (GitTraceLocation.CORE.isActive())
									GitTraceLocation
											.getTrace()
											.trace(
													GitTraceLocation.CORE
															.getLocation(),
													"Already tracked - skipping"); //$NON-NLS-1$
								continue;
							}
						}
					}

					tomerge.put(rm, Boolean.TRUE);
					if (toAdd instanceof IContainer) {
						((IContainer)toAdd).accept(new IResourceVisitor() {
							public boolean visit(IResource resource) throws CoreException {
								try {
									String repoPath = rm.getRepoRelativePath(resource);
									// We use add to reset the assume valid bit, so we check the bit
									// first. If a resource within a ignored folder is marked
									// we ignore it here, i.e. there is no way to unmark it expect
									// by explicitly selecting and invoking track on it.
									boolean isIgnored = Team.isIgnoredHint(resource);
									if (resource.getType() == IResource.FILE) {
										Entry entry = index.getEntry(repoPath);
										if (!isIgnored || entry != null && entry.isAssumedValid()) {
											entry = index.add(rm.getWorkTree(), new File(rm.getWorkTree(), repoPath));
											entry.setAssumeValid(false);
										}
									}
									if (isIgnored)
										return false;

								} catch (IOException e) {
									if (GitTraceLocation.CORE.isActive())
										GitTraceLocation.getTrace().trace(GitTraceLocation.CORE.getLocation(), e.getMessage(), e);
									throw new CoreException(Activator.error(CoreText.AddOperation_failed, e));
								}
								return true;
							}
						},IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
					} else {
						Entry entry = index.add(rm.getWorkTree(), new File(rm.getWorkTree(),rm.getRepoRelativePath(toAdd)));
						entry.setAssumeValid(false);

					}
				m.worked(200);
			}
			for (RepositoryMapping rm : tomerge.keySet()) {
				m.setTaskName(NLS.bind(CoreText.TrackOperation_writingIndex, rm.getRepository().getDirectory()));
				rm.getRepository().getIndex().write();
			}
		} catch (RuntimeException e) {
			if (GitTraceLocation.CORE.isActive())
				GitTraceLocation.getTrace().trace(GitTraceLocation.CORE.getLocation(), e.getMessage(), e);
			throw new CoreException(Activator.error(CoreText.AddOperation_failed, e));
		} catch (IOException e) {
			if (GitTraceLocation.CORE.isActive())
				GitTraceLocation.getTrace().trace(GitTraceLocation.CORE.getLocation(), e.getMessage(), e);
			throw new CoreException(Activator.error(CoreText.AddOperation_failed, e));
		} finally {
			try {
				final Iterator i = tomerge.keySet().iterator();
				while (i.hasNext()) {
					final RepositoryMapping r = (RepositoryMapping) i.next();
					r.getRepository().getIndex().read();
					r.fireRepositoryChanged();
				}
			} catch (IOException e) {
				if (GitTraceLocation.CORE.isActive())
					GitTraceLocation.getTrace().trace(GitTraceLocation.CORE.getLocation(), e.getMessage(), e);
			} finally {
				m.done();
			}
		}
	}
}
