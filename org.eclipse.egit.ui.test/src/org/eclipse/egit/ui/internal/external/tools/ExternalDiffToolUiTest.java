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

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.diffmerge.DiffToolMode;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for using an external diff tool.
 */
public class ExternalDiffToolUiTest extends ExternalToolUiTestCase {

	private int previousDiffToolModePreference;

	private String previousPerExtensionPreference;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		previousPerExtensionPreference = preferenceNode
				.get(UIPreferences.EXTERNAL_DIFF_TOOL_FOR_EXTENSION, "");
		previousDiffToolModePreference = preferenceNode.getInt(
				UIPreferences.DIFF_TOOL_MODE, DiffToolMode.INTERNAL.ordinal());
	}

	@Override
	@After
	public void tearDown() throws Exception {
		try {
			preferenceNode.put(UIPreferences.EXTERNAL_DIFF_TOOL_FOR_EXTENSION,
					previousPerExtensionPreference);
			preferenceNode.putInt(UIPreferences.DIFF_TOOL_MODE,
					previousDiffToolModePreference);
		} finally {
			super.tearDown();
		}
	}

	@Test
	public void testExternalDiffTool() throws Exception {
		assumePosixPlatform();

		preferenceNode.putInt(UIPreferences.DIFF_TOOL_MODE,
				DiffToolMode.EXTERNAL_FOR_TYPE.ordinal());
		String command = "(echo test_command local=\"$LOCAL\" remote=\"$REMOTE\" > "
				+ resultFile.toAbsolutePath().toString() + ")";
		preferenceNode.put(UIPreferences.EXTERNAL_DIFF_TOOL_FOR_EXTENSION,
				"txt," + command);

		createMergeConflict();
		triggerCompareWithPreviousAction();

		List<String> commandOutputLines = waitForToolOutput();

		String actualCommandOutput = String.join(System.lineSeparator(),
				commandOutputLines);
		String expectedOutputPattern = "test_command local=.*.txt remote=.*.txt";
		boolean matchingOutput = Pattern.matches(expectedOutputPattern,
				actualCommandOutput);
		assertTrue("Command output doesn't match expected pattern: "
				+ expectedOutputPattern + ", command output: "
				+ actualCommandOutput, matchingOutput);
	}

	private void triggerCompareWithPreviousAction() {
		SWTBotTree packageExplorer = TestUtil.getExplorerTree();
		SWTBotTreeItem project1 = getProjectItem(packageExplorer, PROJ1)
				.select();

		SWTBotTreeItem folderNode = TestUtil.expandAndWait(project1)
				.getNode(FOLDER);
		SWTBotTreeItem fileNode = TestUtil.expandAndWait(folderNode)
				.getNode(FILE1);
		fileNode.select();
		ContextMenuHelper.clickContextMenu(packageExplorer,
				util.getPluginLocalizedValue("CompareWithMenu.label"),
				util.getPluginLocalizedValue(
						"CompareWithPreviousAction.label"));
	}
}