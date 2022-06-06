/*******************************************************************************
 * Copyright (C) 2022 Simeon Andreev and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.external.tools;

import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_CMD;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_PROMPT;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_TOOL;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_TRUST_EXIT_CODE;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_MERGETOOL_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_MERGE_SECTION;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.diffmerge.MergeToolMode;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for using an external merge tool.
 */
public class ExternalMergeToolUiTest extends ExternalToolUiTestCase {

	private int previousMergeToolModePreference;

	private String previousCustomToolPreference;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		previousCustomToolPreference = preferenceNode
				.get(UIPreferences.MERGE_TOOL_CUSTOM, "");
		previousMergeToolModePreference = preferenceNode.getInt(
				UIPreferences.MERGE_TOOL_MODE,
				MergeToolMode.INTERNAL.ordinal());
	}

	@Override
	@After
	public void tearDown() throws Exception {
		try {
			preferenceNode.put(UIPreferences.MERGE_TOOL_CUSTOM,
					previousCustomToolPreference);
			preferenceNode.putInt(UIPreferences.MERGE_TOOL_MODE,
					previousMergeToolModePreference);
		} finally {
			super.tearDown();
		}
	}

	@Test
	public void testExternalMergeTool() throws Exception {
		assumePosixPlatform();

		String toolName = "custom_tool";
		configureEchoTool(toolName);
		preferenceNode.putInt(UIPreferences.MERGE_TOOL_MODE,
				MergeToolMode.EXTERNAL.ordinal());
		preferenceNode.put(UIPreferences.MERGE_TOOL_CUSTOM, toolName);

		createMergeConflict();
		triggerMergeToolAction();

		List<String> commandOutputLines = waitForToolOutput();

		String actualCommandOutput = String.join(System.lineSeparator(),
				commandOutputLines);
		String expectedOutputPattern = "custom_tool .*.txt";
		boolean matchingOutput = Pattern.matches(expectedOutputPattern,
				actualCommandOutput);
		assertTrue("Command output doesn't match expected pattern: "
				+ expectedOutputPattern + ", command output: "
				+ actualCommandOutput, matchingOutput);
	}

	@Test
	public void testExternalMergeToolGitConfig() throws Exception {
		assumePosixPlatform();

		createMergeConflict();

		// run with differently configured tools, to ensure the git config is
		// not cached improperly
		int runs = 2;
		for (int i = 0; i < runs; ++i) {
			clearResultFile();

			String toolName = "custom_tool" + i;
			configureEchoTool(toolName);
			preferenceNode.putInt(UIPreferences.MERGE_TOOL_MODE,
					MergeToolMode.GIT_CONFIG.ordinal());
			preferenceNode.put(UIPreferences.MERGE_TOOL_CUSTOM, toolName);

			triggerMergeToolAction();

			List<String> commandOutputLines = waitForToolOutput();

			String actualCommandOutput = String.join(System.lineSeparator(),
					commandOutputLines);
			String expectedOutputPattern = toolName + " .*.txt";
			boolean matchingOutput = Pattern.matches(expectedOutputPattern,
					actualCommandOutput);
			assertTrue("Command output doesn't match expected pattern: "
					+ expectedOutputPattern + ", command output: "
					+ actualCommandOutput, matchingOutput);
		}
	}

	private void triggerMergeToolAction() {
		SWTBotTree packageExplorer = TestUtil.getExplorerTree();
		SWTBotTreeItem project1 = getProjectItem(packageExplorer, PROJ1)
				.select();

		SWTBotTreeItem folderNode = TestUtil.expandAndWait(project1)
				.getNode(FOLDER);
		SWTBotTreeItem fileNode = TestUtil.expandAndWait(folderNode)
				.getNode(FILE1);
		fileNode.select();
		ContextMenuHelper.clickContextMenu(packageExplorer,
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("MergeToolAction.label"));
	}

	private void configureEchoTool(String toolName) throws Exception {
		StoredConfig config = testRepository.getRepository().getConfig();
		config.clear();
		// the default merge tool is configured without a subsection
		String subsection = null;
		config.setString(CONFIG_MERGE_SECTION, subsection, CONFIG_KEY_TOOL,
				toolName);

		String command = getEchoCommand(toolName);

		config.setString(CONFIG_MERGETOOL_SECTION, toolName, CONFIG_KEY_CMD,
				command);
		config.setString(CONFIG_MERGETOOL_SECTION, toolName,
				CONFIG_KEY_TRUST_EXIT_CODE, String.valueOf(Boolean.TRUE));
		/*
		 * prevent prompts as we are running in tests and there is no user to
		 * interact with on the command line
		 */
		config.setString(CONFIG_MERGETOOL_SECTION, toolName, CONFIG_KEY_PROMPT,
				String.valueOf(false));
		config.save();
	}

	private String getEchoCommand(String toolName) {
		/*
		 * Use 'MERGED' placeholder, as both 'LOCAL' and 'REMOTE' will be
		 * replaced with full paths to a temporary file during some of the tests.
		 *
		 * Exit with non-zero code, to prevent a native dialog from being shown.
		 */
		return "(echo " + toolName + " \"$MERGED\" > "
				+ resultFile.toAbsolutePath().toString() + "; exit 1)";
	}
}