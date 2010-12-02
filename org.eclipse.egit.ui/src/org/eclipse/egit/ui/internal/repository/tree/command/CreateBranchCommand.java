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

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
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
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RepositoryTreeNode node = getSelectedNodes(event).get(0);

		if (node.getType() == RepositoryTreeNodeType.ADDITIONALREF) {
			Ref ref = (Ref) node.getObject();
			try {
				RevCommit baseCommit = new RevWalk(node.getRepository())
						.parseCommit(ref.getLeaf().getObjectId());
				WizardDialog dlg = new WizardDialog(
						getShell(event),
						new CreateBranchWizard(node.getRepository(), baseCommit));
				dlg.setHelpAvailable(false);
				dlg.open();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			return null;
		}
		final Ref baseBranch;
		if (node.getObject() instanceof Ref)
			baseBranch = (Ref) node.getObject();
		else {
			// we are on another node, so we have no Ref as context
			// -> try to determine the currently checked out branch
			Ref branch;
			try {
				if (node.getRepository().getFullBranch().startsWith(
						Constants.R_HEADS)) {
					// simple case: local branch checked out
					branch = node.getRepository().getRef(
							node.getRepository().getFullBranch());
				} else {
					// remote branch or tag checked out: resolve the commit
					String ref = Activator
							.getDefault()
							.getRepositoryUtil()
							.mapCommitToRef(node.getRepository(),
									node.getRepository().getFullBranch(), false);
					if (ref == null)
						branch = null;
					else {
						if (ref.startsWith(Constants.R_TAGS))
							// if a tag is checked out, we don't suggest
							// anything
							branch = null;
						else
							branch = node.getRepository().getRef(ref);
					}
				}
			} catch (IOException e) {
				branch = null;
			}
			baseBranch = branch;
		}
		new WizardDialog(getShell(event), new CreateBranchWizard(node
				.getRepository(), baseBranch)).open();
		return null;
	}
}
