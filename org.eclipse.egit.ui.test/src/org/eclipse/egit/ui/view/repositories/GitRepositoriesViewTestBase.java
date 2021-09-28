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
package org.eclipse.egit.ui.view.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.ToggleCommand;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.Version;

/**
 * Collection of utility methods for Git Repositories View tests
 */
public abstract class GitRepositoriesViewTestBase extends
		LocalRepositoryTestCase {

	// test utilities
	protected static final TestUtil myUtil = new TestUtil();

	// the human-readable view name
	protected final static String viewName = myUtil
			.getPluginLocalizedValue("GitRepositoriesView_name");

	protected static GitRepositoriesViewTestUtils myRepoViewUtil;

	@Before
	public void setup() {
		setTestUtils();
	}

	private static void setTestUtils() {
		myRepoViewUtil = new GitRepositoriesViewTestUtils();
	}

	@After
	public void teardown() {
		myRepoViewUtil.dispose();
	}

	/**
	 * remove all configured repositories from the view
	 */
	@SuppressWarnings("deprecation")
	protected static void clearView() {
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID)
				.remove(RepositoryUtil.PREFS_DIRECTORIES);
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID)
				.remove(RepositoryUtil.PREFS_DIRECTORIES_REL);
	}

	protected static void createStableBranch(Repository myRepository)
			throws IOException {
		// let's create a stable branch temporarily so
		// that we push two branches to remote
		String newRefName = "refs/heads/stable";
		RefUpdate updateRef = myRepository.updateRef(newRefName);
		Ref sourceBranch = myRepository.exactRef("refs/heads/master");
		ObjectId startAt = sourceBranch.getObjectId();
		String startBranch = Repository.shortenRefName(sourceBranch.getName());
		updateRef.setNewObjectId(startAt);
		updateRef
				.setRefLogMessage("branch: Created from " + startBranch, false); //$NON-NLS-1$
		updateRef.update();
	}

	protected static void setVerboseBranchMode(boolean state) {
		ICommandService srv = PlatformUI.getWorkbench()
				.getService(ICommandService.class);
		State verboseBranchModeState = srv
				.getCommand(ToggleCommand.COMMIT_MESSAGE_DECORATION_ID)
				.getState(RegistryToggleState.STATE_ID);
		verboseBranchModeState.setValue(Boolean.valueOf(state));
	}

	protected static boolean setLinkWithSelection(boolean state) {
		ICommandService srv = PlatformUI.getWorkbench()
				.getService(ICommandService.class);
		State linkingState = srv
				.getCommand(RepositoriesView.LINK_WITH_SELECTION_ID)
				.getState(RegistryToggleState.STATE_ID);
		boolean previousState = ((Boolean) linkingState.getValue())
				.booleanValue();
		linkingState.setValue(Boolean.valueOf(state));
		return previousState;
	}

	protected SWTBotView getOrOpenView() throws Exception {
		SWTBotView view = TestUtil.showView(RepositoriesView.VIEW_ID);
		TestUtil.joinJobs(JobFamilies.REPO_VIEW_REFRESH);
		return view;
	}

	protected void assertHasRepo(File repositoryDir) throws Exception {
		final SWTBotTree tree = getOrOpenView().bot().tree();
		final SWTBotTreeItem[] items = tree.getAllItems();
		boolean found = false;
		for (SWTBotTreeItem item : items) {
			if (item.getText().contains(repositoryDir.getParentFile().getName())) {
				found = true;
				break;
			}
		}
		assertTrue("Tree should have item with correct text", found);
	}

	protected void assertEmpty() throws Exception {
		final SWTBotView view = getOrOpenView();
		view.bot().label(UIText.RepositoriesView_messageEmpty);
	}

	protected void refreshAndWait() throws Exception {
		RepositoriesView view = (RepositoriesView) getOrOpenView()
				.getReference().getPart(true);
		view.refresh();
		try {
			Job.getJobManager().join(JobFamilies.REPO_VIEW_REFRESH,
					new TimeoutProgressMonitor(60, TimeUnit.SECONDS));
		} catch (OperationCanceledException e) {
			fail("Refresh took longer 60 seconds");
		}
		TestUtil.processUIEvents();
		TestUtil.waitForDecorations();
	}

	@Override
	protected void clearAllConfiguredRepositories() throws Exception {
		super.clearAllConfiguredRepositories();
		refreshAndWait();
	}

	@Override
	@SuppressWarnings("boxing")
	protected void assertProjectExistence(String projectName, boolean existence) {
		IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(
				projectName);
		assertEquals("Project existence " + projectName, prj.exists(),
				existence);
	}

	protected void assertWizardDialogMessage(SWTBot dialogBot,
			String expectedText) {
		// The TitleAreaDialog's title message was changed to Label in Eclipse
		// 4.18; changed back to Text in 4.21.
		Version jFaceVersion = Platform.getBundle("org.eclipse.jface")
				.getVersion();
		if (jFaceVersion.compareTo(Version.valueOf("3.22.0")) < 0
				|| jFaceVersion.compareTo(Version.valueOf("3.23.0")) >= 0) {
			dialogBot.text(expectedText);
		} else {
			dialogBot.label(expectedText);
		}
	}

	private static class TimeoutProgressMonitor extends NullProgressMonitor {

		private final long stopTime;

		public TimeoutProgressMonitor(long timeUnits, TimeUnit timeUnit) {
			stopTime = System.currentTimeMillis()
					+ timeUnit.toMillis(timeUnits);
		}

		@Override
		public boolean isCanceled() {
			boolean canceled = super.isCanceled();
			if (canceled) {
				return true;
			}
			setCanceled(System.currentTimeMillis() > stopTime);
			return super.isCanceled();
		}
	}
}
