/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
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
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * This class implements checkouts of a specific revision. A check is made that
 * this can be done without data loss.
 */
public class BranchOperation implements IEGitOperation {

	private final Repository repository;

	private final String refName;

	private final ObjectId commitId;

	private CheckoutResult result;

	/**
	 * Construct a {@link BranchOperation} object for a {@link Ref}.
	 *
	 * @param repository
	 * @param refName
	 *            Name of git ref to checkout
	 */
	public BranchOperation(Repository repository, String refName) {
		this.repository = repository;
		this.refName = refName;
		this.commitId = null;
	}

	/**
	 * Construct a {@link BranchOperation} object for a commit.
	 *
	 * @param repository
	 * @param commit
	 */
	public BranchOperation(Repository repository, ObjectId commit) {
		this.repository = repository;
		this.refName = null;
		this.commitId = commit;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		if (refName != null && !refName.startsWith(Constants.R_REFS))
			throw new TeamException(NLS.bind(
					CoreText.BranchOperation_CheckoutOnlyBranchOrTag, refName));

		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			public void run(IProgressMonitor pm) throws CoreException {
				IProject[] validProjects = ProjectUtil
						.getValidProjects(repository);
				pm.beginTask(NLS.bind(
						CoreText.BranchOperation_performingBranch, refName), 1);

				CheckoutCommand co = new Git(repository).checkout();
				co.setName(getTarget());

				try {
					co.call();
				} catch (JGitInternalException e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				} catch (GitAPIException e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				} finally {
					BranchOperation.this.result = co.getResult();
				}
				if (result.getStatus() == Status.NONDELETED)
					retryDelete(result.getUndeletedList());
				pm.worked(1);
				ProjectUtil.refreshValidProjects(validProjects,
						new SubProgressMonitor(pm, 1));
				pm.worked(1);

				pm.done();
			}
		};
		// lock workspace to protect working tree changes
		ResourcesPlugin.getWorkspace().run(action, monitor);
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * @return the result of the operation
	 */
	public CheckoutResult getResult() {
		return result;
	}

	private String getTarget() {
		if (refName != null)
			return refName;
		return commitId.name();
	}

	void retryDelete(List<String> pathList) {
		// try to delete, but for a short time only
		long startTime = System.currentTimeMillis();
		for (String path : pathList) {
			if (System.currentTimeMillis() - startTime > 1000)
				break;
			File fileToDelete = new File(repository.getWorkTree(), path);
			if (fileToDelete.exists())
				try {
					FileUtils.delete(fileToDelete, FileUtils.RETRY
							| FileUtils.RECURSIVE);
				} catch (IOException e) {
					// ignore here
				}
		}
	}
}
