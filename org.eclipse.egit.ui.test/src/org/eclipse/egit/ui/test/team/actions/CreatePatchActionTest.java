/*******************************************************************************
 * Copyright (C) 2012, 2013 IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.ui.common.CreatePatchWizard;
import org.eclipse.egit.ui.common.CreatePatchWizard.NoChangesPopup;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.IO;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.ui.PlatformUI;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the Team->Create Patch action
 */
public class CreatePatchActionTest extends LocalRepositoryTestCase {

	private static final String PATCH_FILE = "test.patch";

	private static final String EXPECTED_PATCH_CONTENT = //
	"diff --git a/GeneralProject/folder/test.txt b/GeneralProject/folder/test.txt\n"
			+ "index e256dbb..d070357 100644\n"
			+ "--- a/GeneralProject/folder/test.txt\n"
			+ "+++ b/GeneralProject/folder/test.txt\n"
			+ "@@ -1 +1 @@\n"
			+ "-oldContent\n"
			+ "\\ No newline at end of file\n"
			+ "+newContent\n" //
			+ "\\ No newline at end of file\n";

	private static final String EXPECTED_WORKSPACE_PATCH_CONTENT = //
	"### Eclipse Workspace Patch 1.0\n" //
			+ "#P "	+ PROJ1	+ "\n"
			+ "diff --git folder/test.txt folder/test.txt\n" //
			+ "index e256dbb..d070357 100644\n" //
			+ "--- folder/test.txt\n" //
			+ "+++ folder/test.txt\n" //
			+ "@@ -1 +1 @@\n" //
			+ "-oldContent\n" //
			+ "\\ No newline at end of file\n" //
			+ "+newContent\n" //
			+ "\\ No newline at end of file\n"
			+ "diff --git folder/test2.txt folder/test2.txt\n"
			+ "deleted file mode 100644\n" //
			+ "index 8f4e8d3..0000000\n" //
			+ "--- folder/test2.txt\n" //
			+ "+++ /dev/null\n" //
			+ "@@ -1 +0,0 @@\n" //
			+ "-Some more content\n" //
			+ "\\ No newline at end of file\n" //
			+ "#P " + PROJ2 + "\n" //
			+ "diff --git test.txt test.txt\n" //
			+ "new file mode 100644\n" //
			+ "index 0000000..dbe9dba\n" //
			+ "--- /dev/null\n" //
			+ "+++ test.txt\n" //
			+ "@@ -0,0 +1 @@\n" //
			+ "+Hello, world\n" //
			+ "\\ No newline at end of file\n";

	@Before
	public void setup() throws Exception {
		createProjectAndCommitToRepository();

		IFile[] commitables = getAllFiles();
		CommitOperation cop = new CommitOperation(commitables,
				Arrays.asList(commitables), TestUtil.TESTAUTHOR,
				TestUtil.TESTCOMMITTER, "Initial commit");
		cop.setAmending(true);
		cop.execute(null);
	}

	private static IFile[] getAllFiles() {
		IProject firstProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		IProject secondProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ2);

		IFolder folder = firstProject.getFolder(FOLDER);
		IFile textFile = folder.getFile(FILE1);
		IFile textFile2 = folder.getFile(FILE2);

		IFolder secondfolder = secondProject.getFolder(FOLDER);
		IFile secondtextFile = secondfolder.getFile(FILE1);
		IFile secondtextFile2 = secondfolder.getFile(FILE2);

		return new IFile[] { firstProject.getFile(".project"), textFile,
				textFile2, secondProject.getFile(".project"), secondtextFile,
				secondtextFile2 };
	}

	@Test
	public void testNoChanges() throws Exception {
		CreatePatchWizard.openWizard(PROJ1);
		NoChangesPopup popup = new NoChangesPopup(
				bot.shell(UIText.GitCreatePatchAction_cannotCreatePatch));
		popup.cancelPopup();
	}

	@Test
	public void testNoChangesInSelection() throws Exception {
		IFile fileToStage = touch(PROJ1, "folder/test.txt", "new content in "
				+ PROJ1);
		stage(fileToStage);
		touch(PROJ2, "folder/test.txt", "new content in " + PROJ2);

		CreatePatchWizard.openWizard(PROJ1);

		NoChangesPopup popup = new NoChangesPopup(
				bot.shell(UIText.GitCreatePatchAction_cannotCreatePatch));
		popup.cancelPopup();
	}

	@Test
	public void testClipboard() throws Exception {
		touchAndSubmit("oldContent", null);
		touch("newContent");

		CreatePatchWizard createPatchWizard = openCreatePatchWizard();
		LocationPage locationPage = createPatchWizard.getLocationPage();
		locationPage.selectClipboard();
		createPatchWizard.finishWithNoneFormat();

		bot.waitUntil(Conditions.shellCloses(createPatchWizard.getShell()));

		assertClipboard(EXPECTED_PATCH_CONTENT);
	}

	@Test
	public void testFilesystem() throws Exception {
		touchAndSubmit("oldContent", null);
		touch("newContent");

		CreatePatchWizard createPatchWizard = openCreatePatchWizard();
		LocationPage locationPage = createPatchWizard.getLocationPage();
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		File patch = new File(project.getLocation().toFile(), PATCH_FILE);
		locationPage.selectFilesystem(patch.toString());
		createPatchWizard.finishWithNoneFormat();

		bot.waitUntil(Conditions.shellCloses(createPatchWizard.getShell()));

		byte[] bytes = IO.readFully(patch);
		String patchContent = new String(bytes, "UTF-8");

		assertEquals(EXPECTED_PATCH_CONTENT, patchContent);
	}

	@Test
	public void testWorkspace() throws Exception {
		touchAndSubmit("oldContent", null);
		touch("newContent");

		CreatePatchWizard createPatchWizard = openCreatePatchWizard();
		LocationPage locationPage = createPatchWizard.getLocationPage();
		locationPage.selectWorkspace("/" + PROJ1 + "/" + PATCH_FILE);
		createPatchWizard.finishWithNoneFormat();

		bot.waitUntil(Conditions.shellCloses(createPatchWizard.getShell()));

		IFile patch = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1).getFile(PATCH_FILE);
		byte[] bytes = IO.readFully(patch.getLocation().toFile());
		String patchContent = new String(bytes, patch.getCharset());

		assertEquals(EXPECTED_PATCH_CONTENT, patchContent);
	}

	@Test
	public void testWorkspaceHeader() throws Exception {
		touchAndSubmit("oldContent", null);
		touch("newContent");
		URI fileLocationUri = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1).getFolder(FOLDER).getFile(FILE2)
				.getLocationURI();
		FileUtils.delete(new File(fileLocationUri));
		IProject secondProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ2);
		IFile newFile = secondProject.getFile(FILE1);
		newFile.create(
				new ByteArrayInputStream("Hello, world".getBytes(secondProject
						.getDefaultCharset())), false, null);

		CreatePatchWizard createPatchWizard = openCreatePatchWizard();
		LocationPage locationPage = createPatchWizard.getLocationPage();
		locationPage.selectWorkspace("/" + PROJ1 + "/" + PATCH_FILE);
		OptionsPage optionsPage = locationPage.nextToOptionsPage();
		optionsPage.setFormat(CoreText.DiffHeaderFormat_Workspace);
		createPatchWizard.finish();

		bot.waitUntil(Conditions.shellCloses(createPatchWizard.getShell()));

		IFile patch = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1).getFile(PATCH_FILE);
		byte[] bytes = IO.readFully(patch.getLocation().toFile());
		String patchContent = new String(bytes, patch.getCharset());

		assertEquals(EXPECTED_WORKSPACE_PATCH_CONTENT, patchContent);
	}

	private CreatePatchWizard openCreatePatchWizard() throws Exception {
		CreatePatchWizard.openWizard(PROJ1, PROJ2);
		SWTBotShell shell = bot
				.shell(UIText.GitCreatePatchWizard_CreatePatchTitle);
		return new CreatePatchWizard(shell);
	}

	private static void assertClipboard(final String expected) {
		final String[] value = { null };
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				Clipboard clp = new Clipboard(Display.getCurrent());
				value[0] = (String) clp.getContents(TextTransfer.getInstance());
				clp.dispose();
			}
		});
		assertEquals(expected, value[0]);
	}
}
