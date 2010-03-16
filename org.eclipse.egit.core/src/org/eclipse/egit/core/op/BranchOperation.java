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

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.WorkDirCheckout;
import org.eclipse.team.core.TeamException;

/**
 * This class implements checkouts of a specific revision. A check
 * is made that this can be done without data loss.
 */
public class BranchOperation implements IWorkspaceRunnable {

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



	public void run(IProgressMonitor monitor) throws CoreException {
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

		ProjectUtil.refreshProjects(repository, monitor);
		monitor.worked(1);

		monitor.done();
	}

	private void updateHeadRef() throws TeamException {
		try {
			RefUpdate u = repository.updateRef(Constants.HEAD);
			u.setRefLogMessage("checkout: moving to " + refName, false);
			switch (u.link(refName)) {
			case NEW:
			case FORCED:
			case NO_CHANGE:
				break;
			default:
				throw new IOException(u.getResult().name());
			}
		} catch (IOException e) {
			throw new TeamException("Updating HEAD to ref: " + refName, e);
		}
	}

	private void writeIndex() throws TeamException {
		try {
			index.write();
		} catch (IOException e) {
			throw new TeamException("Writing index", e);
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
			throw new TeamException("Problem while checking out:", e);
		}
	}

	private void mapObjects() throws TeamException {
		try {
			oldTree = oldCommit.getTree();
			index = repository.getIndex();
			newTree = newCommit.getTree();
		} catch (IOException e) {
			throw new TeamException("Mapping trees", e);
		}
	}

	private void lookupRefs() throws TeamException {
		try {
			newCommit = repository.mapCommit(refName);
		} catch (IOException e) {
			throw new TeamException("Mapping commit: " + refName, e);
		}

		try {
			oldCommit = repository.mapCommit(Constants.HEAD);
		} catch (IOException e) {
			throw new TeamException("Mapping commit HEAD commit", e);
		}
	}

}
