/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.navigator.CommonActionProvider;

/**
 * Contributes the global actions (copy/paste)
 *
 */
public class RepositoriesViewActionProvider extends CommonActionProvider {

	private IAction copyAction;

	private IAction pasteAction;

	@Override
	public void fillActionBars(IActionBars actionBars) {
		if (pasteAction == null) {
			pasteAction = new Action("") { //$NON-NLS-1$

				@Override
				public void run() {
					IHandlerService srv = (IHandlerService) PlatformUI
							.getWorkbench().getService(IHandlerService.class);
					ICommandService csrv = (ICommandService) PlatformUI
							.getWorkbench().getService(ICommandService.class);
					Command openCommand = csrv
							.getCommand("org.eclipse.egit.ui.RepositoriesViewPaste"); //$NON-NLS-1$
					ExecutionEvent evt = srv.createExecutionEvent(openCommand,
							null);

					try {
						openCommand.executeWithChecks(evt);
					} catch (Exception e) {
						Activator.handleError(e.getMessage(), e, true);
					}
				}

			};
		}

		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
				pasteAction);

		if (copyAction == null) {
			copyAction = new Action("") { //$NON-NLS-1$

				@Override
				public void run() {
					IHandlerService srv = (IHandlerService) PlatformUI
							.getWorkbench().getService(IHandlerService.class);
					ICommandService csrv = (ICommandService) PlatformUI
							.getWorkbench().getService(ICommandService.class);
					Command openCommand = csrv
							.getCommand("org.eclipse.egit.ui.RepositoriesViewCopyPath"); //$NON-NLS-1$
					ExecutionEvent evt = srv.createExecutionEvent(openCommand,
							null);

					try {
						openCommand.executeWithChecks(evt);
					} catch (Exception e) {
						Activator.handleError(e.getMessage(), e, true);
					}
				}

			};
		}

		IStructuredSelection sel = (IStructuredSelection) getActionSite()
				.getViewSite().getSelectionProvider().getSelection();

		if (sel.size() == 1) {
			RepositoryTreeNode node = (RepositoryTreeNode) sel
					.getFirstElement();
			if (node.getType() == RepositoryTreeNodeType.REPO
					|| node.getType() == RepositoryTreeNodeType.FILE
					|| node.getType() == RepositoryTreeNodeType.FOLDER) {
				copyAction.setEnabled(true);
			} else if (node.getType() == RepositoryTreeNodeType.WORKINGDIR) {
				boolean isBare = node.getRepository().getConfig().getBoolean(
						"core", "bare", false); //$NON-NLS-1$//$NON-NLS-2$
				copyAction.setEnabled(!isBare);
			} else {
				copyAction.setEnabled(false);
			}
		}

		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
				copyAction);
	}
}
