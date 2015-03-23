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
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * Toggles the "Hierarchical Branch Representation" preference
 */
public class ToggleBranchHierarchyCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {

	/**
	 * The toggle branch hierarchy command id
	 */
	public static final String ID = "org.eclipse.egit.ui.RepositoriesToggleBranchHierarchy"; //$NON-NLS-1$

	/**
	 * The toggle state of this command
	 */
	public static final String TOGGLE_STATE = "org.eclipse.ui.commands.toggleState"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		HandlerUtil.toggleCommandState(event.getCommand());
		IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
		if (part instanceof RepositoriesView) {
			CommonViewer viewer = ((RepositoriesView) part).getCommonViewer();
			viewer.refresh();
		}
		return null;
	}
}
