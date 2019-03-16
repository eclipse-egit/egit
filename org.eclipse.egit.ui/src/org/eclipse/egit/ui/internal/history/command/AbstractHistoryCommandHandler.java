/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2015, IBM Corporation (Dani Megert <daniel_megert@ch.ibm.com>)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Common helper methods for the history command handlers
 */
abstract class AbstractHistoryCommandHandler extends AbstractHandler {
	protected IWorkbenchPart getPart(ExecutionEvent event)
			throws ExecutionException {
		return HandlerUtil.getActivePartChecked(event);
	}

	private Object getInput(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = getPart(event);
		if (!(part instanceof IHistoryView))
			throw new ExecutionException(
					UIText.AbstractHistoryCommanndHandler_NoInputMessage);
		return (((IHistoryView) part).getHistoryPage().getInput());
	}

	protected Repository getRepository(ExecutionEvent event)
			throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (!selection.isEmpty()) {
			IRepositoryCommit commit = AdapterUtils
					.adapt(selection.getFirstElement(), IRepositoryCommit.class);
			if (commit != null) {
				return commit.getRepository();
			}
		}
		Object input = getInput(event);
		if (input == null) {
			return null;
		}
		if (input instanceof HistoryPageInput) {
			return ((HistoryPageInput) input).getRepository();
		}
		if (input instanceof RepositoryTreeNode) {
			return ((RepositoryTreeNode) input).getRepository();
		}
		Repository repo = AdapterUtils.adapt(input, Repository.class);
		if (repo != null) {
			return repo;
		}
		IResource resource = AdapterUtils.adaptToAnyResource(input);
		if (resource != null) {
			Repository repository = ResourceUtil.getRepository(resource);
			if (repository != null) {
				return repository;
			}
		}

		throw new ExecutionException(
				UIText.AbstractHistoryCommanndHandler_CouldNotGetRepositoryMessage);
	}

	protected String getRepoRelativePath(Repository repo, File file) {
		IPath workdirPath = new Path(repo.getWorkTree().getPath());
		IPath filePath = new Path(file.getPath()).setDevice(null);
		return filePath.removeFirstSegments(workdirPath.segmentCount())
				.toString();
	}

	/**
	 * @param event
	 * @return the tags
	 * @throws ExecutionException
	 */
	protected List<RevTag> getRevTags(ExecutionEvent event)
			throws ExecutionException {
		Repository repo = getRepository(event);
		try (RevWalk walk = new RevWalk(repo)) {
			Collection<Ref> revTags = repo.getRefDatabase()
					.getRefsByPrefix(Constants.R_TAGS);
			List<RevTag> tags = new ArrayList<>();
			for (Ref ref : revTags) {
				tags.add(walk.parseTag(repo.resolve(ref.getName())));
			}
			return tags;
		} catch (IOException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
	}

	protected GitHistoryPage getPage() {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window == null)
			return null;
		if (window.getActivePage() == null)
			return null;
		IWorkbenchPart part = window.getActivePage().getActivePart();
		return getPageFromPart(part);
	}

	protected GitHistoryPage getPage(ExecutionEvent event)
			throws ExecutionException {
		IWorkbenchPart part = getPart(event);
		return getPageFromPart(part);
	}

	private GitHistoryPage getPageFromPart(IWorkbenchPart part) {
		if (!(part instanceof IHistoryView))
			return null;
		IHistoryView view = (IHistoryView) part;
		IHistoryPage page = view.getHistoryPage();
		if (page instanceof GitHistoryPage)
			return (GitHistoryPage) page;
		return null;
	}

	protected IStructuredSelection getSelection(GitHistoryPage page) {
		if (page == null)
			return StructuredSelection.EMPTY;
		ISelection selection = page.getSelectionProvider().getSelection();
		return getStructuredSelection(selection);
	}

	protected IStructuredSelection getSelection(ExecutionEvent event)
			throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		return getStructuredSelection(selection);
	}

	private IStructuredSelection getStructuredSelection(ISelection selection) {
		if (selection instanceof IStructuredSelection)
			return (IStructuredSelection) selection;
		else
			return StructuredSelection.EMPTY;
	}

	/**
	 * @param event
	 * @return ID of selected commit
	 * @throws ExecutionException
	 *             if no or multiple commits were found
	 */
	protected ObjectId getSelectedCommitId(ExecutionEvent event)
			throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (selection.size() != 1) {
			throw new ExecutionException(
					UIText.AbstractHistoryCommandHandler_ActionRequiresOneSelectedCommitMessage);
		}
		RevCommit commit = AdapterUtils.adapt(selection.getFirstElement(),
				RevCommit.class);
		if (commit == null) {
			throw new ExecutionException(
					UIText.AbstractHistoryCommandHandler_ActionRequiresOneSelectedCommitMessage);
		}
		return commit.getId();
	}

	/**
	 * Gets the selected commit, re-parsed to have correct parent information
	 * regardless of how history was walked.
	 *
	 * @param event
	 * @return the selected commit, never null
	 * @throws ExecutionException
	 *             if no or multiple commits were found
	 */
	protected RevCommit getSelectedCommit(ExecutionEvent event)
			throws ExecutionException {
		List<RevCommit> commits = getSelectedCommits(event);
		if (commits.size() != 1)
			throw new ExecutionException(
					UIText.AbstractHistoryCommandHandler_ActionRequiresOneSelectedCommitMessage);
		return commits.get(0);
	}

	/**
	 * Gets the selected commits, re-parsed to have correct parent information
	 * regardless of how history was walked.
	 *
	 * @param event
	 * @return the selected commits, or an empty list
	 * @throws ExecutionException
	 */
	protected List<RevCommit> getSelectedCommits(ExecutionEvent event)
			throws ExecutionException {
		Repository repository = getRepository(event);
		if (repository == null)
			return Collections.emptyList();
		IStructuredSelection selection = getSelection(event);
		if (selection.isEmpty())
			return Collections.emptyList();
		List<RevCommit> commits = new ArrayList<>();
		try (RevWalk walk = new RevWalk(repository)) {
			for (Object element : selection.toList()) {
				RevCommit commit = AdapterUtils.adapt(element, RevCommit.class);
				if (commit != null) {
					// Re-parse commit to clear effects of TreeFilter
					RevCommit reparsed = walk.parseCommit(commit.getId());
					commits.add(reparsed);
				}
			}
		} catch (IOException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		return commits;
	}

	/**
	 * Utility to get a list of Refs from a commit in order to handle ambiguous
	 * selections when a Ref is preferred over a commit.
	 *
	 * @param commit
	 * @param repo
	 * @param refPrefixes
	 *            e.g. "refs/heads/" or ""
	 * @return a list of RefNodes
	 */
	protected List<RefNode> getRefNodes(ObjectId commit, Repository repo,
			String... refPrefixes) {
		List<Ref> availableBranches = new ArrayList<>();
		List<RefNode> nodes = new ArrayList<>();
		try {
			List<Ref> branches = new ArrayList<>();
			for (String refPrefix : refPrefixes) {
				branches.addAll(
						repo.getRefDatabase().getRefsByPrefix(refPrefix));
			}
			for (Ref branch : branches) {
				ObjectId objectId = branch.getLeaf().getObjectId();
				if (objectId != null && objectId.equals(commit)) {
					availableBranches.add(branch);
				}
			}
			RepositoryNode repoNode = new RepositoryNode(null, repo);
			for (Ref ref : availableBranches) {
				nodes.add(new RefNode(repoNode, repo, ref));
			}

		} catch (IOException e) {
			// ignore here
		}
		return nodes;
	}

	protected List<Ref> getBranchesOfCommit(IStructuredSelection selection,
			final Repository repo, boolean hideCurrentBranch)
			throws IOException {
		String head = repo.getFullBranch();
		return getBranchesOfCommit(selection, head, hideCurrentBranch);
	}

	protected List<Ref> getBranchesOfCommit(IStructuredSelection selection) {
		return getBranchesOfCommit(selection, (String) null, false);
	}

	private List<Ref> getBranchesOfCommit(IStructuredSelection selection,
			String head, boolean hideCurrentBranch) {
		final List<Ref> branchesOfCommit = new ArrayList<>();
		if (selection.isEmpty()) {
			return branchesOfCommit;
		}
		RevCommit revCommit = AdapterUtils.adapt(selection.getFirstElement(),
				RevCommit.class);
		if (!(revCommit instanceof PlotCommit)) {
			return branchesOfCommit;
		}
		PlotCommit commit = (PlotCommit) revCommit;

		int refCount = commit.getRefCount();
		for (int i = 0; i < refCount; i++) {
			Ref ref = commit.getRef(i);
			String refName = ref.getName();
			if (hideCurrentBranch && head != null && refName.equals(head))
				continue;
			if (refName.startsWith(Constants.R_HEADS)
					|| refName.startsWith(Constants.R_REMOTES))
				branchesOfCommit.add(ref);
		}
		return branchesOfCommit;
	}

	protected Repository getRepository(GitHistoryPage page) {
		if (page == null)
			return null;
		HistoryPageInput input = page.getInputInternal();
		if (input == null)
			return null;

		final Repository repository = input.getRepository();
		return repository;
	}

	/**
	 * Get renamed path in commit
	 *
	 * @param path
	 * @param commit
	 * @return path respecting renames
	 */
	protected String getRenamedPath(final String path, final ObjectId commit) {
		return getPage().getRenamedPath(path, commit);
	}
}
