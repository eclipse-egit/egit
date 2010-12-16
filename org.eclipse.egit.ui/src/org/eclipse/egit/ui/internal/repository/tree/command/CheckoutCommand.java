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
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements "Checkout"
 */
public class CheckoutCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final RepositoryTreeNode node = getSelectedNodes(event).get(0);
		if (!(node.getObject() instanceof Ref))
			return null;

		final Ref ref = (Ref) node.getObject();
		Repository repo = node.getRepository();
		String refName = ref.getLeaf().getName();
		final BranchOperationUI op;
		if (refName.startsWith(Constants.R_REFS))
			op = new BranchOperationUI(repo, ref.getName());
		else
			op = new BranchOperationUI(repo, ref.getLeaf().getObjectId());

		op.start();

		return null;
	}
}
