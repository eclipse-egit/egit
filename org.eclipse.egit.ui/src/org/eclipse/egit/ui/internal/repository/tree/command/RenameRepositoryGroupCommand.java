/*******************************************************************************
 * Copyright (C) 2019, Alexander Nittka <alex@nittka.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;

/**
 * Rename an existing repository group
 */
public class RenameRepositoryGroupCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode> elements = getSelectedNodes(event);
		if (elements.size() == 1
				&& elements.get(0) instanceof RepositoryGroupNode) {
			RepositoriesView view = getView(event);
			view.getCommonViewer().editElement(elements.get(0), 0);
		} else {
			Activator.logError(
					UIText.RepositoriesView_RepoGroup_Rename_IllegalSelection,
					null);
		}
		return null;
	}
}
