/*******************************************************************************
 * Copyright (C) 2011, 2015 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 355809
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import static org.eclipse.egit.ui.internal.UIIcons.EXPAND_ALL;
import static org.eclipse.egit.ui.internal.UIIcons.PULL;
import static org.eclipse.egit.ui.internal.UIIcons.PUSH;
import static org.eclipse.egit.ui.internal.UIText.GitActionContributor_ExpandAll;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.ADD_TO_INDEX;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.COMMIT_ACTION;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.CREATE_PATCH;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.IGNORE_ACTION;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.MERGE_TOOL_ACTION;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.PUSH_ACTION;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.REMOVE_FROM_INDEX;
import static org.eclipse.egit.ui.internal.synchronize.model.SupportedContextActionsHelper.canPush;
import static org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration.P_OPEN_ACTION;
import static org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration.NAVIGATE_GROUP;
import static org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration.P_TOOLBAR_MENU;
import static org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration.SYNCHRONIZE_GROUP;
import static org.eclipse.ui.ISources.ACTIVE_MENU_SELECTION_NAME;
import static org.eclipse.ui.menus.CommandContributionItem.STYLE_PUSH;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.resources.IResourceState;
import org.eclipse.egit.ui.internal.resources.ResourceStateFactory;
import org.eclipse.egit.ui.internal.synchronize.action.ExpandAllModelAction;
import org.eclipse.egit.ui.internal.synchronize.action.GitOpenInCompareAction;
import org.eclipse.egit.ui.internal.synchronize.action.OpenWorkingFileAction;
import org.eclipse.egit.ui.internal.synchronize.action.PullAction;
import org.eclipse.egit.ui.internal.synchronize.action.PushAction;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.SupportedContextActionsHelper;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

class GitActionContributor extends SynchronizePageActionGroup {

	private static final String GIT_ACTIONS = "gitActions"; //$NON-NLS-1$

	private OpenWorkingFileAction openWorkingFileAction;

	@Override
	public void fillContextMenu(IMenuManager menu) {
		IStructuredSelection selection = (IStructuredSelection) getContext()
				.getSelection();
		if (selection.isEmpty())
			return;

		Object element = selection.getFirstElement();
		IResource resource = ResourceUtil.getResource(element);
		if (resource != null) {
			// add standard git action for 'workspace' models
			menu.appendToGroup(GIT_ACTIONS, createItem(COMMIT_ACTION));
			IResourceState state = ResourceStateFactory.getInstance()
					.get(resource);
			if (state.hasUnstagedChanges()) {
				menu.appendToGroup(GIT_ACTIONS, createItem(ADD_TO_INDEX));
			}
			if (state.isStaged()) {
				menu.appendToGroup(GIT_ACTIONS, createItem(REMOVE_FROM_INDEX));
			}
			if (!state.isIgnored()) {
				menu.appendToGroup(GIT_ACTIONS, createItem(IGNORE_ACTION));
			}
			menu.appendToGroup(GIT_ACTIONS, createItem(MERGE_TOOL_ACTION));
			menu.appendToGroup(GIT_ACTIONS, createItem(CREATE_PATCH));
		} else if (element instanceof GitModelObject && selection.size() == 1) {
			createMenuForGitModelObject(menu, (GitModelObject) element);
		}

		IContributionItem fileGroup = findGroup(menu,
				ISynchronizePageConfiguration.FILE_GROUP);

		if (fileGroup != null) {
			ModelSynchronizeParticipant msp = ((ModelSynchronizeParticipant) getConfiguration()
					.getParticipant());

			if (msp.hasCompareInputFor(element))
				menu.appendToGroup(fileGroup.getId(), openWorkingFileAction);
		}
	}

	private void createMenuForGitModelObject(IMenuManager menu,
			GitModelObject object) {
		if (SupportedContextActionsHelper.canCommit(object)) {
			menu.appendToGroup(GIT_ACTIONS, createItem(COMMIT_ACTION));
		}
		if (SupportedContextActionsHelper.canStage(object)) {
			// We know we have a model object for a working tree file here.
			IPath path = object.getLocation();
			if (path != null) {
				IResourceState state = ResourceStateFactory.getInstance()
						.get(path.toFile());
				if (state.hasUnstagedChanges()) {
					menu.appendToGroup(GIT_ACTIONS, createItem(ADD_TO_INDEX));
				}
				if (state.isStaged()) {
					menu.appendToGroup(GIT_ACTIONS,
							createItem(REMOVE_FROM_INDEX));
				}
				if (!state.isIgnored()) {
					menu.appendToGroup(GIT_ACTIONS, createItem(IGNORE_ACTION));
				}
			}
		}
		if (SupportedContextActionsHelper.canUseMergeTool(object)) {
			menu.appendToGroup(GIT_ACTIONS, createItem(MERGE_TOOL_ACTION));
		}
		if (canPush(object)) {
			menu.appendToGroup(GIT_ACTIONS, createItem(PUSH_ACTION));
		}
	}

	private CommandContributionItem createItem(String itemAction) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		CommandContributionItemParameter itemParam = new CommandContributionItemParameter(
				workbench, null, itemAction, STYLE_PUSH);

		IWorkbenchWindow activeWorkbenchWindow = workbench
				.getActiveWorkbenchWindow();
		IHandlerService hsr = activeWorkbenchWindow
				.getService(IHandlerService.class);
		IEvaluationContext ctx = hsr.getCurrentState();
		ctx.addVariable(ACTIVE_MENU_SELECTION_NAME, getContext().getSelection());

		return new CommandContributionItem(itemParam);
	}

	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);

		ExpandAllModelAction expandAllAction = new ExpandAllModelAction(
				GitActionContributor_ExpandAll, configuration);
		expandAllAction.setImageDescriptor(EXPAND_ALL);
		appendToGroup(P_TOOLBAR_MENU, NAVIGATE_GROUP, expandAllAction);

		PullAction pullAction = new PullAction(
				UIText.GitActionContributor_Pull, configuration);
		pullAction.setImageDescriptor(PULL);
		appendToGroup(P_TOOLBAR_MENU, SYNCHRONIZE_GROUP, pullAction);

		PushAction pushAction = new PushAction(
				UIText.GitActionContributor_Push, configuration);
		pushAction.setImageDescriptor(PUSH);
		appendToGroup(P_TOOLBAR_MENU, SYNCHRONIZE_GROUP, pushAction);

		ISynchronizePageSite site = configuration.getSite();
		IWorkbenchSite ws = site.getWorkbenchSite();
		openWorkingFileAction = new OpenWorkingFileAction(ws.getWorkbenchWindow()
			.getActivePage());

		site.getSelectionProvider().addSelectionChangedListener(
				openWorkingFileAction);

		if (ws instanceof IViewSite) {
			Object oldAction = configuration.getProperty(P_OPEN_ACTION);
			if (!(oldAction instanceof Action))
				return;

			final GitOpenInCompareAction openInCompareAction = new GitOpenInCompareAction(
					configuration, (Action) oldAction);
			configuration.setProperty(P_OPEN_ACTION, openInCompareAction);
		}
	}
}
