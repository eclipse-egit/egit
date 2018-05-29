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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.egit.ui.internal.stash.StashCreateUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * The "Stashes" submenu, for stashing changes and listing the existing stashes.
 */
public class StashesMenu extends CompoundContributionItem implements
		IWorkbenchContribution {

	private IServiceLocator serviceLocator;

	@Override
	public void initialize(IServiceLocator locator) {
		this.serviceLocator = locator;
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		Repository repository = getRepository();

		List<IContributionItem> items = new ArrayList<>();

		items.add(createStashChangesItem(repository));
		items.add(new Separator());
		items.addAll(createStashItems(repository));

		return items.toArray(new IContributionItem[0]);
	}

	private static IContributionItem createStashChangesItem(
			final Repository repository) {
		Action action = new Action(UIText.StashesMenu_StashChangesActionText) {
			@Override
			public void run() {
				StashCreateUI stashCreateUI = new StashCreateUI(repository);
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				stashCreateUI.createStash(shell);
			}

			@Override
			public boolean isEnabled() {
				return StashCreateHandler.isEnabled(repository);
			}
		};
		return new ActionContributionItem(action);
	}

	private static Collection<IContributionItem> createStashItems(
			Repository repository) {
		if (repository == null)
			return Collections.singleton(createNoStashedChangesItem());

		try {
			Collection<RevCommit> stashCommits = Git.wrap(repository)
					.stashList().call();

			if (stashCommits.isEmpty())
				return Collections.singleton(createNoStashedChangesItem());

			List<IContributionItem> items = new ArrayList<>(
					stashCommits.size());

			int index = 0;
			for (final RevCommit stashCommit : stashCommits)
				items.add(createStashItem(repository, stashCommit, index++));

			return items;
		} catch (GitAPIException e) {
			String repoName = repository.getWorkTree().getName();
			String message = MessageFormat.format(
					UIText.StashesMenu_StashListError, repoName);
			Activator.logError(message, e);
			return Collections.singleton(createNoStashedChangesItem());
		}
	}

	private static IContributionItem createNoStashedChangesItem() {
		Action action = new Action(UIText.StashesMenu_NoStashedChangesText) {
			@Override
			public boolean isEnabled() {
				return false;
			}
		};
		return new ActionContributionItem(action);
	}

	private Repository getRepository() {
		if (serviceLocator == null)
			return null;

		IHandlerService handlerService = CommonUtils.getService(serviceLocator, IHandlerService.class);
		if (handlerService == null)
			return null;

		IEvaluationContext evaluationContext = handlerService.getCurrentState();
		return SelectionUtils.getRepository(evaluationContext);
	}

	private static ActionContributionItem createStashItem(
			final Repository repo, final RevCommit stashCommit, int index) {
		String text = MessageFormat.format(UIText.StashesMenu_StashItemText,
				Integer.valueOf(index), stashCommit.getShortMessage());
		Action action = new Action(text) {
			@Override
			public void run() {
				RepositoryCommit repositoryCommit = new RepositoryCommit(repo,
						stashCommit);
				repositoryCommit.setStash(true);
				CommitEditor.openQuiet(repositoryCommit);
			}
		};
		return new ActionContributionItem(action);
	}

}
