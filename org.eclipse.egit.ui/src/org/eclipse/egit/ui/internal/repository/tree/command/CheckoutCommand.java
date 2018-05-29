/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

/**
 * Implements "Checkout"
 */
public class CheckoutCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> implements
		IElementUpdater {
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final RepositoryTreeNode node = getSelectedNodes(event).get(0);
		if (!(node.getObject() instanceof Ref))
			return null;

		final Ref ref = (Ref) node.getObject();
		Repository repo = node.getRepository();

		BranchOperationUI op = BranchOperationUI.checkout(repo, ref.getName());
		op.start();

		return null;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		List<RepositoryTreeNode> nodes = getSelectedNodes();
		if (!nodes.isEmpty()) {
			RepositoryTreeNode node = nodes.get(0);
			if (node.getObject() instanceof Ref) {
				Ref ref = (Ref) node.getObject();
				if (BranchOperationUI.checkoutWillShowQuestionDialog(ref
						.getName())) {
					element.setText(UIText.CheckoutCommand_CheckoutLabelWithQuestion);
					return;
				}
			}
		}
		element.setText(UIText.CheckoutCommand_CheckoutLabel);
	}
}
