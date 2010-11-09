/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
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
import java.util.List;

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
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * This class implements checkouts of a specific revision. A check
 * is made that this can be done without data loss.
 */
public class BranchOperation implements IEGitOperation {

	private final Repository repository;

	private final String refName;

	private final ObjectId commitId;

	/**
	 * Construct a {@link BranchOperation} object for a {@link Ref}.
	 * @param repository
	 * @param refName Name of git ref to checkout
	 */
	public BranchOperation(Repository repository, String refName) {
		this.repository = repository;
		this.refName = refName;
		this.commitId = null;
	}

	/**
	 * Construct a {@link BranchOperation} object for a commit.
	 * @param repository
	 * @param commit
	 */
	public BranchOperation(Repository repository, ObjectId commit) {
		this.repository = repository;
		this.refName = null;
		this.commitId = commit;
	}

	private RevTree oldTree;

	private RevTree newTree;

	private RevCommit oldCommit;

	private RevCommit newCommit;



	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		if (refName !=null && !refName.startsWith(Constants.R_REFS))
			throw new TeamException(NLS.bind(
					CoreText.BranchOperation_CheckoutOnlyBranchOrTag, refName));

		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			public void run(IProgressMonitor pm) throws CoreException {
				IProject[] validProjects = ProjectUtil.getValidProjects(repository);
				pm.beginTask(NLS.bind(
						CoreText.BranchOperation_performingBranch, refName), 5);
				lookupRefs();
				pm.worked(1);

				mapObjects();
				pm.worked(1);

				checkoutTree();
				pm.worked(1);

				updateHeadRef();
				pm.worked(1);

				ProjectUtil.refreshValidProjects(validProjects, new SubProgressMonitor(
						pm, 1));
				pm.worked(1);

				pm.done();
			}
		};
		// lock workspace to protect working tree changes
		ResourcesPlugin.getWorkspace().run(action, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	private void updateHeadRef() throws TeamException {
		boolean detach = false;
		// in case of a non-local branch or a tag,
		// we "detach" HEAD, i.e. point it to the
		// underlying commit instead of to the Ref
		if (refName == null || !refName.startsWith(Constants.R_HEADS))
			detach = true;
		try {
			RefUpdate u = repository.updateRef(Constants.HEAD, detach);
			Result res;
			if (detach) {
				u.setNewObjectId(newCommit.getId());
				// using forceUpdate instead of update avoids
				// the merge tests which would otherwise make
				// this fail
				u.setRefLogMessage(NLS.bind(
						CoreText.BranchOperation_checkoutMovingTo, newCommit
								.getId().name()), false);
				res = u.forceUpdate();
			} else {
				u.setRefLogMessage(NLS.bind(
						CoreText.BranchOperation_checkoutMovingTo, refName),
						false);
				res = u.link(refName);
			}
			switch (res) {
			case NEW:
			case FORCED:
			case NO_CHANGE:
			case FAST_FORWARD:
				break;
			default:
				throw new IOException(u.getResult().name());
			}
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.BranchOperation_updatingHeadToRef, refName), e);
		}
	}

	private void checkoutTree() throws TeamException {
		try {
			DirCacheCheckout dirCacheCheckout = new DirCacheCheckout(
					repository, oldTree, repository.lockDirCache(), newTree);
			dirCacheCheckout.setFailOnConflict(true);
			boolean result = dirCacheCheckout.checkout();
			if (!result)
				retryDelete(dirCacheCheckout);
		} catch (CheckoutConflictException e) {
			TeamException teamException = new TeamException(e.getMessage());
			throw teamException;
		} catch (IOException e) {
			throw new TeamException(CoreText.BranchOperation_checkoutProblem, e);
		}
	}

	private void retryDelete(DirCacheCheckout dirCacheCheckout) throws IOException {
		List<String> files = dirCacheCheckout.getToBeDeleted();
		for(String path:files) {
			File file = new File(repository.getWorkTree(), path);
			deleteFile(file);
		}
	}

	/**
	 * Deletes a file. Deletion is retried 10 times to avoid
	 * failing deletion caused by concurrent read.
	 * @param file
	 * @throws IOException
	 */
	private void deleteFile(File file) throws IOException {
		boolean deleted = false;
		for(int i=0; i<10; i++) {
			deleted = file.delete();
			if (deleted)
				break;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		if (!deleted) {
			throw new IOException(CoreText.BranchOperation_couldNotDelete + file.getPath());
		}
	}

	private void mapObjects() {
		oldTree = oldCommit.getTree();
		newTree = newCommit.getTree();
	}

	private void lookupRefs() throws TeamException {
		RevWalk walk = new RevWalk(repository);
		try {
			if (refName != null) {
				newCommit = walk.parseCommit(repository.resolve(refName));
			}
			if (commitId != null) {
				newCommit = walk.parseCommit(commitId);
			}
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.BranchOperation_mappingCommit, refName), e);
		}

		try {
			oldCommit = walk.parseCommit(repository.resolve(Constants.HEAD));
		} catch (IOException e) {
			throw new TeamException(CoreText.BranchOperation_mappingCommitHead,
					e);
		}
	}

}
