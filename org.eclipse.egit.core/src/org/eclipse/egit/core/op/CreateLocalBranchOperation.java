/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;

/**
 * This class implements creation of a local branch based on a commit or another
 * branch
 */
public class CreateLocalBranchOperation implements IEGitOperation {
	private final String name;

	private final Repository repository;

	private final Ref ref;

	private final RevCommit commit;

	/**
	 * @param repository
	 * @param name
	 *            the name for the new local branch (without prefix)
	 * @param ref
	 *            the branch or tag to base the new branch upon
	 */
	public CreateLocalBranchOperation(Repository repository, String name,
			Ref ref) {
		this.name = name;
		this.repository = repository;
		this.ref = ref;
		this.commit = null;
	}

	/**
	 * @param repository
	 * @param name
	 *            the name for the new local branch (without prefix)
	 * @param commit
	 *            a commit to base the new branch upon
	 */
	public CreateLocalBranchOperation(Repository repository, String name,
			RevCommit commit) {
		this.name = name;
		this.repository = repository;
		this.ref = null;
		this.commit = commit;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor actMonitor) throws CoreException {
				String taskName = NLS
						.bind(
								CoreText.CreateLocalBranchOperation_CreatingBranchMessage,
								name);
				actMonitor.beginTask(taskName, 1);
				Git git = new Git(repository);
				try {
					if (ref != null)
						git.branchCreate().setName(name).setStartPoint(ref.getName())
								.setUpstreamMode(SetupUpstreamMode.TRACK)
								.call();
					else
						git.branchCreate().setName(name).setStartPoint(commit)
								.setUpstreamMode(SetupUpstreamMode.NOTRACK)
								.call();
				} catch (Exception e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				}
				actMonitor.worked(1);
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
