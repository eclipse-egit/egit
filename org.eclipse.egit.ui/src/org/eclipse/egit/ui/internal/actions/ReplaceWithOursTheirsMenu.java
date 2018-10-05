/*******************************************************************************
 * Copyright (C) 2014 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RevUtils;
import org.eclipse.egit.core.RevUtils.ConflictCommits;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.core.op.DiscardChangesOperation.Stage;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Menu for replacing one/multiple files with version from ours/theirs on
 * conflict.
 */
public class ReplaceWithOursTheirsMenu extends CompoundContributionItem
		implements IWorkbenchContribution {

	private IServiceLocator serviceLocator;

	@Override
	public void initialize(IServiceLocator locator) {
		serviceLocator = locator;
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> items = new ArrayList<>();

		IHandlerService handlerService = CommonUtils.getService(serviceLocator,
				IHandlerService.class);
		IStructuredSelection selection = SelectionUtils
				.getSelection(handlerService.getCurrentState());
		IPath[] locations = SelectionUtils.getSelectedLocations(selection);
		if (locations.length == 0)
			return new IContributionItem[0];

		Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
				.splitPathsByRepository(Arrays.asList(locations));
		if (pathsByRepository.size() == 1) {

			Entry<Repository, Collection<String>> entry = pathsByRepository
					.entrySet().iterator().next();
			Repository repository = entry.getKey();
			Collection<String> paths = entry.getValue();
			if (paths.size() == 1) {
				String path = paths.iterator().next();
				items.addAll(createSpecificOursTheirsItems(repository, path));
			} else if (paths.size() > 1) {
				items.addAll(createUnspecificOursTheirsItems(Arrays
						.asList(locations)));
			}
		}

		return items.toArray(new IContributionItem[0]);
	}

	private static Collection<IContributionItem> createSpecificOursTheirsItems(
			Repository repository, String path) {

		Collection<IPath> paths = Collections.<IPath> singleton(new Path(
				new File(repository.getWorkTree(), path).getAbsolutePath()));
		List<IContributionItem> result = new ArrayList<>();

		try {
			ConflictCommits conflictCommits = RevUtils.getConflictCommits(
					repository, path);
			RevCommit ourCommit = conflictCommits.getOurCommit();
			RevCommit theirCommit = conflictCommits.getTheirCommit();

			if (ourCommit != null)
				result.add(createOursItem(
						formatCommit(
								UIText.ReplaceWithOursTheirsMenu_OursWithCommitLabel,
								ourCommit), paths));
			else
				result.add(createOursItem(UIText.ReplaceWithOursTheirsMenu_OursWithoutCommitLabel, paths));

			if (theirCommit != null)
				result.add(createTheirsItem(
						formatCommit(
								UIText.ReplaceWithOursTheirsMenu_TheirsWithCommitLabel,
								theirCommit), paths));
			else
				result.add(createTheirsItem(
						UIText.ReplaceWithOursTheirsMenu_TheirsWithoutCommitLabel,
						paths));

			return result;
		} catch (IOException e) {
			Activator.logError(
					UIText.ReplaceWithOursTheirsMenu_CalculatingOursTheirsCommitsError, e);
			return createUnspecificOursTheirsItems(paths);
		}
	}

	private static Collection<IContributionItem> createUnspecificOursTheirsItems(
			Collection<IPath> paths) {
		List<IContributionItem> result = new ArrayList<>();
		result.add(createOursItem(
				UIText.ReplaceWithOursTheirsMenu_OursWithoutCommitLabel, paths));
		result.add(createTheirsItem(
				UIText.ReplaceWithOursTheirsMenu_TheirsWithoutCommitLabel,
				paths));
		return result;
	}

	private static IContributionItem createOursItem(String label,
			final Collection<IPath> paths) {
		return new ActionContributionItem(new ReplaceAction(label, Stage.OURS,
				paths));
	}

	private static IContributionItem createTheirsItem(String label,
			final Collection<IPath> paths) {
		return new ActionContributionItem(new ReplaceAction(label,
				Stage.THEIRS, paths));
	}

	private static String formatCommit(String format, RevCommit commit) {
		String message = Utils.shortenText(commit.getShortMessage(), 60);
		return NLS.bind(format, commit.abbreviate(7).name(), message);
	}

	private static class ReplaceAction extends Action {

		private final Stage stage;
		private final Collection<IPath> paths;

		public ReplaceAction(String text, Stage stage, Collection<IPath> paths) {
			super(text);
			this.stage = stage;
			this.paths = paths;
		}

		@Override
		public void run() {
			final DiscardChangesOperation operation = new DiscardChangesOperation(
					paths);
			operation.setStage(stage);
			String jobname = UIText.DiscardChangesAction_discardChanges;
			Job job = new WorkspaceJob(jobname) {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					try {
						operation.execute(monitor);
					} catch (CoreException e) {
						return Activator.createErrorStatus(e.getStatus()
								.getMessage(), e);
					}
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					if (JobFamilies.DISCARD_CHANGES.equals(family))
						return true;
					return super.belongsTo(family);
				}
			};
			job.setUser(true);
			job.setRule(operation.getSchedulingRule());
			job.schedule();
		}
	}
}
