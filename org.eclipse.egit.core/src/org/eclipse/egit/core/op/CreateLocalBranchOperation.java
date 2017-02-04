/*******************************************************************************
 * Copyright (C) 2010, 2016 Mathias Kinzler <mathias.kinzler@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
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

	private final BranchRebaseMode upstreamConfig;

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
			Ref ref, BranchRebaseMode config) {
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

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor actMonitor) throws CoreException {
				String taskName = NLS.bind(
						CoreText.CreateLocalBranchOperation_CreatingBranchMessage,
						name);
				SubMonitor progress = SubMonitor.convert(actMonitor);
				progress.setTaskName(taskName);
				try (Git git = new Git(repository)) {
					if (ref != null) {
						SetupUpstreamMode mode;
						if (upstreamConfig == null)
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

				if (upstreamConfig != null) {
					// set "branch.<name>.rebase"
					StoredConfig config = repository.getConfig();
					config.setEnum(ConfigConstants.CONFIG_BRANCH_SECTION, name,
							ConfigConstants.CONFIG_KEY_REBASE, upstreamConfig);
					try {
						config.save();
					} catch (IOException e) {
						throw new CoreException(Activator.error(e.getMessage(),
								e));
					}
				}
			}
		};
		// lock workspace to protect working tree changes
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, monitor);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository);
	}

	/**
	 * Get the default upstream config for the specified repository and upstream
	 * branch ref.
	 *
	 * @param repo
	 * @param upstreamRefName
	 * @return the default {@link BranchRebaseMode}, or {@code null} if none is
	 *         configured
	 */
	public static BranchRebaseMode getDefaultUpstreamConfig(Repository repo,
			String upstreamRefName) {
		boolean isLocalBranch = upstreamRefName.startsWith(Constants.R_HEADS);
		boolean isRemoteBranch = upstreamRefName
				.startsWith(Constants.R_REMOTES);
		if (!isLocalBranch && !isRemoteBranch) {
			return null;
		}
		String autosetupMerge = repo.getConfig().getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, null,
				ConfigConstants.CONFIG_KEY_AUTOSETUPMERGE);
		if (autosetupMerge == null) {
			autosetupMerge = ConfigConstants.CONFIG_KEY_TRUE;
		}
		boolean setupMerge = autosetupMerge
				.equals(ConfigConstants.CONFIG_KEY_ALWAYS)
				|| (isRemoteBranch && autosetupMerge
						.equals(ConfigConstants.CONFIG_KEY_TRUE));
		if (!setupMerge) {
			return null;
		}
		String autosetupRebase = repo.getConfig().getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, null,
				ConfigConstants.CONFIG_KEY_AUTOSETUPREBASE);
		if (autosetupRebase == null) {
			autosetupRebase = ConfigConstants.CONFIG_KEY_NEVER;
		}
		boolean setupRebase = autosetupRebase
				.equals(ConfigConstants.CONFIG_KEY_ALWAYS)
				|| (autosetupRebase.equals(ConfigConstants.CONFIG_KEY_LOCAL)
						&& isLocalBranch)
				|| (autosetupRebase.equals(ConfigConstants.CONFIG_KEY_REMOTE)
						&& isRemoteBranch);
		if (setupRebase) {
			// Like cgit: plain rebase
			return BranchRebaseMode.REBASE;
		}
		return BranchRebaseMode.NONE;
	}

}
