/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.Activator.error;
import static org.eclipse.jgit.api.MergeCommand.FastForwardMode.NO_FF;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.core.op.DeleteBranchOperation;
import org.eclipse.egit.core.op.FetchOperation;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;

/**
 * Common logic for Git Flow operations.
 */
// TODO: This class should be called AbstractGitFlowOperation for consistency
abstract public class GitFlowOperation implements IEGitOperation {
	/**
	 * git path separator
	 */
	public static final String SEP = "/"; //$NON-NLS-1$

	/**
	 * repository that is operated on.
	 */
	protected GitFlowRepository repository;

	/**
	 * the status of the latest merge from this operation
	 */
	// TODO: Remove from this class. Not all GitFlow operations involve a merge
	protected MergeResult mergeResult;

	/**
	 * @param repository
	 */
	public GitFlowOperation(GitFlowRepository repository) {
		this.repository = repository;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository.getRepository());
	}

	/**
	 * @param branchName
	 * @param sourceCommit
	 * @return operation constructed from parameters
	 */
	protected CreateLocalBranchOperation createBranchFromHead(
			String branchName, RevCommit sourceCommit) {
		return new CreateLocalBranchOperation(repository.getRepository(),
				branchName, sourceCommit);
	}

	/**
	 * git flow * start
	 *
	 * @param monitor
	 * @param branchName
	 * @param sourceCommit
	 * @throws CoreException
	 */
	protected void start(IProgressMonitor monitor, String branchName,
			RevCommit sourceCommit) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		CreateLocalBranchOperation branchOperation = createBranchFromHead(
				branchName, sourceCommit);
		branchOperation.execute(progress.newChild(1));
		BranchOperation checkoutOperation = new BranchOperation(
				repository.getRepository(), branchName);
		checkoutOperation.execute(progress.newChild(1));
	}

	/**
	 * git flow * finish
	 *
	 * @param monitor
	 * @param branchName
	 * @param squash
	 * @param fastForwardSingleCommit Has no effect if {@code squash} is true.
	 * @param keepBranch
	 * @throws CoreException
	 * @since 4.1
	 */
	protected void finish(IProgressMonitor monitor, String branchName,
			boolean squash, boolean keepBranch, boolean fastForwardSingleCommit)
			throws CoreException {
		try {
			SubMonitor progress = SubMonitor.convert(monitor, 2);
			mergeResult = mergeTo(progress.newChild(1), branchName,
					repository.getConfig()
					.getDevelop(), squash, fastForwardSingleCommit);
			if (!mergeResult.getMergeStatus().isSuccessful()) {
				return;
			}

			Ref branch = repository.findBranch(branchName);
			if (branch == null) {
				throw new IllegalStateException(NLS.bind(
						CoreText.GitFlowOperation_branchMissing, branchName));
			}
			boolean forceDelete = squash;

			if (!keepBranch) {
				new DeleteBranchOperation(repository.getRepository(), branch,
						forceDelete).execute(progress.newChild(1));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Finish without squash, NO_FF and keep for single commit branches:
	 * {@link org.eclipse.egit.gitflow.op.GitFlowOperation#finish(IProgressMonitor, String, boolean, boolean, boolean)}
	 *
	 * @param monitor
	 * @param branchName
	 * @throws CoreException
	 */
	protected void finish(IProgressMonitor monitor, String branchName)
			throws CoreException {
		finish(monitor, branchName, false, false, false);
	}

	/**
	 * @param monitor
	 * @param branchName
	 * @param targetBranchName
	 * @param squash
	 * @param fastForwardSingleCommit Has no effect if {@code squash} is true.
	 * @return result of merging back to targetBranchName
	 * @throws CoreException
	 * @since 4.1
	 */
	protected @NonNull MergeResult mergeTo(IProgressMonitor monitor, String branchName,
			String targetBranchName, boolean squash, boolean fastForwardSingleCommit) throws CoreException {
		try {
			if (!repository.hasBranch(targetBranchName)) {
				throw new RuntimeException(NLS.bind(
						CoreText.GitFlowOperation_branchNotFound,
						targetBranchName));
			}
			SubMonitor progress = SubMonitor.convert(monitor, 2);
			boolean dontCloseProjects = false;
			Repository gitRepo = repository.getRepository();
			BranchOperation branchOperation = new BranchOperation(gitRepo,
					targetBranchName, dontCloseProjects);
			branchOperation.execute(progress.newChild(1));
			Status status = branchOperation.getResult(gitRepo).getStatus();
			if (!CheckoutResult.Status.OK.equals(status)) {
				throw new CoreException(error(NLS.bind(
						CoreText.GitFlowOperation_unableToCheckout, branchName,
						status.toString())));
			}
			MergeOperation mergeOperation = new MergeOperation(gitRepo,
					branchName);
			mergeOperation.setSquash(squash);
			if (squash) {
				mergeOperation.setCommit(true);
			}
			if (!squash && (!fastForwardSingleCommit || hasMultipleCommits(branchName))) {
				mergeOperation.setFastForwardMode(NO_FF);
			}
			mergeOperation.execute(progress.newChild(1));

			MergeResult result = mergeOperation.getResult();
			if (result == null) {
				throw new CoreException(error(NLS.bind(
						CoreText.GitFlowOperation_unableToMerge, branchName,
						targetBranchName)));
			}

			return result;
		} catch (GitAPIException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean hasMultipleCommits(String branchName) throws IOException {
		return getAheadOfDevelopCount(branchName) > 1;
	}

	private int getAheadOfDevelopCount(String branchName) throws IOException {
		String parentBranch = repository.getConfig().getDevelop();

		Ref develop = repository.findBranch(parentBranch);
		Ref branch = repository.findBranch(branchName);

		try (RevWalk walk = new RevWalk(repository.getRepository())) {
			RevCommit branchCommit = walk.parseCommit(branch.getObjectId());
			RevCommit developCommit = walk.parseCommit(develop.getObjectId());

			RevCommit mergeBase = findCommonBase(walk, branchCommit,
					developCommit);

			walk.reset();
			walk.setRevFilter(RevFilter.ALL);
			int aheadCount = RevWalkUtils.count(walk, branchCommit, mergeBase);

			return aheadCount;
		}
	}

	private RevCommit findCommonBase(RevWalk walk, RevCommit branchCommit,
			RevCommit developCommit) throws IOException {
		walk.setRevFilter(RevFilter.MERGE_BASE);
		walk.markStart(branchCommit);
		walk.markStart(developCommit);
		return walk.next();
	}

	/**
	 * Merge without squash and NO_FF for single commit branches:
	 * {@link org.eclipse.egit.gitflow.op.GitFlowOperation#mergeTo(IProgressMonitor, String, String, boolean, boolean)}
	 *
	 * @param monitor
	 * @param branchName
	 * @param targetBranchName
	 * @return result of merging back to targetBranchName
	 * @throws CoreException
	 */
	protected MergeResult mergeTo(IProgressMonitor monitor, String branchName,
			String targetBranchName) throws CoreException {
		return mergeTo(monitor, branchName, targetBranchName, false, false);
	}

	/**
	 * Fetch using the default remote configuration
	 *
	 * @param monitor
	 * @param timeout
	 *            timeout in seconds
	 * @return result of fetching from remote
	 * @throws URISyntaxException
	 * @throws InvocationTargetException
	 *
	 * @since 4.2
	 */
	protected FetchResult fetch(IProgressMonitor monitor, int timeout)
			throws URISyntaxException, InvocationTargetException {
		RemoteConfig config = repository.getConfig().getDefaultRemoteConfig();
		FetchOperation fetchOperation = new FetchOperation(
				repository.getRepository(), config, timeout, false);
		fetchOperation.run(monitor);
		return fetchOperation.getOperationResult();
	}

	/**
	 * @return The result of the merge this operation performs. May be null, if
	 *         no merge was performed.
	 */
	public MergeResult getMergeResult() {
		return mergeResult;
	}
}
