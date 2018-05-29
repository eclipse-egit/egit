/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
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

import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefsNode;
import org.eclipse.egit.ui.internal.repository.tree.BranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.LocalNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteTrackingNode;
import org.eclipse.egit.ui.internal.repository.tree.RemotesNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class GitRepositoriesViewTestUtils {

	/**
	 * Create a new instance of {@link RepositoriesViewLabelProvider}
	 *
	 * @return label provider
	 */
	public static RepositoriesViewLabelProvider createLabelProvider() {
		final AtomicReference<RepositoriesViewLabelProvider> providerRef = new AtomicReference<RepositoriesViewLabelProvider>();
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				providerRef.set(new RepositoriesViewLabelProvider());
			}

		});
		return providerRef.get();
	}

	protected static final TestUtil myUtil = new TestUtil();

	private final RepositoriesViewLabelProvider labelProvider;

	/**
	 * Create repositories view test utilities
	 */
	public GitRepositoriesViewTestUtils() {
		labelProvider = createLabelProvider();
	}

	public SWTBotTreeItem getLocalBranchesItem(SWTBotTree tree, File repo)
			throws Exception {
		Repository repository = lookupRepository(repo);
		RepositoryNode root = new RepositoryNode(null, repository);
		BranchesNode branches = new BranchesNode(root, repository);
		LocalNode localBranches = new LocalNode(branches,
				repository);

		String rootText = labelProvider.getStyledText(root).getString();
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		SWTBotTreeItem branchesItem = TestUtil.expandAndWait(rootItem)
				.getNode(labelProvider.getStyledText(branches).getString());
		SWTBotTreeItem localItem = TestUtil.expandAndWait(branchesItem).getNode(
				labelProvider.getStyledText(localBranches).getString());
		return localItem;
	}

	public SWTBotTreeItem getTagsItem(SWTBotTree tree, File repo)
			throws Exception {
		Repository repository = lookupRepository(repo);
		RepositoryNode root = new RepositoryNode(null, repository);
		TagsNode tags = new TagsNode(root, repository);

		String rootText = labelProvider.getStyledText(root).getString();
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		SWTBotTreeItem tagsItem = TestUtil.expandAndWait(rootItem)
				.getNode(labelProvider.getStyledText(tags).getString());
		return tagsItem;
	}

	public SWTBotTreeItem getRemoteBranchesItem(SWTBotTree tree,
			File repositoryFile) throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		BranchesNode branches = new BranchesNode(root, repository);
		RemoteTrackingNode remoteBranches = new RemoteTrackingNode(branches,
				repository);

		String rootText = labelProvider.getStyledText(root).getString();
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		SWTBotTreeItem branchesItem = TestUtil.expandAndWait(rootItem)
				.getNode(labelProvider.getStyledText(branches).getString());
		SWTBotTreeItem remoteItem = TestUtil.expandAndWait(branchesItem)
				.getNode(labelProvider.getStyledText(remoteBranches)
						.getString());
		return remoteItem;
	}

	public SWTBotTreeItem getWorkdirItem(SWTBotTree tree, File repositoryFile)
			throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);

		WorkingDirNode workdir = new WorkingDirNode(root, repository);

		String rootText = labelProvider.getStyledText(root).getString();
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		SWTBotTreeItem workdirItem = TestUtil.expandAndWait(rootItem)
				.getNode(labelProvider.getStyledText(workdir).getString());
		return workdirItem;
	}

	public SWTBotTreeItem getRootItem(SWTBotTree tree, File repositoryFile)
			throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		String rootText = labelProvider.getStyledText(root).getString();
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		return rootItem;
	}

	public SWTBotTreeItem getSymbolicRefsItem(SWTBotTree tree,
			File repositoryFile) throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		AdditionalRefsNode symrefsnode = new AdditionalRefsNode(root, repository);
		SWTBotTreeItem rootItem = tree
				.getTreeItem(labelProvider.getStyledText(root).getString());
		SWTBotTreeItem symrefsitem = TestUtil.expandAndWait(rootItem)
				.getNode(labelProvider.getStyledText(symrefsnode).getString());
		return symrefsitem;
	}

	public SWTBotTreeItem getRemotesItem(SWTBotTree tree, File repositoryFile)
			throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		RemotesNode remotes = new RemotesNode(root, repository);

		String rootText = labelProvider.getStyledText(root).getString();
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		SWTBotTreeItem remotesItem = TestUtil.expandAndWait(rootItem)
				.getNode(labelProvider.getStyledText(remotes).getString());
		return remotesItem;
	}

	public Repository lookupRepository(File directory) throws Exception {
		return org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().lookupRepository(directory);
	}
}
