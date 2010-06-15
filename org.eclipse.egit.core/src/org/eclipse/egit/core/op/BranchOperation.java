/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
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
import org.eclipse.jgit.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.WorkDirCheckout;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * This class implements checkouts of a specific revision. A check
 * is made that this can be done without data loss.
 */
public class BranchOperation implements IEGitOperation {

	private final Repository repository;

	private final String refName;

	/**
	 * Construct a {@link BranchOperation} object.
	 * @param repository
	 * @param refName Name of git ref to checkout
	 */
	public BranchOperation(Repository repository, String refName) {
		this.repository = repository;
		this.refName = refName;
	}

	private Tree oldTree;

	private GitIndex index;

	private Tree newTree;

	private Commit oldCommit;

	private Commit newCommit;



	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		if (!refName.startsWith(Constants.R_REFS))
			throw new TeamException(NLS.bind(
					CoreText.BranchOperation_CheckoutOnlyBranchOrTag, refName));

		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			public void run(IProgressMonitor monitor) throws CoreException {
				IProject[] validProjects = ProjectUtil.getValidProjects(repository);
				monitor.beginTask(NLS.bind(
						CoreText.BranchOperation_performingBranch, refName), 6);
				lookupRefs();
				monitor.worked(1);

				mapObjects();
				monitor.worked(1);

				checkoutTree();
				monitor.worked(1);

				writeIndex();
				monitor.worked(1);

				updateHeadRef();
				monitor.worked(1);

				ProjectUtil.refreshValidProjects(validProjects, new SubProgressMonitor(
						monitor, 1));
				monitor.worked(1);

				monitor.done();
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
		if (!refName.startsWith(Constants.R_HEADS))
			detach = true;
		try {
			RefUpdate u = repository.updateRef(Constants.HEAD, detach);
			Result res;
			if (detach) {
				u.setNewObjectId(newCommit.getCommitId());
				// using forceUpdate instead of update avoids
				// the merge tests which would otherwise make
				// this fail
				u.setRefLogMessage(NLS.bind(
						CoreText.BranchOperation_checkoutMovingTo, newCommit
								.getCommitId().toString()), false);
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

	private void writeIndex() throws TeamException {
		try {
			index.write();
		} catch (IOException e) {
			throw new TeamException(CoreText.BranchOperation_writingIndex, e);
		}
	}

	private void checkoutTree() throws TeamException {
		try {
			new WorkDirCheckout(repository, repository.getWorkDir(), oldTree,
					index, newTree).checkout();
		} catch (CheckoutConflictException e) {
			TeamException teamException = new TeamException(e.getMessage());
			throw teamException;
		} catch (IOException e) {
			throw new TeamException(CoreText.BranchOperation_checkoutProblem, e);
		}
	}

	private void mapObjects() throws TeamException {
		try {
			oldTree = oldCommit.getTree();
			index = repository.getIndex();
			newTree = newCommit.getTree();
		} catch (IOException e) {
			throw new TeamException(CoreText.BranchOperation_mappingTrees, e);
		}
	}

	private void lookupRefs() throws TeamException {
		try {
			// if we have a tag, we have to make an indirection
			if (refName.startsWith(Constants.R_TAGS)) {
				Tag tag = repository.mapTag(refName);
				newCommit = repository.mapCommit(tag.getObjId());
			} else
				newCommit = repository.mapCommit(refName);
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.BranchOperation_mappingCommit, refName), e);
		}

		try {
			oldCommit = repository.mapCommit(Constants.HEAD);
		} catch (IOException e) {
			throw new TeamException(CoreText.BranchOperation_mappingCommitHead,
					e);
		}
	}

}
