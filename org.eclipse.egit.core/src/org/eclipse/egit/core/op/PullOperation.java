/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler <mathias.kinzler@sap.com> - initial implementation
 *    Laurent Delaigue (Obeo) - use of preferred merge strategy
 *    Stephan Hackstedt - bug 477695
 *    Mickael Istria (Red Hat Inc.) - [485124] Introduce PullReferenceConfig
 *    Karsten Thoms (itemis) - [540548] Parallelize pull jobs per repository
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;

/**
 * Wraps the JGit API {@link PullCommand} into an operation
 */
public class PullOperation implements IEGitOperation {

	/**
	 * This describe a specification of a Pull command
	 * @since 4.2
	 */
	public static class PullReferenceConfig {
		private String remote;

		private String reference;

		private BranchRebaseMode upstreamConfig;

		/**
		 * @param remote
		 * @param reference
		 * @param upstreamConfig
		 */
		public PullReferenceConfig(@Nullable String remote,
				@Nullable String reference,
				@Nullable BranchRebaseMode upstreamConfig) {
			this.remote = remote;
			this.reference = reference;
			this.upstreamConfig = upstreamConfig;
		}

		/**
		 * @return the remote to pull from. Can be null (in which case client is
		 *         free to either ignore the pull, send an error, use default
		 *         configuration for the current branch...)
		 */
		@Nullable
		public String getRemote() {
			return this.remote;
		}

		/**
		 * @return the reference (commit, tag, id...) to pull from. Can be null
		 *         (in which case client is free to either ignore the pull, send
		 *         an error, use default configuration for the current
		 *         branch...)
		 */
		@Nullable
		public String getReference() {
			return this.reference;
		}

		/**
		 * @return the upstream config strategy to use for the specified pull
		 */
		@Nullable
		public BranchRebaseMode getUpstreamConfig() {
			return this.upstreamConfig;
		}
	}

	private final Repository[] repositories;

	private Map<Repository, PullReferenceConfig> configs;

	private final Map<Repository, PullResult> results;

	private final int timeout;

	private CredentialsProvider credentialsProvider;

	/**
	 * @param repositories
	 *            the repositories
	 * @param timeout
	 *            in seconds
	 */
	public PullOperation(Set<Repository> repositories, int timeout) {
		this.timeout = timeout;
		this.repositories = repositories
				.toArray(new Repository[0]);
		this.configs = Collections.emptyMap();
		this.results = new LinkedHashMap<>();
	}

	/**
	 * @param repositories
	 *            Repositories to pull, with specific configuration
	 * @param timeout
	 *            in seconds
	 * @since 4.2
	 */
	public PullOperation(
			@NonNull Map<Repository, PullReferenceConfig> repositories,
			int timeout) {
		this(repositories.keySet(), timeout);
		this.configs = repositories;
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		if (!results.isEmpty()) {
			throw new CoreException(
					new Status(IStatus.ERROR, Activator.getPluginId(),
							CoreText.OperationAlreadyExecuted));
		}
		int workers = repositories.length;
		String taskName = MessageFormat.format(CoreText.PullOperation_TaskName,
				Integer.valueOf(workers));

		int maxThreads = getMaxPullThreadsCount();
		JobGroup jobGroup = new PullJobGroup(taskName, maxThreads, workers);

		SubMonitor progress = SubMonitor.convert(m, workers);
		for (Repository repository : repositories) {
			PullJob pullJob = new PullJob(repository, configs.get(repository));
			pullJob.setJobGroup(jobGroup);
			pullJob.schedule();
		}
		// No timeout for the group: each single job has a timeout
		long noTimeout = 0;
		try {
			jobGroup.join(noTimeout, progress);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CoreException(Activator.cancel(e.getMessage(), e));
		} catch (OperationCanceledException e) {
			throw new CoreException(Activator.cancel(e.getMessage(), e));
		}
	}

	private int getMaxPullThreadsCount() {
		String key = GitCorePreferences.core_maxPullThreadsCount;
		int defaultValue = 1;
		int value = Platform.getPreferencesService().getInt(
				Activator.getPluginId(), key,
				defaultValue, null);
		return Math.max(defaultValue, value);
	}

	/**
	 * JobGroup for multiple pulls.
	 */
	private static class PullJobGroup extends JobGroup {
		public PullJobGroup(String name, int maxThreads, int initialJobCount) {
			super(name, maxThreads, initialJobCount);
		}

		/**
		 * Always continue processing all other pulls
		 */
		@Override
		protected boolean shouldCancel(IStatus lastCompletedJobResult,
				int numberOfFailedJobs, int numberOfCancelledJobs) {
			return false;
		}
	}

	private final class PullJob extends Job {
		private final Repository repository;
		private final PullReferenceConfig config;

		PullJob(Repository repository, PullReferenceConfig config) {
			super(getPullTaskName(repository, config));
			this.repository = repository;
			this.config = config;
			setRule(RuleUtil.getRule(repository));
		}

		@Override
		public IStatus run(IProgressMonitor mymonitor) {
			PullResult pullResult = null;
			try (Git git = new Git(repository)) {
				PullCommand pull = git.pull();
				SubMonitor monitor = SubMonitor.convert(mymonitor, 4);
				pull.setProgressMonitor(new EclipseGitProgressTransformer(
						monitor.newChild(3)));
				pull.setTimeout(timeout);
				pull.setCredentialsProvider(credentialsProvider);
				if (config != null) {
					if (config.getRemote() != null) {
						pull.setRemote(config.getRemote());
					}
					if (config.getReference() != null) {
						pull.setRemoteBranchName(config.getReference());
					}
					pull.setRebase(config.getUpstreamConfig());
				}
				MergeStrategy strategy = Activator.getDefault()
						.getPreferredMergeStrategy();
				if (strategy != null) {
					pull.setStrategy(strategy);
				}
				pullResult = pull.call();
				synchronized (results) {
					results.put(repository, pullResult);
				}
				IProject[] projects = ProjectUtil
						.getValidOpenProjects(repository);
				if (refreshNeeded(pullResult)) {
					ProjectUtil.refreshValidProjects(projects,
							monitor.newChild(1));
				} else {
					monitor.worked(1);
				}
				return Status.OK_STATUS;
			} catch (DetachedHeadException e) {
				return Activator.error(
						CoreText.PullOperation_DetachedHeadMessage, e);
			} catch (InvalidConfigurationException e) {
				return Activator
						.error(CoreText.PullOperation_PullNotConfiguredMessage,
								e);
			} catch (GitAPIException | CoreException e) {
				return Activator.error(e.getMessage(), e);
			} catch (JGitInternalException e) {
				Throwable cause = e.getCause();
				if (cause == null || !(cause instanceof TransportException)) {
					cause = e;
				}
				return Activator.error(cause.getMessage(), cause);
			} finally {
				mymonitor.done();
			}
		}

		@Override
		public boolean belongsTo(Object family) {
			return JobFamilies.PULL.equals(family);
		}
	}

	static String getPullTaskName(Repository repo,
			PullReferenceConfig rc) {

		StoredConfig config = repo.getConfig();
		if (rc != null) {
			String remoteUri = config.getString(
					ConfigConstants.CONFIG_REMOTE_SECTION, rc.remote,
					ConfigConstants.CONFIG_KEY_URL);
			return "Pulling " + rc.remote + " from " + remoteUri; //$NON-NLS-1$ //$NON-NLS-2$
		}

		String branchName;
		try {
			String fullBranch = repo.getFullBranch();
			branchName = fullBranch != null
					? fullBranch.substring(Constants.R_HEADS.length())
					: ""; //$NON-NLS-1$
		} catch (IOException e) {
			return "Pulling from " + repo.toString(); //$NON-NLS-1$
		}

		// get the configured remote for the currently checked out branch
		// stored in configuration key branch.<branch name>.remote
		String remote = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION,
				branchName, ConfigConstants.CONFIG_KEY_REMOTE);
		if (remote == null) {
			// fall back to default remote
			remote = Constants.DEFAULT_REMOTE_NAME;
		}

		String remoteUri = config.getString(
				ConfigConstants.CONFIG_REMOTE_SECTION, remote,
				ConfigConstants.CONFIG_KEY_URL);
		if (remoteUri != null) {
			return "Pulling " + remote + " from " + remoteUri; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return "Pulling from " + repo.getDirectory(); //$NON-NLS-1$
	}

	boolean refreshNeeded(PullResult pullResult) {
		if (pullResult == null) {
			return true;
		}
		RebaseResult rebaseResult = pullResult.getRebaseResult();
		if (rebaseResult != null
				&& rebaseResult.getStatus() == RebaseResult.Status.UP_TO_DATE) {
			return false;
		}
		MergeResult mergeResult = pullResult.getMergeResult();
		if (mergeResult != null && mergeResult
				.getMergeStatus() == MergeStatus.ALREADY_UP_TO_DATE) {
			return false;
		}
		return true;
	}

	/**
	 * @return the results, or an empty Map if this has not been executed
	 */
	public Map<Repository, PullResult> getResults() {
		return this.results;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		// main job does not block anyone, but the sub tasks are blocking and
		// have own rules
		return null;
	}

	/**
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * @return the operation's credentials provider
	 */
	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}
}
