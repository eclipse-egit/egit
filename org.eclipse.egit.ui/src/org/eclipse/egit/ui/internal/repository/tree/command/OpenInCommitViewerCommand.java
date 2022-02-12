/*******************************************************************************
 *  Copyright (c) 2022 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.IEvaluationService;

/**
 * Opens commits pointed to by branch refs, tags, or stashes in the commit
 * viewer.
 */
public class OpenInCommitViewerCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode>
		implements IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		for (RepositoryTreeNode node : getSelectedNodes(event)) {
			RepositoryCommit commit = null;
			try {
				if (node instanceof RefNode
						|| node instanceof AdditionalRefNode) {
					Ref ref = (Ref) node.getObject();
					Repository repo = node.getRepository();
					ref = repo.exactRef(ref.getName());
					if (ref != null) {
						commit = new RepositoryCommit(repo,
								repo.parseCommit(ref.getObjectId()));
					}
				} else if (node instanceof TagNode) {
					String oid = ((TagNode) node).getCommitId();
					if (oid != null) {
						Repository repo = node.getRepository();
						commit = new RepositoryCommit(repo,
								repo.parseCommit(ObjectId.fromString(oid)));
					}
				} else if (node instanceof StashedCommitNode) {
					commit = new RepositoryCommit(node.getRepository(),
							((StashedCommitNode) node).getObject());
					commit.setStash(true);
				}
			} catch (IOException e) {
				Activator.logError(e.getLocalizedMessage(), e);
			}
			if (commit != null) {
				CommitEditor.openQuiet(commit);
			}
		}
		return null;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		IStructuredSelection selection = SelectionUtils.getSelection(
				PlatformUI.getWorkbench().getService(IEvaluationService.class)
						.getCurrentState());
		if (selection.size() > 1) {
			element.setText(UIText.RepositoriesView_OpenAllInCommitViewerLabel);
		}
	}
}
