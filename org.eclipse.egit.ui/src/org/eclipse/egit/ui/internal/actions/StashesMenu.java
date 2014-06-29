/*******************************************************************************
 * Copyright (C) 2014 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * The "Stashes" submenu, for stashing changes and listing the existing stashes.
 */
public class StashesMenu extends CompoundContributionItem implements
		IWorkbenchContribution {

	private IServiceLocator serviceLocator;

	public void initialize(IServiceLocator locator) {
		this.serviceLocator = locator;
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> items = new ArrayList<IContributionItem>();

		items.add(createStashChangesItem());
		items.add(new Separator());
		items.addAll(createStashItems());

		return items.toArray(new IContributionItem[0]);
	}

	private CommandContributionItem createStashChangesItem() {
		return new CommandContributionItem(
				new CommandContributionItemParameter(serviceLocator, null,
						ActionCommands.STASH_CREATE,
						CommandContributionItem.STYLE_PUSH));
	}

	private IContributionItem createNoStashedChangesItem() {
		Action action = new Action(UIText.StashesMenu_NoStashedChangesText) {
			@Override
			public boolean isEnabled() {
				return false;
			}
		};
		return new ActionContributionItem(action);
	}

	private Collection<IContributionItem> createStashItems() {
		Repository repository = getRepository();
		if (repository == null)
			return Collections.singleton(createNoStashedChangesItem());

		try {
			Collection<RevCommit> stashCommits = Git.wrap(repository)
					.stashList().call();

			if (stashCommits.isEmpty())
				return Collections.singleton(createNoStashedChangesItem());

			List<IContributionItem> items = new ArrayList<IContributionItem>(
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

	/**
	 * @return repository or null
	 */
	private Repository getRepository() {
		if (serviceLocator == null)
			return null;

		ISelectionService selectionService = (ISelectionService) serviceLocator
				.getService(ISelectionService.class);

		if (selectionService == null)
			return null;

		ISelection s = selectionService.getSelection();
		if (!(s instanceof IStructuredSelection))
			return null;

		IStructuredSelection selection = (IStructuredSelection) s;
		if (selection.isEmpty())
			return null;

		return RepositoryActionHandler.getRepository(selection);
	}

	private ActionContributionItem createStashItem(final Repository repo,
			final RevCommit stashCommit, int index) {
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
