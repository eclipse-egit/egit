/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.RevUtils;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.core.op.DiscardChangesOperation.Stage;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.IEvaluationService;

/**
 * Action handler base class for replacing conflicting files with a particular
 * index stage.
 */
public abstract class ReplaceConflictActionHandler
		extends RepositoryActionHandler implements IElementUpdater {

	private final DiscardChangesOperation.Stage stage;

	/**
	 * Creates new instance.
	 *
	 * @param stage
	 *            to check out
	 */
	protected ReplaceConflictActionHandler(
			DiscardChangesOperation.Stage stage) {
		this.stage = stage;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IPath[] locations = getSelectedLocations(event);
		if (locations == null || locations.length == 0) {
			return null;
		}
		Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
				.splitPathsByRepository(Arrays.asList(locations));
		if (pathsByRepository.size() != 1) {
			return null;
		}
		Entry<Repository, Collection<String>> entry = pathsByRepository
				.entrySet().iterator().next();
		Repository repository = entry.getKey();
		IndexDiffCacheEntry indexDiff = IndexDiffCache.getInstance()
				.getIndexDiffCacheEntry(repository);
		if (indexDiff == null) {
			return null;
		}
		IndexDiffData data = indexDiff.getIndexDiff();
		if (data == null) {
			return null;
		}
		Map<String, StageState> conflictStates = data.getConflictStates();
		List<String> toCheckout = new ArrayList<>();
		List<String> toRemove = new ArrayList<>();
		for (String path : entry.getValue()) {
			StageState state = conflictStates.get(path);
			if (StageState.DELETED_BY_THEM == state && stage == Stage.THEIRS
					|| StageState.DELETED_BY_US == state
							&& stage == Stage.OURS) {
				toRemove.add(path);
			} else {
				toCheckout.add(path);
			}
		}
		replaceWithStage(repository, stage, toCheckout, toRemove);
		return null;
	}

	/**
	 * Check out or remove files from the given repository using the given index
	 * stage.
	 *
	 * @param repository
	 *            to operate on
	 * @param stage
	 *            to check out
	 * @param toCheckout
	 *            repository-relative git paths of files to check out
	 * @param toRemove
	 *            repository-relative git paths of files to remove
	 */
	public static void replaceWithStage(Repository repository, Stage stage,
			Collection<String> toCheckout, Collection<String> toRemove) {
		if (toRemove.isEmpty()) {
			DiscardChangesOperation operation = new DiscardChangesOperation(
					repository, toCheckout);
			operation.setStage(stage);
			JobUtil.scheduleUserWorkspaceJob(operation,
					UIText.DiscardChangesAction_discardChanges,
					JobFamilies.DISCARD_CHANGES);
		} else {
			IEGitOperation operation = new IEGitOperation() {

				@Override
				public ISchedulingRule getSchedulingRule() {
					return RuleUtil.getRule(repository);
				}

				@Override
				public void execute(IProgressMonitor monitor)
						throws CoreException {
					IWorkspaceRunnable action = new IWorkspaceRunnable() {

						@Override
						public void run(IProgressMonitor progress)
								throws CoreException {
							ResourceUtil.saveLocalHistory(repository);
							try (Git git = new Git(repository)) {
								if (!toCheckout.isEmpty()) {
									CheckoutCommand checkout = git.checkout()
											.setProgressMonitor(
													new EclipseGitProgressTransformer(
															progress));

									checkout.setStage(stage == Stage.OURS
											? CheckoutCommand.Stage.OURS
											: CheckoutCommand.Stage.THEIRS);
									for (String path : toCheckout) {
										checkout.addPath(path);
									}
									checkout.call();
								}
								if (!toRemove.isEmpty()) {
									RmCommand rm = git.rm();
									for (String path : toRemove) {
										rm.addFilepattern(path);
									}
									rm.call();
								}
							} catch (GitAPIException e) {
								throw new CoreException(
										Activator.createErrorStatus(
												e.getLocalizedMessage(), e));
							}
						}
					};
					ResourcesPlugin.getWorkspace().run(action,
							getSchedulingRule(), IWorkspace.AVOID_UPDATE,
							monitor);
				}
			};
			JobUtil.scheduleUserWorkspaceJob(operation,
					UIText.DiscardChangesAction_discardChanges,
					JobFamilies.DISCARD_CHANGES);
		}
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		Repository repository = SelectionUtils.getRepository(
				PlatformUI.getWorkbench().getService(IEvaluationService.class)
						.getCurrentState());
		if (DiscardChangesOperation.Stage.OURS == stage) {
			RevCommit commit = SelectionRepositoryStateCache.INSTANCE
					.getHeadCommit(repository);
			if (commit != null) {
				element.setText(formatCommit(
						UIText.ReplaceWithOursTheirsMenu_OursWithCommitLabel,
						commit));
			}
		} else {
			try {
				RevCommit commit = RevUtils.getTheirs(repository);
				if (commit != null) {
					element.setText(formatCommit(
							UIText.ReplaceWithOursTheirsMenu_TheirsWithCommitLabel,
							commit));
				}
			} catch (IOException e) {
				Activator.logError(e.getLocalizedMessage(), e);
			}
		}
	}

	private static String formatCommit(String format, RevCommit commit) {
		String message = Utils.shortenText(commit.getShortMessage(), 60);
		return MessageFormat.format(format, Utils.getShortObjectId(commit),
				message);
	}
}
