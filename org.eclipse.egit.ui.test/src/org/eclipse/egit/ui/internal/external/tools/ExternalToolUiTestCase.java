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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS_POSIX;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;

/**
 * Base test case for using an external diff and merge tools via the UI.
 */
public abstract class ExternalToolUiTestCase extends LocalRepositoryTestCase {

	private static final String EGIT_UI_PLUGIN_ID = "org.eclipse.egit.ui";

	protected IEclipsePreferences preferenceNode;

	protected java.nio.file.Path resultFile;

	protected TestRepository testRepository;

	private List<java.nio.file.Path> createdFiles;

	@Before
	public void setUp() throws Exception {
		FS.FileStoreAttributes.setBackground(false);

		preferenceNode = DefaultScope.INSTANCE.getNode(EGIT_UI_PLUGIN_ID);

		File repositoryFile = createProjectAndCommitToRepository();
		Repository repository = lookupRepository(repositoryFile);
		testRepository = new TestRepository<>(repository);

		resultFile = Files.createTempFile(
				ExternalToolUiTestCase.class.getSimpleName(), "test_output_file");

		createdFiles = new ArrayList<>();
		createdFiles.add(resultFile);
	}

	@After
	public void tearDown() throws Exception {
		for (java.nio.file.Path createdFile : createdFiles) {
			Files.deleteIfExists(createdFile);
		}
	}

	protected void clearResultFile() throws Exception {
		Files.write(resultFile, new byte[] {});
	}

	protected List<String> waitForToolOutput() {
		WaitForToolOutput waitForToolOutput = new WaitForToolOutput(resultFile);
		bot.waitUntil(waitForToolOutput, 5_000, 250);
		List<String> commandOutputLines = waitForToolOutput.commandOutputLines;
		return commandOutputLines;
	}

	protected void configureTools(
			BiConsumer<String, StoredConfig> configureDefaultTool,
			BiConsumer<String, StoredConfig> configureToolSubsection,
			String defaultToolName, String... extraToolNames) throws Exception {
		StoredConfig config = testRepository.getRepository().getConfig();
		config.clear();

		configureDefaultTool.accept(defaultToolName, config);

		List<String> toolNames = new ArrayList<>();
		toolNames.add(defaultToolName);
		if (extraToolNames != null) {
			toolNames.addAll(Arrays.asList(extraToolNames));
		}

		for (String toolName : toolNames) {
			configureToolSubsection.accept(toolName, config);
		}

		config.save();
	}

	protected void createMergeConflict() throws Exception {
		IPath path = new Path(PROJ1).append(FOLDER).append(FILE1);
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
	}

	protected void writeFolderGitAttributes(String gitAttributesContents)
			throws IOException {
		writeProjectFile(FOLDER + "/.gitattributes", gitAttributesContents);
	}

	protected void writeProjectFile(String projectFilePath,
			String gitAttributesContents) throws IOException {
		java.nio.file.Path path = getProjectFilePath(projectFilePath);
		Files.write(path, gitAttributesContents.getBytes(),
				StandardOpenOption.CREATE_NEW);
		createdFiles.add(path);
	}

	protected java.nio.file.Path getProjectFilePath(String path) {
		IPath workspacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1).getFile(new Path(path)).getLocation();
		java.nio.file.Path filePath = Paths
				.get(workspacePath.toFile().getAbsolutePath());
		return filePath;
	}

	protected static void assumePosixPlatform() {
		Assume.assumeTrue("This test can run only in Linux tests",
				FS.DETECTED instanceof FS_POSIX);
	}

	protected static class WaitForToolOutput implements ICondition {

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