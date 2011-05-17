/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * This class implements deletion of a branch
 */
public class DeleteBranchOperation implements IEGitOperation {
	/** Operation was performed */
	public final static int OK = 0;

	/** Current branch can not be deleted */
	public final static int REJECTED_CURRENT = 1;

	/**
	 * Branch to be deleted has not been fully merged; use force to delete
	 * anyway
	 */
	public final static int REJECTED_UNMERGED = 2;

	/** This operation was not executed yet */
	public final static int NOT_TRIED = -1;

	private int status = NOT_TRIED;

	private final Repository repository;

	private final Set<Ref> branches;

	private final boolean force;

	/**
	 * @param repository
	 * @param branch
	 *            the branch to delete
	 * @param force
	 */
	public DeleteBranchOperation(Repository repository, Ref branch,
			boolean force) {
		this(repository, new HashSet<Ref>(asList(branch)), force);
	}

	/**
	 * @param repository
	 * @param branches
	 *            the list of branches to deleted
	 * @param force
	 */
	public DeleteBranchOperation(Repository repository, Set<Ref> branches,
			boolean force) {
		this.repository = repository;
		this.branches = branches;
		this.force = force;
	}

	/**
	 * @return one of {@link #OK}, {@link #REJECTED_CURRENT},
	 *         {@link #REJECTED_UNMERGED}, {@link #NOT_TRIED}
	 */
	public int getStatus() {
		return status;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor actMonitor) throws CoreException {

				String taskName;
				if (branches.size() == 1)
					taskName = NLS.bind(
							CoreText.DeleteBranchOperation_TaskName, branches
									.iterator().next().getName());
				else {
					StringBuilder names = new StringBuilder();
					for (Iterator<Ref> it = branches.iterator(); it.hasNext(); ) {
						Ref ref = it.next();
						names.append(ref.getName());
						if (it.hasNext())
							names.append(", "); //$NON-NLS-1$
					}
					taskName = NLS.bind(
							CoreText.DeleteBranchOperation_TaskName, names);
				}
				actMonitor.beginTask(taskName, branches.size());
				for (Ref branch : branches) {
					try {
						new Git(repository).branchDelete().setBranchNames(
								branch.getName()).setForce(force).call();
						status = OK;
					} catch (NotMergedException e) {
						status = REJECTED_UNMERGED;
						break;
					} catch (CannotDeleteCurrentBranchException e) {
						status = REJECTED_CURRENT;
						break;
					} catch (JGitInternalException e) {
						throw new CoreException(Activator.error(e.getMessage(), e));
					}
					actMonitor.worked(1);
				}
				actMonitor.done();
			}
		};
		// lock workspace to protect working tree changes
		ResourcesPlugin.getWorkspace().run(action, monitor);
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
}
