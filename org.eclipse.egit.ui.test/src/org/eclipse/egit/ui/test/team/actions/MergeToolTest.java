/*******************************************************************************
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
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

	@Before
	public void setUp() throws Exception {
		File repositoryFile = createProjectAndCommitToRepository();
		FileRepository repository = lookupRepository(repositoryFile);
		testRepository = new TestRepository<FileRepository>(repository);
	}

	@After
	public void tearDown() throws Exception {
		deleteAllProjects();
		shutDownRepositories();
	}

	@Test
	public void useHeadOptionShouldCauseFileToNotHaveConflictMarkers()
			throws Exception {
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

		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		SWTBotTree packageExplorer = bot
				.viewById("org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		SWTBotTreeItem project1 = getProjectItem(packageExplorer, PROJ1)
				.select();

		SWTBotTreeItem folderNode = project1.expand().getNode(FOLDER);
		SWTBotTreeItem fileNode = folderNode.expand().getNode(FILE1);
		fileNode.select();
		ContextMenuHelper.clickContextMenu(packageExplorer,
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("MergeToolAction.label"));

		SWTBotShell shell = bot.shell(UIText.MergeModeDialog_DialogTitle)
				.activate();
		shell.bot().radio(UIText.MergeModeDialog_MergeMode_2_Label).click();
		shell.bot().button(IDialogConstants.OK_LABEL).click();

		SWTBotEditor editor = bot.editorById("org.eclipse.compare.CompareEditor");
		SWTBotStyledText styledText = editor.bot().styledText(0);
		bot.waitUntil(Conditions.widgetIsEnabled(styledText));

		String text = styledText.getText();
		assertThat(text, is("master"));
	}
}
