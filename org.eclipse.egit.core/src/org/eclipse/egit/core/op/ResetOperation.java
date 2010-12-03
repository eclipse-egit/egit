/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * A class for changing a ref and possibly index and workdir too.
 */
public class ResetOperation implements IEGitOperation {
	/**
	 * Kind of reset
	 */
	public enum ResetType {
		/**
		 * Just change the ref. The index and workdir are not changed.
		 */
		SOFT,

		/**
		 * Change the ref and the index. The workdir is not changed.
		 */
		MIXED,

		/**
		 * Change the ref, the index and the workdir
		 */
		HARD
	}

	private final Repository repository;
	private final String refName;
	private final ResetType type;

	private RevCommit commit;

	/**
	 * Construct a {@link ResetOperation}
	 *
	 * @param repository
	 * @param refName
	 * @param type
	 */
	public ResetOperation(Repository repository, String refName, ResetType type) {
		this.repository = repository;
		this.refName = refName;
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		if (type == ResetType.HARD)
			return ResourcesPlugin.getWorkspace().getRoot();
		else
			return null;
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
		if (type == ResetType.HARD) {
			IWorkspaceRunnable action = new IWorkspaceRunnable() {
				public void run(IProgressMonitor actMonitor) throws CoreException {
					reset(actMonitor);
				}
			};
			// lock workspace to protect working tree changes
			ResourcesPlugin.getWorkspace().run(action, monitor);
		} else {
			reset(monitor);
		}
	}

	private void reset(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(NLS.bind(CoreText.ResetOperation_performingReset,
				type.toString().toLowerCase(), refName), 3);

		IProject[] validProjects = null;
		if (type == ResetType.HARD)
			validProjects = ProjectUtil.getValidProjects(repository);
		boolean merging = false;
		if (repository.getRepositoryState().equals(RepositoryState.MERGING)
				|| repository.getRepositoryState().equals(
						RepositoryState.MERGING_RESOLVED))
			merging = true;

		mapObjects();
		monitor.worked(1);

		writeRef();
		monitor.worked(1);

		switch (type) {
		case HARD:
			checkoutIndex();
			monitor.worked(1);
			if (merging)
				resetMerge();
			monitor.worked(1);
			// only refresh if working tree changes
			ProjectUtil.refreshValidProjects(validProjects, new SubProgressMonitor(
					monitor, 1));
			monitor.worked(1);
			break;

		case MIXED:
			// Change the
			resetIndex();
			monitor.worked(2);
			if (merging)
				resetMerge();
			monitor.worked(1);
			break;

		case SOFT:
			// only change the ref
			monitor.worked(3);
		}
		monitor.done();
	}

	private void resetMerge() throws CoreException {
		try {
			repository.writeMergeHeads(null);
			repository.writeMergeCommitMsg(null);
		} catch (IOException e) {
			throw new TeamException(CoreText.ResetOperation_resetMergeFailed, e);
		}
	}

	private void mapObjects() throws TeamException {
		final ObjectId commitId;
		try {
			commitId = repository.resolve(refName);
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.ResetOperation_lookingUpRef, refName), e);
		}
		RevWalk rw = new RevWalk(repository);
		try {
			commit = rw.parseCommit(commitId);
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.ResetOperation_lookingUpCommit, commitId), e);
		} finally {
			rw.release();
		}
	}

	private void writeRef() throws TeamException {
		try {
			final RefUpdate ru = repository.updateRef(Constants.HEAD);
			ru.setNewObjectId(commit.getId());
			String name = refName;
			if (name.startsWith("refs/heads/"))  //$NON-NLS-1$
				name = name.substring(11);
			if (name.startsWith("refs/remotes/"))  //$NON-NLS-1$
				name = name.substring(13);
			String message = "reset --" //$NON-NLS-1$
					+ type.toString().toLowerCase() + " " + name; //$NON-NLS-1$
			ru.setRefLogMessage(message, false);
			if (ru.forceUpdate() == RefUpdate.Result.LOCK_FAILURE)
				throw new TeamException(NLS.bind(
						CoreText.ResetOperation_cantUpdate, ru.getName()));
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.ResetOperation_updatingFailed, Constants.HEAD), e);
		}
	}

	private void resetIndex() throws TeamException {
		DirCache dc = null;
		try {
			dc = repository.lockDirCache();
			dc.clear();
			DirCacheBuilder dcb = dc.builder();
			dcb.addTree(new byte[0], 0, repository.newObjectReader(), commit
					.getTree());
			dcb.commit();
		} catch (IOException e) {
			throw new TeamException(CoreText.ResetOperation_readingIndex, e);
		} finally {
			if (dc != null)
				dc.unlock();
		}
	}

	private void checkoutIndex() throws TeamException {
		DirCache dc = null;
		try {
			dc = repository.lockDirCache();
			DirCacheCheckout checkout = new DirCacheCheckout(repository, dc,
					commit.getTree());
			checkout.setFailOnConflict(false);
			checkout.checkout();
		} catch (IOException e) {
			throw new TeamException(
					CoreText.ResetOperation_mappingTreeForCommit, e);
		} finally {
			if (dc != null)
				dc.unlock();
		}
	}
}

