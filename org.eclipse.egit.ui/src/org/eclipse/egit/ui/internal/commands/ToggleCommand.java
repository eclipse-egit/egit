/*******************************************************************************
 * Copyright (c) 2020 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * A command implementation that just toggles its state.
 */
public class ToggleCommand extends AbstractHandler {

	/**
	 * The ID for the "Toggle branch hierarchy" command. Influences the
	 * {@link org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider
	 * RepositoriesViewContentProvider}.
	 */
	public static final String BRANCH_HIERARCHY_ID = "org.eclipse.egit.ui.RepositoriesToggleBranchHierarchy"; //$NON-NLS-1$

	/**
	 * The ID for the "Show short commit messages" command. Influences the
	 * {@link org.eclipse.egit.ui.internal.repository.RepositoryTreeNodeDecorator
	 * RepositoryTreeNodeDecorator}.
	 */
	public static final String COMMIT_MESSAGE_DECORATION_ID = "org.eclipse.egit.ui.RepositoriesToggleBranchCommit"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		HandlerUtil.toggleCommandState(event.getCommand());
		return null;
	}

}
