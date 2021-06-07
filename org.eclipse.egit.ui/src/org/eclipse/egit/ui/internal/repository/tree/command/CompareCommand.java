/*******************************************************************************
 * Copyright (C) 2020, 2021 Alexander Nittka <alex@nittka.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.merge.GitCompareEditorInput;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.IEvaluationService;

/**
 * Compares the commits referenced by two refs, or the working tree against a
 * selected ref.
 */
public class CompareCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode>
		implements IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode> nodes = getSelectedNodes();
		List<Ref> refs = new ArrayList<>();
		nodes.forEach(node -> {
			Ref ref = getRef(node);
			if (ref != null) {
				refs.add(ref);
			}
		});
		int numberOfRefs = refs.size();
		try {
			if (numberOfRefs == 2) {
				Repository repo = nodes.get(0).getRepository();
				IWorkbenchPage workbenchPage = HandlerUtil
						.getActiveWorkbenchWindowChecked(event).getActivePage();
				// Use the older one as base
				RevCommit a = repo.parseCommit(refs.get(0).getObjectId());
				RevCommit b = repo.parseCommit(refs.get(1).getObjectId());
				if (a.getCommitTime() <= b.getCommitTime()) {
					compare(workbenchPage, repo, b.getName(), a.getName());
				} else {
					compare(workbenchPage, repo, a.getName(), b.getName());
				}
			} else if (numberOfRefs == 1) {
				Repository repo = nodes.get(0).getRepository();
				IWorkbenchPage workbenchPage = HandlerUtil
						.getActiveWorkbenchWindowChecked(event).getActivePage();
				RevCommit a = repo.parseCommit(refs.get(0).getObjectId());
				compare(workbenchPage, repo, null, a.getName());
			}
		} catch (IOException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
		return null;
	}

	/**
	 * Shows the comparison {@code baseCommit..compareCommit}.
	 *
	 * @param workbenchPage
	 *            to show the result in
	 * @param repo
	 *            of the two commits
	 * @param compareCommit
	 *            commit ID
	 * @param baseCommit
	 *            commit ID
	 * @throws ExecutionException
	 *             if the comparison cannot be shown
	 */
	protected void compare(IWorkbenchPage workbenchPage, Repository repo,
			String compareCommit, String baseCommit) throws ExecutionException {
		GitCompareEditorInput compareInput = new GitCompareEditorInput(
				compareCommit, baseCommit, repo);
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
		int numberOfNodes = nodes.size();
		if (numberOfNodes > 1) {
			return numberOfNodes == 2
					&& nodes.stream().map(RepositoryTreeNode::getRepository)
					.distinct().count() == 1;
		}
		return numberOfNodes == 1 && getRef(nodes.get(0)) != null;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		IStructuredSelection selection = SelectionUtils.getSelection(
				PlatformUI.getWorkbench().getService(IEvaluationService.class)
						.getCurrentState());
		if (selection.size() == 1) {
			element.setText(UIText.CompareCommand_WithWorkingTreeLabel);
		}
	}

}
