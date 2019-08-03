/*******************************************************************************
 * Copyright (c) 2010, 2012 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Benjamin Muskalla (EclipseSource) - initial implementation
 *    Mathias Kinzler <mathias.kinzler@sap.com> - replace InputDialog with RenameBranchDialog
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.dialogs.BranchRenameDialog;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;

/**
 * Renames a branch
 */
public class RenameBranchCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final List<RepositoryTreeNode> nodes = getSelectedNodes(event);
		RepositoryTreeNode node = nodes.get(0);

		Repository repository = node.getRepository();
		Ref branch = null;
		if (node instanceof RefNode) {
			branch = (Ref) node.getObject();
		} else if (node instanceof RepositoryNode) {
			try {
				branch = repository
						.exactRef(Constants.R_HEADS + repository.getBranch());
			} catch (IOException e) {
				Activator.logError("Cannot rename branch", e); //$NON-NLS-1$
			}
		}

		if (branch != null) {
			Shell shell = getShell(event);
			new BranchRenameDialog(shell, repository, branch).open();
		}
		return null;
	}
}
