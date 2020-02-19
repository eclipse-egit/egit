/*******************************************************************************
 * Copyright (C) 2020, Alexander Nittka <alex@nittka.de>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.merge.GitCompareEditorInput;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Compared the commits referenced by two refs
 */
public class CompareCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode> nodes = getSelectedNodes();
		Ref ref1 = getRef(nodes.get(0));
		Ref ref2 = getRef(nodes.get(1));
		if (ref1 != null && ref2 != null) {
			Repository repo = (nodes.get(0).getRepository());
			try {
				RevCommit commit1 = repo.parseCommit(ref1.getObjectId());
				RevCommit commit2 = repo.parseCommit(ref2.getObjectId());
				if (commit1.getCommitTime() <= commit2.getCommitTime()) {
					compare(event, repo, commit1.getName(), commit2.getName());
				} else {
					compare(event, repo, commit2.getName(), commit1.getName());
				}
			} catch (IOException e) {
				throw new ExecutionException(e.getMessage(), e);
			}
		}
		return null;
	}

	void compare(ExecutionEvent event, Repository repo,
			String commit1, String commit2) throws ExecutionException {
		IWorkbenchPage workbenchPage = HandlerUtil
				.getActiveWorkbenchWindowChecked(event).getActivePage();
		GitCompareEditorInput compareInput = new GitCompareEditorInput(commit1,
				commit2, repo);
		CompareUtils.openInCompare(workbenchPage, compareInput);
	}

	private Ref getRef(RepositoryTreeNode node) {
		if (node instanceof TagNode) {
			return ((TagNode) node).getObject();
		} else if (node instanceof RefNode
				|| node instanceof AdditionalRefNode) {
			return (Ref) node.getObject();
		}
		return null;
	}


	@Override
	public boolean isEnabled() {
		List<RepositoryTreeNode> nodes = getSelectedNodes();
		if (nodes.size() == 2) {
			return nodes.stream().map(RepositoryTreeNode::getRepository)
					.distinct().count() == 1;
		}
		return false;
	}

}
