/*******************************************************************************
 * Copyright (C) 2012, 2021 Robin Stocker <robin@nibor.org> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.op.CherryPickOperation;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.common.CompareEditorTester;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the "Merge Tool" action on conflicting files.
 */
public class MergeToolTest extends LocalRepositoryTestCase {

	private TestRepository testRepository;

	private int mergeMode;

	@Before
	public void setUp() throws Exception {
		File repositoryFile = createProjectAndCommitToRepository();
		Repository repository = lookupRepository(repositoryFile);
		testRepository = new TestRepository<>(repository);
		mergeMode = Activator.getDefault()
				.getPreferenceStore().getInt(UIPreferences.MERGE_MODE);
	}

	@After
	public void resetMergeMode() throws Exception {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.MERGE_MODE, mergeMode);
	}

	@Test
	public void useHeadOptionShouldCauseFileToNotHaveConflictMarkers()
			throws Exception {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.MERGE_MODE, 2);
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

		IndexDiffCache cache = IndexDiffCache.getInstance();
		cache.getIndexDiffCacheEntry(testRepository.getRepository());
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
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("MergeToolAction.label"));

		CompareEditorTester compareEditor = CompareEditorTester
				.forTitleContaining("Merging");

		String text = compareEditor.getLeftEditor().getText();
		assertThat(text, is("master"));
	}

	@Test
	public void useHeadOptionOpenedAgainShouldHaveEdits() throws Exception {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.MERGE_MODE, 2);
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

		IndexDiffCache cache = IndexDiffCache.getInstance();
		cache.getIndexDiffCacheEntry(testRepository.getRepository());
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
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("MergeToolAction.label"));

		CompareEditorTester compareEditor = CompareEditorTester
				.forTitleContaining("Merging");

		String text = compareEditor.getLeftEditor().getText();
		assertThat(text, is("master"));
		compareEditor.getLeftEditor().setText("master edited");
		assertTrue(compareEditor.isDirty());
		compareEditor.save();
		compareEditor.close();
		TestUtil.navigateTo(packageExplorer, PROJ1, FOLDER, FILE1).select();
		ContextMenuHelper.clickContextMenu(packageExplorer,
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("MergeToolAction.label"));

		compareEditor = CompareEditorTester.forTitleContaining("Merging");

		text = compareEditor.getLeftEditor().getText();
		assertThat(text, is("master edited"));
	}

	@Test
	public void verifyCherrypickBase() throws Exception {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.MERGE_MODE, 2);
		testRepository.branch("stable").commit()
				.add(FILE1_PATH, "stable").create();
		touchAndSubmit("master", "master");
		RevCommit stableTip = testRepository.branch("stable").commit()
				.add(FILE1_PATH, "stable 2").create();
		CherryPickOperation op = new CherryPickOperation(
				testRepository.getRepository(), stableTip);
		op.execute(null);
		CherryPickResult result = op.getResult();

		assertThat(result.getStatus(), is(CherryPickStatus.CONFLICTING));

		IndexDiffCache cache = IndexDiffCache.getInstance();
		cache.getIndexDiffCacheEntry(testRepository.getRepository());
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		SWTBotTree packageExplorer = TestUtil.getExplorerTree();
		TestUtil.navigateTo(packageExplorer, PROJ1, FOLDER, FILE1).select();
		ContextMenuHelper.clickContextMenu(packageExplorer,
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("MergeToolAction.label"));

		CompareEditorTester compareEditor = CompareEditorTester
				.forTitleContaining("Merging");

		String text = compareEditor.getLeftEditor().getText();
		assertThat(text, is("master"));
		text = compareEditor.getRightEditor().getText();
		assertThat(text, is("stable 2"));
		text = compareEditor.getAncestorEditor().getText();
		// If the common ancestor was used the content would be "Hello, world"
		assertThat(text, is("stable"));
	}

	@Test
	public void conflictUnderneathIgnoredFolder() throws Exception {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.MERGE_MODE, 2);
		try (Git git = new Git(testRepository.getRepository())) {
			// The "canonical" file is already inside a folder
			touch(PROJ1, ".gitignore", '/' + FOLDER);
			git.add().addFilepattern(".gitignore").call();
			git.commit().setMessage("Ignoring folder");
		}
		testRepository.branch("stable").commit().add(FILE1_PATH, "stable")
				.create();
		touchAndSubmit("master", "master");
		RevCommit stableTip = testRepository.branch("stable").commit()
				.add(FILE1_PATH, "stable 2").create();
		CherryPickOperation op = new CherryPickOperation(
				testRepository.getRepository(), stableTip);
		op.execute(null);
		CherryPickResult result = op.getResult();

		assertThat(result.getStatus(), is(CherryPickStatus.CONFLICTING));

		IndexDiffCache cache = IndexDiffCache.getInstance();
		cache.getIndexDiffCacheEntry(testRepository.getRepository());
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		SWTBotTree packageExplorer = TestUtil.getExplorerTree();
		TestUtil.navigateTo(packageExplorer, PROJ1, FOLDER, FILE1).select();
		ContextMenuHelper.clickContextMenu(packageExplorer,
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("MergeToolAction.label"));

		CompareEditorTester compareEditor = CompareEditorTester
				.forTitleContaining("Merging");

		String text = compareEditor.getLeftEditor().getText();
		assertThat(text, is("master"));
		text = compareEditor.getRightEditor().getText();
		assertThat(text, is("stable 2"));
	}

	@Test
	public void mergedOursVersion() throws Exception {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.MERGE_MODE, 3);
		testRepository.branch("stable").commit().add(FILE1_PATH, "stable")
				.create();
		touchAndSubmit("master", "master");
		RevCommit stableTip = testRepository.branch("stable").commit()
				.add(FILE1_PATH, "stable 2").create();
		CherryPickOperation op = new CherryPickOperation(
				testRepository.getRepository(), stableTip);
		op.execute(null);
		CherryPickResult result = op.getResult();

		assertThat(result.getStatus(), is(CherryPickStatus.CONFLICTING));

		IndexDiffCache cache = IndexDiffCache.getInstance();
		cache.getIndexDiffCacheEntry(testRepository.getRepository());
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// Verify file content.
		assertThat(getTestFileContent(),
				is("<<<<<<< master\nmaster\n=======\nstable 2\n>>>>>>> "
						+ stableTip.abbreviate(7).name() + " \n"));

		SWTBotTree packageExplorer = TestUtil.getExplorerTree();
		TestUtil.navigateTo(packageExplorer, PROJ1, FOLDER, FILE1).select();
		ContextMenuHelper.clickContextMenu(packageExplorer,
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("MergeToolAction.label"));

		CompareEditorTester compareEditor = CompareEditorTester
				.forTitleContaining("Merging");

		String text = compareEditor.getLeftEditor().getText();
		assertThat(text, is("master\n"));
		text = compareEditor.getRightEditor().getText();
		// Change it
		compareEditor.getLeftEditor().setText(text);
		// Save it
		compareEditor.save();
		assertThat(getTestFileContent(), is("stable 2"));
	}

	@Test
	public void mergedOursVersionLongerMarkers() throws Exception {
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.MERGE_MODE, 3);
		testRepository.branch("stable").commit().add(FILE1_PATH, "stable")
				.create();
		touchAndSubmit("master", "master");
		RevCommit stableTip = testRepository.branch("stable").commit()
				.add(FILE1_PATH, "stable 2").create();
		CherryPickOperation op = new CherryPickOperation(
				testRepository.getRepository(), stableTip);
		op.execute(null);
		CherryPickResult result = op.getResult();

		assertThat(result.getStatus(), is(CherryPickStatus.CONFLICTING));

		IndexDiffCache cache = IndexDiffCache.getInstance();
		cache.getIndexDiffCacheEntry(testRepository.getRepository());
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// Verify file content.
		assertThat(getTestFileContent(),
				is("<<<<<<< master\nmaster\n=======\nstable 2\n>>>>>>> "
						+ stableTip.abbreviate(7).name() + " \n"));

		// Pretend we did the merge with longer conflict markers
		touch("<<<<<<<<< master\nmaster\n=========\nstable 2\n>>>>>>>>> "
				+ stableTip.abbreviate(7).name() + " \n");
		touch(PROJ1, ".gitattributes", "test.txt conflict-marker-size=9");

		SWTBotTree packageExplorer = TestUtil.getExplorerTree();
		TestUtil.navigateTo(packageExplorer, PROJ1, FOLDER, FILE1).select();
		ContextMenuHelper.clickContextMenu(packageExplorer,
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("MergeToolAction.label"));

		CompareEditorTester compareEditor = CompareEditorTester
				.forTitleContaining("Merging");

		String text = compareEditor.getLeftEditor().getText();
		assertThat(text, is("master\n"));
		text = compareEditor.getRightEditor().getText();
		// Change it
		compareEditor.getLeftEditor().setText(text);
		// Save it
		compareEditor.save();
		assertThat(getTestFileContent(), is("stable 2"));
	}
}
