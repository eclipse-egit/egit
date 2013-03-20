/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.op;

import java.io.IOException;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
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

	private final UpstreamConfig upstreamConfig;

	/**
	 * @param repository
	 * @param name
	 *            the name for the new local branch (without prefix)
	 * @param ref
	 *            the branch or tag to base the new branch upon
	 * @param config
	 *            how to do the upstream configuration
	 */
	public CreateLocalBranchOperation(Repository repository, String name,
			Ref ref, UpstreamConfig config) {
		this.name = name;
		this.repository = repository;
		this.ref = ref;
		this.commit = null;
		this.upstreamConfig = config;
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
		this.upstreamConfig = null;
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
					if (ref != null) {
						SetupUpstreamMode mode;
						if (upstreamConfig == UpstreamConfig.NONE)
							mode = SetupUpstreamMode.NOTRACK;
						else
							mode = SetupUpstreamMode.SET_UPSTREAM;
						git.branchCreate().setName(name).setStartPoint(
								ref.getName()).setUpstreamMode(mode).call();
					}
					else
						git.branchCreate().setName(name).setStartPoint(commit)
								.setUpstreamMode(SetupUpstreamMode.NOTRACK)
								.call();
				} catch (Exception e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				}

				if (UpstreamConfig.REBASE == upstreamConfig) {
					// set "branch.<name>.rebase" to "true"
					StoredConfig config = repository.getConfig();
					config.setBoolean(ConfigConstants.CONFIG_BRANCH_SECTION,
							name, ConfigConstants.CONFIG_KEY_REBASE, true);
					try {
						config.save();
					} catch (IOException e) {
						throw new CoreException(Activator.error(e.getMessage(),
								e));
					}
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

	/**
	 * Describes how to configure the upstream branch
	 */
	public static enum UpstreamConfig {
		/** Rebase */
		REBASE(),
		/** Merge */
		MERGE(),
		/** No configuration */
		NONE();
	}
}
