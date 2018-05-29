/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org - set action disabled when HEAD cannot
 *    										be resolved
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Creates a branch using a simple dialog.
 * <p>
 * This is context-sensitive as it suggests the currently selected branch or (if
 * not started from a branch) the currently checked-out branch as source branch.
 * The user can override this suggestion.
 */
public class CreateBranchCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RepositoryTreeNode node = getSelectedNodes(event).get(0);

		if (node.getType() == RepositoryTreeNodeType.ADDITIONALREF) {
			Ref ref = (Ref) node.getObject();
			try (RevWalk rw = new RevWalk(node.getRepository())) {
				RevCommit baseCommit = rw
						.parseCommit(ref.getLeaf().getObjectId());
				WizardDialog dlg = new WizardDialog(
						getShell(event),
						new CreateBranchWizard(node.getRepository(), baseCommit.name()));
				dlg.setHelpAvailable(false);
				dlg.open();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			return null;
		}
		String base = null;
		if (node.getObject() instanceof Ref)
			base = ((Ref) node.getObject()).getName();
		new WizardDialog(getShell(event), new CreateBranchWizard(node
				.getRepository(), base)).open();
		return null;
	}

	@Override
	public boolean isEnabled() {
		return selectedRepositoryHasHead();
	}

}
