/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.stash.StashCreateUI;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command to stash current changes in working directory and index
 */
public class StashCreateCommand extends
		RepositoriesViewCommandHandler<RepositoryNode> {

	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.team.stash.create"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryNode> nodes = getSelectedNodes(event);
		if (nodes.isEmpty())
			return null;
		Repository repo = nodes.get(0).getRepository();
		if (repo == null)
			return null;

		final Shell shell = HandlerUtil.getActiveShell(event);
		StashCreateUI stashCreateUI = new StashCreateUI(repo);
		stashCreateUI.createStash(shell);

		return null;
	}

}
