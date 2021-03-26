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
package org.eclipse.egit.ui.internal.staging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.RevUtils;
import org.eclipse.egit.core.RevUtils.ConflictCommits;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.core.op.DiscardChangesOperation.Stage;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.CompoundContributionItem;

/**
 * Menu for replacing one/multiple files with version from ours/theirs on
 * conflict. Specialized version of ReplaceWithOursTheirsMenu taking into
 * account delete-modify and modify-delete conflicts.
 */
public class ReplaceConflictMenu extends CompoundContributionItem {

	private final Repository repo;

	private final Collection<StagingEntry> entries;

	/**
	 * Creates a new instance.
	 *
	 * @param repository
	 *            the entries are for
	 * @param entries
	 *            selected {@link StagingEntry} elements
	 */
	public ReplaceConflictMenu(Repository repository,
			Collection<StagingEntry> entries) {
		this.repo = repository;
		this.entries = entries;
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		if (entries == null || entries.isEmpty()) {
			return new IContributionItem[0];
		}
		List<IContributionItem> items = new ArrayList<>();
		if (entries.size() == 1) {
			items.addAll(createSpecificOursTheirsItems(repo, entries));
		} else {
			items.addAll(createUnspecificOursTheirsItems(repo, entries));
		}
		return items.toArray(new IContributionItem[0]);
	}

	private static Collection<IContributionItem> createSpecificOursTheirsItems(
			Repository repository, Collection<StagingEntry> entries) {

		StagingEntry single = entries.iterator().next();
		List<IContributionItem> result = new ArrayList<>();

		try {
			ConflictCommits conflictCommits = RevUtils.getConflictCommits(
					repository, single.getPath());
			RevCommit ourCommit = conflictCommits.getOurCommit();
			RevCommit theirCommit = conflictCommits.getTheirCommit();

			if (ourCommit != null)
				result.add(createOursItem(
						formatCommit(
								UIText.ReplaceWithOursTheirsMenu_OursWithCommitLabel,
								ourCommit),
						repository, entries));
			else
				result.add(createOursItem(
						UIText.ReplaceWithOursTheirsMenu_OursWithoutCommitLabel,
						repository, entries));

			if (theirCommit != null)
				result.add(createTheirsItem(
						formatCommit(
								UIText.ReplaceWithOursTheirsMenu_TheirsWithCommitLabel,
								theirCommit),
						repository, entries));
			else
				result.add(createTheirsItem(
						UIText.ReplaceWithOursTheirsMenu_TheirsWithoutCommitLabel,
						repository, entries));

			return result;
		} catch (IOException e) {
			Activator.logError(
					UIText.ReplaceWithOursTheirsMenu_CalculatingOursTheirsCommitsError, e);
			return createUnspecificOursTheirsItems(repository, entries);
		}
	}

	private static Collection<IContributionItem> createUnspecificOursTheirsItems(
			Repository repository, Collection<StagingEntry> entries) {
		List<IContributionItem> result = new ArrayList<>();
		result.add(createOursItem(
				UIText.ReplaceWithOursTheirsMenu_OursWithoutCommitLabel,
				repository, entries));
		result.add(createTheirsItem(
				UIText.ReplaceWithOursTheirsMenu_TheirsWithoutCommitLabel,
				repository, entries));
		return result;
	}

	private static IContributionItem createOursItem(String label,
			Repository repository, Collection<StagingEntry> entries) {
		return new ActionContributionItem(
				new ReplaceAction(label, Stage.OURS, repository, entries));
	}

	private static IContributionItem createTheirsItem(String label,
			Repository repository, Collection<StagingEntry> entries) {
		return new ActionContributionItem(
				new ReplaceAction(label, Stage.THEIRS, repository, entries));
	}

	private static String formatCommit(String format, RevCommit commit) {
		String message = Utils.shortenText(commit.getShortMessage(), 60);
		return NLS.bind(format, Utils.getShortObjectId(commit), message);
	}

	private static class ReplaceAction extends Action {

		private final Stage stage;

		private final Repository repository;

		private final Collection<StagingEntry> entries;

		public ReplaceAction(String text, Stage stage, Repository repository,
				Collection<StagingEntry> entries) {
			super(text);
			this.stage = stage;
			this.repository = repository;
			this.entries = entries;
		}

		@Override
		public void run() {
			List<String> toCheckout = new ArrayList<>();
			List<String> toRemove = new ArrayList<>();
			for (StagingEntry entry : entries) {
				StageState state = entry.getConflictType();
				if (StageState.DELETED_BY_THEM == state && stage == Stage.THEIRS
						|| StageState.DELETED_BY_US == state
								&& stage == Stage.OURS) {
					toRemove.add(entry.getPath());
				} else {
					toCheckout.add(entry.getPath());
				}
			}
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
										CheckoutCommand checkout = git
												.checkout().setProgressMonitor(
														new EclipseGitProgressTransformer(
																progress));

										checkout
												.setStage(stage == Stage.OURS
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
													e.getLocalizedMessage(),
													e));
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
	}
}
