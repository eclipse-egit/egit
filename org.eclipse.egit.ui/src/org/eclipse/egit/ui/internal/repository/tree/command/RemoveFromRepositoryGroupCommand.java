/*******************************************************************************
 * Copyright (C) 2019, Alexander Nittka <alex@nittka.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;

/**
 * Removes selected repositories from repository groups
 */
public class RemoveFromRepositoryGroupCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryGroups groups = new RepositoryGroups();

		List<String> repoDirs = getSelectedRepositories(event);
		groups.removeFromGroups(repoDirs);
		getView(event).refresh();
		return null;
	}

	private List<String> getSelectedRepositories(ExecutionEvent event)
			throws ExecutionException {
		List<String> repoDirs = new ArrayList<>();
		List<RepositoryTreeNode> elements = getSelectedNodes(event);
		for (Object element : elements) {
			if (element instanceof RepositoryNode) {
				File dir = ((RepositoryNode) element).getRepository()
						.getDirectory();
				repoDirs.add(dir.toString());
			}
		}
		return repoDirs;
	}
}
