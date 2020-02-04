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
package org.eclipse.egit.ui.internal.difftool;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.diffmerge.DiffToolMode;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for using an external diff tool.
 */
public class ExternalDiffToolUiTest extends LocalRepositoryTestCase {

	private static final String EGIT_UI_PLUGIN_ID = "org.eclipse.egit.ui";

	private static final String PER_EXTENSION_PREFERENCE_NAME = "external_diff_tool_per_extension";

	private static final String DIFF_TOOL_MODE_PREFERENCE_NAME = "diff_tool_mode";

	private IEclipsePreferences preferenceNode;

	private int previousDiffToolModePreference;

	private String previousPerExtensionPreference;

	private java.nio.file.Path resultFile;

	private TestRepository testRepository;

	@Before
	public void setUp() throws Exception {
		preferenceNode = DefaultScope.INSTANCE.getNode(EGIT_UI_PLUGIN_ID);
		previousPerExtensionPreference = preferenceNode
				.get(PER_EXTENSION_PREFERENCE_NAME, "");
		previousDiffToolModePreference = preferenceNode.getInt(
				DIFF_TOOL_MODE_PREFERENCE_NAME,
				DiffToolMode.INTERNAL.ordinal());

		FS.FileStoreAttributes.setBackground(false);

		File repositoryFile = createProjectAndCommitToRepository();
		Repository repository = lookupRepository(repositoryFile);
		testRepository = new TestRepository<>(repository);

		resultFile = Files.createTempFile(
				ExternalDiffToolUiTest.class.getSimpleName(), "test_output_file");
	}

	@After
	public void tearDown() throws Exception {
		preferenceNode.put(PER_EXTENSION_PREFERENCE_NAME,
				previousPerExtensionPreference);
		preferenceNode.putInt(DIFF_TOOL_MODE_PREFERENCE_NAME,
				previousDiffToolModePreference);
		Files.delete(resultFile);
	}

	@Test
	public void testExternalDiffTool() throws Exception {
		preferenceNode.putInt(DIFF_TOOL_MODE_PREFERENCE_NAME,
				DiffToolMode.EXTERNAL_FOR_TYPE.ordinal());
		String command = "echo test_command local=\"$LOCAL\" remote=\"$REMOTE\" > "
				+ resultFile.toAbsolutePath().toString();
		preferenceNode.put(PER_EXTENSION_PREFERENCE_NAME, "txt," + command);

		IPath path = new Path(PROJ1).append("folder/test.txt");
		testRepository.branch("stable").commit().add(path.toString(), "stable")
				.create();
		touchAndSubmit("master", "master");
		MergeOperation mergeOp = new MergeOperation(
				testRepository.getRepository(), "stable");
		mergeOp.execute(null);
		MergeResult mergeResult = mergeOp.getResult();
		assertThat(mergeResult.getMergeStatus(), is(MergeStatus.CONFLICTING));
		assertThat(mergeResult.getConflicts().keySet(),
				hasItem(path.toString()));

		IndexDiffCache.INSTANCE
				.getIndexDiffCacheEntry(testRepository.getRepository());
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);

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



		WaitForToolOutput waitForToolOutput = new WaitForToolOutput(resultFile);
		bot.waitUntil(waitForToolOutput, 5_000, 250);

		String actualCommandOutput = String.join(System.lineSeparator(),
				waitForToolOutput.commandOutputLines);
		String expectedOutputPattern = "test_command local=.*.txt remote=.*.txt";
		boolean matchingOutput = Pattern.matches(expectedOutputPattern,
				actualCommandOutput);
		assertTrue("Command output doesn't match expected pattern: "
				+ expectedOutputPattern + ", command output: "
				+ actualCommandOutput, matchingOutput);
	}

	private static class WaitForToolOutput implements ICondition {

		final java.nio.file.Path resultFile;

		List<String> commandOutputLines;

		WaitForToolOutput(java.nio.file.Path resultFile) {
			this.resultFile = resultFile;
		}

		@Override
		public boolean test() throws Exception {
			commandOutputLines = Files.readAllLines(resultFile);
			return !commandOutputLines.isEmpty();
		}

		@Override
		public void init(SWTBot swtBot) {
			// nothing to initialize
		}

		@Override
		public String getFailureMessage() {
			return "timeout occurred while waiting for external diff tool";
		}

	}
}