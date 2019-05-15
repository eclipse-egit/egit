/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withMnemonic;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveView;
import org.eclipse.egit.ui.internal.reflog.ReflogView;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestBase;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarDropDownButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.internal.ui.history.GenericHistoryView;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for EGit views reacting on repository changes.
 */
@SuppressWarnings("restriction")
@RunWith(SWTBotJunit4ClassRunner.class)
public class SelectionForViewsTest extends GitRepositoriesViewTestBase {

	private File localRepositoryDir; // Normal repo

	private File remoteRepositoryDir; // Bare repo

	private File clonedRepositoryDir; // 2nd normal repo

	private SWTBotView stagingView;

	private SWTBotView reflogView;

	private SWTBotView rebaseInteractiveView;

	private SWTBotView historyView;

	private SWTBotView repoView;

	@Before
	public void before() throws Exception {
		localRepositoryDir = createProjectAndCommitToRepository();
		remoteRepositoryDir = createRemoteRepository(localRepositoryDir);
		URIish uri = new URIish("file:///" + remoteRepositoryDir.getPath());
		File workdir = new File(getTestDirectory(), "ClonedRepo");
		CloneOperation op = new CloneOperation(uri, true, null, workdir,
				"refs/heads/master", "origin", 0);
		op.run(null);
		clonedRepositoryDir = new File(workdir, Constants.DOT_GIT);
		RepositoryUtil repoUtil = Activator.getDefault().getRepositoryUtil();
		repoUtil.addConfiguredRepository(localRepositoryDir);
		repoUtil.addConfiguredRepository(clonedRepositoryDir);
		repoUtil.addConfiguredRepository(remoteRepositoryDir); // it's bare
		stagingView = TestUtil.showView(StagingView.VIEW_ID);
		reflogView = TestUtil.showView(ReflogView.VIEW_ID);
		rebaseInteractiveView = TestUtil
				.showView(RebaseInteractiveView.VIEW_ID);
		repoView = TestUtil.showView(RepositoriesView.VIEW_ID);
		RepositoriesView repos = (RepositoriesView) repoView.getViewReference()
				.getView(false);
		repos.setReactOnSelection(true);
		historyView = TestUtil.showHistoryView();
		IHistoryView history = (IHistoryView) historyView.getViewReference()
				.getView(false);
		((GenericHistoryView) history).setLinkingEnabled(true);
		// Ensure that the git history page is active
		Exception[] exception = { null };
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			try {
				history.showHistoryFor(new RepositoryNode(null,
						lookupRepository(localRepositoryDir)), true);
			} catch (Exception e) {
				exception[0] = e;
			}
		});
		if (exception[0] != null) {
			throw exception[0];
		}
		waitForRefreshes();
	}

	@After
	public void after() {
		RepositoriesView repos = (RepositoriesView) repoView.getViewReference()
				.getView(false);
		repos.setReactOnSelection(false);
		IHistoryView history = (IHistoryView) historyView.getViewReference()
				.getView(false);
		((GenericHistoryView) history).setLinkingEnabled(false);
		stagingView = null;
		reflogView = null;
		rebaseInteractiveView = null;
		historyView = null;
		repoView = null;
	}

	private void assertRepoSelection(SWTBotView view, File repoDir)
			throws Exception {
		view.show();
		waitForRefreshes();
		String viewId = view.getViewReference().getId();
		ISelectionProvider selectionProvider = view.getViewReference()
				.getView(false).getViewSite().getSelectionProvider();
		assertNotNull("No selection provider " + viewId, selectionProvider);
		ISelection selection = UIThreadRunnable
				.syncExec(() -> selectionProvider.getSelection());
		assertTrue("Expected an IStructuredSelection " + viewId,
				selection instanceof IStructuredSelection);
		assertFalse("Expected a non-empty selection " + viewId,
				selection.isEmpty());
		Object firstElement = ((IStructuredSelection) selection)
				.getFirstElement();
		assertNotNull("Null in selection " + viewId, firstElement);
		Repository repo = Adapters.adapt(firstElement, Repository.class);
		assertNotNull("Expected a repository " + viewId + ", but "
				+ firstElement.getClass().getName()
				+ " doesn't adapt to Repository", repo);
		assertEquals("Wrong directory " + viewId, repoDir, repo.getDirectory());
	}

	private void assertAllViews(File repoDir) throws Exception {
		assertRepoSelection(repoView, repoDir);
		assertRepoSelection(reflogView, repoDir);
		assertRepoSelection(rebaseInteractiveView, repoDir);
		assertRepoSelection(stagingView, repoDir);
		assertRepoSelection(historyView, repoDir);
	}

	private void waitForRefreshes() throws Exception {
		TestUtil.joinJobs(JobFamilies.GENERATE_HISTORY);
		TestUtil.joinJobs(JobFamilies.STAGING_VIEW_RELOAD);
		// Join UI update triggered by GenerateHistoryJob
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			// Nothing
		});
		refreshAndWait();
	}

	private String repositoryName(File repoDir) throws Exception {
		return org.eclipse.egit.core.Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(lookupRepository(repoDir));
	}

	@Test
	public void testViewsReactOnRepoViewSelection() throws Exception {
		repoView.show();
		repoView.setFocus();
		SWTBotTree tree = repoView.bot().tree();
		SWTBotTreeItem repoNode = myRepoViewUtil.getRootItem(tree,
				clonedRepositoryDir);
		repoNode.select();
		assertAllViews(clonedRepositoryDir);
	}

	@Test
	public void testViewsReactOnBareRepoViewSelection() throws Exception {
		repoView.show();
		repoView.setFocus();
		SWTBotTree tree = repoView.bot().tree();
		SWTBotTreeItem repoNode = myRepoViewUtil.getRootItem(tree,
				remoteRepositoryDir);
		repoNode.select();
		assertAllViews(remoteRepositoryDir);
	}

	@Ignore("Doesn't work for an unknown reason. Other views do not update in test, while this works in production.")
	@Test
	public void testViewsReactOnRepoSwitchInStaging() throws Exception {
		stagingView.show();
		stagingView.setFocus();
		waitForRefreshes();
		SWTBotToolbarDropDownButton button = stagingView
				.toolbarDropDownButton(UIText.RepositoryToolbarAction_tooltip);
		button.menuItem(allOf(instanceOf(MenuItem.class),
				withMnemonic(repositoryName(clonedRepositoryDir)))).click();
		TestUtil.joinJobs(JobFamilies.STAGING_VIEW_RELOAD);
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			// Nothing
		});
		assertAllViews(clonedRepositoryDir);
		stagingView.show();
		stagingView.setFocus();
		waitForRefreshes();
		button = stagingView
				.toolbarDropDownButton(UIText.RepositoryToolbarAction_tooltip);
		button.menuItem(allOf(instanceOf(MenuItem.class),
				withMnemonic(repositoryName(clonedRepositoryDir)))).click();
		TestUtil.joinJobs(JobFamilies.STAGING_VIEW_RELOAD);
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			// Nothing
		});
		assertAllViews(localRepositoryDir);
	}
}
