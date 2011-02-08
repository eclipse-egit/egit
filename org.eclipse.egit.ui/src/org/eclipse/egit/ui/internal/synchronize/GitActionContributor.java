/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import static org.eclipse.egit.ui.internal.actions.ActionCommands.ADD_TO_INDEX;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.COMMIT_ACTION;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.IGNORE_ACTION;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.PUSH_ACTION;
import static org.eclipse.egit.ui.internal.synchronize.model.SupportedContextActionsHelper.canPush;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.SupportedContextActionsHelper;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

class GitActionContributor extends ModelSynchronizeParticipantActionGroup {

	private static final String GIT_ACTIONS = "gitActions"; //$NON-NLS-1$

	public void fillContextMenu(IMenuManager menu) {
		IStructuredSelection selection = (IStructuredSelection) getContext()
				.getSelection();
		if (selection.size() == 1) {
			Object element = selection.getFirstElement();

			if (element instanceof GitModelObject)
				createMenuForGitModelObject(menu, (GitModelObject) element);
			else {
				// add standard git action for 'workspace' models
				menu.appendToGroup(GIT_ACTIONS, createItem(COMMIT_ACTION));
				menu.appendToGroup(GIT_ACTIONS, createItem(ADD_TO_INDEX));
				menu.appendToGroup(GIT_ACTIONS, createItem(IGNORE_ACTION));
			}
		}
	}

	private void createMenuForGitModelObject(IMenuManager menu,
			GitModelObject object) {
		if (SupportedContextActionsHelper.canCommit(object))
			menu.appendToGroup(GIT_ACTIONS, createItem(COMMIT_ACTION));

		if (SupportedContextActionsHelper.canStage(object)) {
			menu.appendToGroup(GIT_ACTIONS, createItem(ADD_TO_INDEX));
			menu.appendToGroup(GIT_ACTIONS, createItem(IGNORE_ACTION));
		}

		if (canPush(object))
			menu.appendToGroup(GIT_ACTIONS, createItem(PUSH_ACTION));
	}

	private CommandContributionItem createItem(String itemAction) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		CommandContributionItemParameter itemParam = new CommandContributionItemParameter(
				workbench, null, itemAction, CommandContributionItem.STYLE_PUSH);

		IWorkbenchWindow activeWorkbenchWindow = workbench
				.getActiveWorkbenchWindow();
		IHandlerService hsr = (IHandlerService) activeWorkbenchWindow
				.getService(IHandlerService.class);
		IEvaluationContext ctx = hsr.getCurrentState();
		ctx.addVariable(ISources.ACTIVE_MENU_SELECTION_NAME, getContext()
				.getSelection());

		return new CommandContributionItem(itemParam);
	}

}
