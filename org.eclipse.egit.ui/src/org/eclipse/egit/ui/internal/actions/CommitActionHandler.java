/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Jing Xue <jingxue@digizenstudio.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2011, Benjamin Muskalla <benjamin.muskalla@tasktop.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.commit.CommitUI;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.egit.ui.internal.utils.CommandUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Scan for modified resources in the same project as the selected resources.
 */
public class CommitActionHandler extends RepositoryActionHandler {

	private static final String EGIT_PUSH_COMMAND_ID = "org.eclipse.egit.ui.team.Push"; //$NON-NLS-1$

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Repository[] repos = getRepositoriesFor(getProjectsForSelectedResources(event));
		final Shell shell = getShell(event);
		IResource[] resourcesInScope;
		try {
			IResource[] selectedResources = getSelectedResources(event);
			IWorkbenchPart part = getPart(event);
			resourcesInScope = GitScopeUtil.getRelatedChanges(part,
					selectedResources);
		} catch (InterruptedException e) {
			// ignore, we will not show the commit dialog in case the user
			// cancels the scope operation
			return null;
		}
		CommitUI commitUi = new CommitUI(shell, repos[0], resourcesInScope,
				false);
		commitUi.commit();
		
		if (commitUi.isExecutePush())
			executePushCommand(HandlerUtil.getCurrentSelection(event));

		return null;
	}

	@Override
	public boolean isEnabled() {
		IProject[] projects = getProjectsForSelectedResources();
		return getRepositoriesFor(projects).length == 1;
	}

	private boolean executePushCommand(ISelection selection)
		throws ExecutionException {
		if (!(selection instanceof IStructuredSelection))
			throw new ExecutionException(NLS.bind(
				UIText.CommitActionHandler_NoSelection,
				EGIT_PUSH_COMMAND_ID));

		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		try {
			return CommandUtils.executeCommand(EGIT_PUSH_COMMAND_ID,
					structuredSelection);
		} catch (NotDefinedException e) {
			throw new ExecutionException(NLS.bind(
					UIText.CommitActionHandler_CommandNotDefined,
					EGIT_PUSH_COMMAND_ID), e);
		} catch (NotEnabledException e) {
			throw new ExecutionException(NLS.bind(
					UIText.CommitActionHandler_CommandNotEnabled,
					EGIT_PUSH_COMMAND_ID), e);
		} catch (NotHandledException e) {
			throw new ExecutionException(NLS.bind(
					UIText.CommitActionHandler_CommandNotHandled,
					EGIT_PUSH_COMMAND_ID), e);
		}
	}
}
