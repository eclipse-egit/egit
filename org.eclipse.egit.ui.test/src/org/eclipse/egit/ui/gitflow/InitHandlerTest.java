/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.gitflow;

import static org.eclipse.egit.gitflow.ui.internal.UIText.InitDialog_developBranch;
import static org.eclipse.egit.gitflow.ui.internal.UIText.InitDialog_featureBranchPrefix;
import static org.eclipse.egit.gitflow.ui.internal.UIText.InitDialog_hotfixBranchPrefix;
import static org.eclipse.egit.gitflow.ui.internal.UIText.InitDialog_masterBranch;
import static org.eclipse.egit.gitflow.ui.internal.UIText.InitDialog_releaseBranchPrefix;
import static org.eclipse.egit.gitflow.ui.internal.UIText.InitDialog_versionTagPrefix;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellIsActive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.gitflow.GitFlowConfig;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Gitflow init
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class InitHandlerTest extends AbstractGitflowHandlerTest {
	private static final String DEVELOP_BRANCH = "a";

	private static final String MASTER_BRANCH_MISSING = "b";

	private static final String MASTER_BRANCH = "master";

	private static final String FEATURE_BRANCH_PREFIX = "c";
	private static final String RELEASE_BRANCH_PREFIX = "d";
	private static final String HOTFIX_BRANCH_PREFIX = "e";
	private static final String VERSION_TAG_PREFIX = "f";

	private static final String ILLEGAL_BRANCH_NAME = "!@#$%^&*()_";

	@Test
	public void testInit() throws Exception {
		selectProject(PROJ1);
		clickInit();
		fillDialog(MASTER_BRANCH);

		bot.waitUntil(Conditions.waitForJobs(JobFamilies.GITFLOW_FAMILY,
				"Git flow jobs"));

		GitFlowRepository gitFlowRepository = new GitFlowRepository(repository);
		GitFlowConfig config = gitFlowRepository.getConfig();

		assertEquals(DEVELOP_BRANCH, repository.getBranch());
		assertEquals(MASTER_BRANCH, config.getMaster());
		assertEquals(FEATURE_BRANCH_PREFIX, config.getFeaturePrefix());
		assertEquals(RELEASE_BRANCH_PREFIX, config.getReleasePrefix());
		assertEquals(HOTFIX_BRANCH_PREFIX, config.getHotfixPrefix());
		assertEquals(VERSION_TAG_PREFIX, config.getVersionTagPrefix());
	}

	@Test
	public void testInitMissingMaster() throws Exception {
		selectProject(PROJ1);
		clickInit();
		fillDialog(MASTER_BRANCH_MISSING);

		bot.waitUntil(shellIsActive(UIText.InitDialog_masterBranchIsMissing));
		bot.button("Yes").click();
		bot.waitUntil(Conditions.waitForJobs(JobFamilies.GITFLOW_FAMILY,
				"Git flow jobs"));

		GitFlowRepository gitFlowRepository = new GitFlowRepository(repository);
		GitFlowConfig config = gitFlowRepository.getConfig();

		assertEquals(DEVELOP_BRANCH, repository.getBranch());
		assertEquals(MASTER_BRANCH_MISSING, config.getMaster());
		assertEquals(FEATURE_BRANCH_PREFIX, config.getFeaturePrefix());
		assertEquals(RELEASE_BRANCH_PREFIX, config.getReleasePrefix());
		assertEquals(HOTFIX_BRANCH_PREFIX, config.getHotfixPrefix());
		assertEquals(VERSION_TAG_PREFIX, config.getVersionTagPrefix());

		assertNotNull(repository.exactRef(Constants.R_HEADS + DEVELOP_BRANCH));
	}

	@Test
	public void testInitEmptyRepoMissingMaster() throws Exception {
		String projectName = "AnyProjectName";
		TestRepository testRepository = createAndShare(projectName);

		selectProject(projectName);
		clickInit();
		bot.button("Yes").click();
		fillDialog(MASTER_BRANCH_MISSING);
		bot.waitUntil(shellIsActive(UIText.InitDialog_masterBranchIsMissing));
		bot.button("Yes").click();
		bot.waitUntil(Conditions.waitForJobs(JobFamilies.GITFLOW_FAMILY,
				"Git flow jobs"));

		Repository localRepository = testRepository.getRepository();
		GitFlowRepository gitFlowRepository = new GitFlowRepository(localRepository);
		GitFlowConfig config = gitFlowRepository.getConfig();

		assertEquals(DEVELOP_BRANCH, localRepository.getBranch());
		assertEquals(MASTER_BRANCH_MISSING, config.getMaster());
		assertEquals(FEATURE_BRANCH_PREFIX, config.getFeaturePrefix());
		assertEquals(RELEASE_BRANCH_PREFIX, config.getReleasePrefix());
		assertEquals(HOTFIX_BRANCH_PREFIX, config.getHotfixPrefix());
		assertEquals(VERSION_TAG_PREFIX, config.getVersionTagPrefix());

		assertNotNull(
				localRepository.exactRef(Constants.R_HEADS + DEVELOP_BRANCH));
	}

	private void selectProject(String projectName) {
		final SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, projectName).select();
	}

	private void fillDialog(String masterBranch) {
		typeInto(InitDialog_developBranch, ILLEGAL_BRANCH_NAME);

		SWTBotButton ok = bot.button("OK");
		assertFalse(ok.isEnabled());

		typeInto(InitDialog_developBranch, DEVELOP_BRANCH);
		typeInto(InitDialog_masterBranch, masterBranch);
		typeInto(InitDialog_featureBranchPrefix, FEATURE_BRANCH_PREFIX);
		typeInto(InitDialog_releaseBranchPrefix, RELEASE_BRANCH_PREFIX);
		typeInto(InitDialog_hotfixBranchPrefix, HOTFIX_BRANCH_PREFIX);
		typeInto(InitDialog_versionTagPrefix, VERSION_TAG_PREFIX);

		ok.click();
	}

	private void clickInit() {
		final SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		final String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("TeamGitFlowInit.name", false, Activator.getDefault().getBundle()) };
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				ContextMenuHelper.clickContextMenuSync(projectExplorerTree,
						menuPath);
			}
		});
	}

	private TestRepository createAndShare(String projectName) throws Exception {
		IProgressMonitor progressMonitor = new NullProgressMonitor();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		project.create(progressMonitor);
		project.open(progressMonitor);
		TestUtil.waitForJobs(50, 5000);

		File gitDir = new File(project.getProject().getLocationURI().getPath(), Constants.DOT_GIT);
		TestRepository testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());
		TestUtil.waitForJobs(50, 5000);

		return testRepository;
	}

	private void typeInto(String textLabel, String textInput) {
		SWTBotText developText = bot.textWithLabel(textLabel);
		developText.selectAll();
		developText.setText(textInput);
	}
}
