/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * Copyright (C) 2012, 2013 Tomasz Zarna <tzarna@gmail.com>
 * Copyright (C) 2014 Axel Richard <axel.richard@obeo.fr>
 * Copyright (C) 2015 Obeo
 * Copyright (C) 2015, Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Tomasz Zarna (IBM) - merge squash, bug 382720
 *    Axel Richard (Obeo) - merge message, bug 422886
 *    Laurent Delaigue (Obeo) - use of preferred merge strategy
 *    Stephan Hackstedt - bug 477695
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * This class implements the merge of a ref with the current head
 *
 */
public class MergeOperation implements IEGitOperation {

	private final Repository repository;

	private final String refName;

	private final MergeStrategy mergeStrategy;

	private Boolean squash;

	private FastForwardMode fastForwardMode;

	private Boolean commit;

	private MergeResult mergeResult;

	private String message;

	/**
	 * Create a MergeOperation object. Initializes the MergeStrategy with the
	 * preferred merge strategy, according to preferences.
	 *
	 * @param repository
	 * @param refName
	 *            name of a commit which should be merged
	 */
	public MergeOperation(@NonNull Repository repository,
			@NonNull String refName) {
		this.repository = repository;
		this.refName = refName;
		this.mergeStrategy = Activator.getDefault().getPreferredMergeStrategy();
	}

	/**
	 * Create a MergeOperation object
	 *
	 * @param repository
	 * @param refName
	 *            name of a commit which should be merged
	 * @param mergeStrategyName
	 *            the strategy to use for merge. If not registered, the default
	 *            merge strategy according to preferences will be used.
	 */
	public MergeOperation(@NonNull Repository repository,
			@NonNull String refName,
			@NonNull String mergeStrategyName) {
		this.repository = repository;
		this.refName = refName;
		MergeStrategy strategy = null;
		strategy = MergeStrategy.get(mergeStrategyName);
		this.mergeStrategy = strategy != null ? strategy : Activator.getDefault()
				.getPreferredMergeStrategy();
	}

	/**
	 * @param squash true to squash merge commits
	 */
	public void setSquash(boolean squash) {
		this.squash = Boolean.valueOf(squash);
	}

	/**
	 * @param ffmode set the fast forward mode
	 * @since 3.0
	 */
	public void setFastForwardMode(FastForwardMode ffmode) {
		this.fastForwardMode = ffmode;
	}

	/**
	 * @param commit
	 *            set the commit option
	 * @since 3.1
	 */
	public void setCommit(boolean commit) {
		this.commit = Boolean.valueOf(commit);
	}

	/**
	 * Set the commit message to be used for the merge commit (in case one is
	 * created)
	 *
	 * @param message
	 *            the message to be used for the merge commit
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		if (mergeResult != null)
			throw new CoreException(new Status(IStatus.ERROR, Activator
					.getPluginId(), CoreText.OperationAlreadyExecuted));
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor mymonitor) throws CoreException {
				IProject[] validProjects = ProjectUtil.getValidOpenProjects(repository);
				SubMonitor progress = SubMonitor.convert(mymonitor, NLS.bind(
						CoreText.MergeOperation_ProgressMerge, refName), 3);
				try (Git git = new Git(repository)) {
					progress.worked(1);
					MergeCommand merge = git.merge().setProgressMonitor(
							new EclipseGitProgressTransformer(
									progress.newChild(1)));
					Ref ref = repository.findRef(refName);
					if (ref != null) {
						merge.include(ref);
					} else {
						merge.include(ObjectId.fromString(refName));
					}
					if (fastForwardMode != null) {
						merge.setFastForward(fastForwardMode);
					}
					if (commit != null) {
						merge.setCommit(commit.booleanValue());
					}
					if (squash != null) {
						merge.setSquash(squash.booleanValue());
					}
					if (mergeStrategy != null) {
						merge.setStrategy(mergeStrategy);
					}
					if (message != null) {
						merge.setMessage(message);
					}
					if (repository.getConfig().getBoolean(
							ConfigConstants.CONFIG_GERRIT_SECTION,
							ConfigConstants.CONFIG_KEY_CREATECHANGEID, false)) {
						merge.setInsertChangeId(true);
					}
					mergeResult = merge.call();
					if (MergeResult.MergeStatus.NOT_SUPPORTED
							.equals(mergeResult.getMergeStatus())) {
						throw new TeamException(new Status(IStatus.INFO,
								Activator.getPluginId(),
								mergeResult.toString()));
					}
				} catch (IOException e) {
					throw new TeamException(
							CoreText.MergeOperation_InternalError, e);
				} catch (NoHeadException e) {
					throw new TeamException(
							CoreText.MergeOperation_MergeFailedNoHead, e);
				} catch (ConcurrentRefUpdateException e) {
					throw new TeamException(
							CoreText.MergeOperation_MergeFailedRefUpdate, e);
				} catch (CheckoutConflictException e) {
					mergeResult = new MergeResult(e.getConflictingPaths());
					return;
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} finally {
					ProjectUtil.refreshValidProjects(validProjects,
							progress.newChild(1));
				}
			}
		};
		// lock workspace to protect working tree changes
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, m);
	}

	/**
	 * @return the merge result, or <code>null</code> if this has not been
	 *         executed or if an exception occurred
	 */
	public @Nullable MergeResult getResult() {
		return this.mergeResult;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository);
	}
}
