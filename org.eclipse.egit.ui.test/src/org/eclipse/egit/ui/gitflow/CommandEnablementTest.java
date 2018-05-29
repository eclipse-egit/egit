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

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellIsActive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.core.commands.Command;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotCommand;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Gitflow plugin.xml expressions, property testers, AdapterFactories, etc.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CommandEnablementTest extends AbstractGitflowHandlerTest {

	private static final String FEATURE_START_CMD = "org.eclipse.egit.gitflow.ui.command.featureStart";
	private static final String FEATURE_FINISH_CMD = "org.eclipse.egit.gitflow.ui.command.featureFinish";

	private static final String RELEASE_START_CMD = "org.eclipse.egit.gitflow.ui.command.releaseStart";
	private static final String RELEASE_END_CMD = "org.eclipse.egit.gitflow.ui.command.releaseFinish";

	private static final String HOTFIX_START_CMD = "org.eclipse.egit.gitflow.ui.command.hotfixStart";
	private static final String HOTFIX_END_CMD = "org.eclipse.egit.gitflow.ui.command.hotfixFinish";

	@Test
	public void testPackageExplorerStartFinishEnablement() throws Exception {
		selectPackageExplorerItem(0);
		assertAllCommandsDisabled();
		init();
		assertAllCommandsEnabledAndExecutable();
	}

	@Test
	public void testRepositoryViewStartFinishEnablement() throws Exception {
		initRepositoriesView();
		selectRepositoryInView(0);
		assertAllCommandsDisabled();
		init();
		assertAllCommandsEnabledAndExecutable();
	}

	private void selectPackageExplorerItem(int index) {
		SWTBotTree explorerTree = TestUtil.getExplorerTree();
		explorerTree.select(index);
	}

	private void selectRepositoryInView(int index) {
		SWTBotTree explorerTree = getRepositoryTree();
		explorerTree.select(index);
	}

	private void assertAllCommandsEnabledAndExecutable() throws Exception {
		testCommand(FEATURE_START_CMD, FEATURE_FINISH_CMD, UIText.FeatureStartHandler_provideFeatureName);
		testCommand(RELEASE_START_CMD, RELEASE_END_CMD, UIText.ReleaseStartHandler_provideReleaseName);
		testCommand(HOTFIX_START_CMD, HOTFIX_END_CMD, UIText.HotfixStartHandler_provideHotfixName);
	}


	private void assertAllCommandsDisabled() {
		assertCommandEnablement(FEATURE_START_CMD, false);
		assertCommandEnablement(RELEASE_START_CMD, false);
		assertCommandEnablement(HOTFIX_START_CMD, false);
	}

	private void initRepositoriesView() throws IOException {
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
		repository = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().lookupRepository(repositoryFile);
	}

	public static SWTBotTree getRepositoryTree() {
		SWTBotView view = TestUtil.showView(RepositoriesView.VIEW_ID);
		return view.bot().tree();
	}

	private void testCommand(String startCommandId, String finishCommandId, String dialogTitle) throws Exception {
		assertCommandEnablement(startCommandId, true);
		assertCommandOpensDialog(startCommandId, dialogTitle);
		assertCommandEnablement(finishCommandId, false);
	}

	private void assertCommandOpensDialog(String startCommandId,
			String dialogTitle) throws Exception {
		runCommand(startCommandId);
		bot.waitUntil(shellIsActive(dialogTitle));
		bot.button("Cancel").click();
	}


	private void runCommand(String commandId) throws Exception {
		final Command command = getCommandService().getCommand(commandId);
		SWTBotCommand swtBotCommand = new SWTBotCommand(command);
		swtBotCommand.click();
	}

	@SuppressWarnings("boxing")
	private void assertCommandEnablement(String id, boolean enablement) {
		ICommandService service = getCommandService();
		Command command = service.getCommand(id);
		assertTrue(command.isDefined());
		assertTrue(command.isHandled());
		assertEquals(enablement, command.isEnabled());
	}

	private ICommandService getCommandService() {
		return PlatformUI.getWorkbench().getService(ICommandService.class);
	}
}
