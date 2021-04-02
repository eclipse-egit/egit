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

import org.eclipse.egit.core.RevUtils;
import org.eclipse.egit.core.op.DiscardChangesOperation.Stage;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.ReplaceConflictActionHandler;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
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
		// Ours is HEAD
		RevCommit ours = SelectionRepositoryStateCache.INSTANCE
				.getHeadCommit(repo);
		if (ours != null) {
			items.add(createOursItem(ReplaceConflictActionHandler.formatCommitLabel(
					UIText.ReplaceWithOursTheirsMenu_OursWithCommitLabel, ours),
					repo, entries));
		} else {
			items.add(createOursItem(
					UIText.ReplaceWithOursTheirsMenu_OursWithoutCommitLabel,
					repo, entries));
		}
		RevCommit theirs = null;
		try {
			theirs = RevUtils.getTheirs(repo);
		} catch (IOException e) {
			Activator.logError(e.getLocalizedMessage(), e);
		}
		if (theirs != null) {
			items.add(
					createTheirsItem(ReplaceConflictActionHandler.formatCommitLabel(
							UIText.ReplaceWithOursTheirsMenu_TheirsWithCommitLabel,
							theirs), repo, entries));
		} else {
			items.add(createTheirsItem(
					UIText.ReplaceWithOursTheirsMenu_TheirsWithoutCommitLabel,
					repo, entries));
		}
		return items.toArray(new IContributionItem[0]);
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
			ReplaceConflictActionHandler.replaceWithStage(repository, stage,
					toCheckout, toRemove);
		}
	}
}
