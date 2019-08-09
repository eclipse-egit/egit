/*******************************************************************************
 * Copyright (c) 2010, 2018 SAP AG and others.
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
package org.eclipse.egit.ui.view.repositories;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.egit.ui.internal.repository.RepositoryTreeNodeLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefsNode;
import org.eclipse.egit.ui.internal.repository.tree.BranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.LocalNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteTrackingNode;
import org.eclipse.egit.ui.internal.repository.tree.RemotesNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.StashNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;

public class GitRepositoriesViewTestUtils {

	/**
	 * Create a new instance of {@link RepositoryTreeNodeLabelProvider}
	 *
	 * @return label provider
	 */
	public static RepositoryTreeNodeLabelProvider createLabelProvider() {
		final AtomicReference<RepositoryTreeNodeLabelProvider> providerRef = new AtomicReference<>();
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				providerRef.set(new RepositoryTreeNodeLabelProvider());
			}

		});
		return providerRef.get();
	}

	protected static final TestUtil myUtil = new TestUtil();

	private final RepositoryTreeNodeLabelProvider labelProvider;

	/**
	 * Create repositories view test utilities
	 */
	public GitRepositoriesViewTestUtils() {
		labelProvider = createLabelProvider();
	}

	public void dispose() {
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> labelProvider.dispose());
	}

	public SWTBotTreeItem getLocalBranchesItem(SWTBotTree tree, File repo)
			throws Exception {
		Repository repository = lookupRepository(repo);
		RepositoryNode root = new RepositoryNode(null, repository);
		BranchesNode branches = new BranchesNode(root, repository);
		LocalNode localBranches = new LocalNode(branches,
				repository);

		String rootText = labelProvider.getText(root);
		String branchesText = labelProvider.getText(branches);
		String localText = labelProvider.getText(localBranches);
		SWTBotTreeItem localItem = TestUtil.navigateTo(tree, rootText,
				branchesText, localText);
		return localItem;
	}

	public SWTBotTreeItem getTagsItem(SWTBotTree tree, File repo)
			throws Exception {
		Repository repository = lookupRepository(repo);
		RepositoryNode root = new RepositoryNode(null, repository);
		TagsNode tags = new TagsNode(root, repository);

		String rootText = labelProvider.getText(root);
		String tagsText = labelProvider.getText(tags);
		SWTBotTreeItem tagsItem = TestUtil.navigateTo(tree, rootText, tagsText);
		return tagsItem;
	}

	public SWTBotTreeItem getRemoteBranchesItem(SWTBotTree tree,
			File repositoryFile) throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		BranchesNode branches = new BranchesNode(root, repository);
		RemoteTrackingNode remoteBranches = new RemoteTrackingNode(branches,
				repository);

		String rootText = labelProvider.getText(root);
		String branchesText = labelProvider.getText(branches);
		String remoteText = labelProvider.getText(remoteBranches);
		SWTBotTreeItem remoteItem = TestUtil.navigateTo(tree, rootText,
				branchesText, remoteText);
		return remoteItem;
	}

	public SWTBotTreeItem getWorkdirItem(SWTBotTree tree, File repositoryFile)
			throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		WorkingDirNode workdir = new WorkingDirNode(root, repository);

		String rootText = labelProvider.getText(root);
		String workDirText = labelProvider.getText(workdir);
		SWTBotTreeItem workdirItem = TestUtil.navigateTo(tree, rootText,
				workDirText);
		return workdirItem;
	}

	public SWTBotTreeItem getRootItem(SWTBotTree tree, File repositoryFile)
			throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);

		String rootText = labelProvider.getText(root);
		SWTBotTreeItem rootItem = TestUtil.navigateTo(tree, rootText);
		return rootItem;
	}

	public SWTBotTreeItem getSymbolicRefsItem(SWTBotTree tree,
			File repositoryFile) throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		AdditionalRefsNode symrefsnode = new AdditionalRefsNode(root, repository);

		String rootText = labelProvider.getText(root);
		String symrefsText = labelProvider.getText(symrefsnode);
		SWTBotTreeItem symrefsitem = TestUtil.navigateTo(tree, rootText,
				symrefsText);
		return symrefsitem;
	}

	public SWTBotTreeItem getRemotesItem(SWTBotTree tree, File repositoryFile)
			throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		RemotesNode remotes = new RemotesNode(root, repository);

		String rootText = labelProvider.getText(root);
		String remotesText = labelProvider.getText(remotes);
		SWTBotTreeItem remotesItem = TestUtil.navigateTo(tree, rootText,
				remotesText);
		return remotesItem;
	}

	public SWTBotTreeItem getStashesItem(SWTBotTree tree, File repositoryFile)
			throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		StashNode stashes = new StashNode(root, repository);

		String rootText = labelProvider.getText(root);
		String stashesText = labelProvider.getText(stashes);
		SWTBotTreeItem stashesItem = TestUtil.navigateTo(tree, rootText,
				stashesText);
		return stashesItem;
	}

	public Repository lookupRepository(File directory) throws Exception {
		return org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().lookupRepository(directory);
	}
}
