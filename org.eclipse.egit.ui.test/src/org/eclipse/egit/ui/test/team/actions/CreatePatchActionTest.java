/*******************************************************************************
 * Copyright (C) 2012, Tomasz Zarna <Tomasz.Zarna@pl.ibm.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.CreatePatchWizard;
import org.eclipse.egit.ui.common.CreatePatchWizard.NoChangesPopup;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the Team->Create Patch action
 */
public class CreatePatchActionTest extends LocalRepositoryTestCase {

	private static SWTBotPerspective perspective;

	@BeforeClass
	public static void setup() throws Exception {
		perspective = bot.activePerspective();
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();
		createProjectAndCommitToRepository();
		waitInUI();
	}

	@AfterClass
	public static void shutdown() {
		perspective.activate();
	}

	@Test
	public void testNoChanges() throws Exception {
		// commit all files
		IFile[] commitables = getAllFiles();
		ArrayList<IFile> untracked = new ArrayList<IFile>();
		untracked.addAll(Arrays.asList(commitables));
		CommitOperation op = new CommitOperation(commitables, untracked,
				TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER, "Initial commit");
		op.setAmending(true);
		op.execute(null);

		CreatePatchWizard.openWizard(PROJ1);
		NoChangesPopup popup = new NoChangesPopup(
				bot.shell(UIText.GitCreatePatchAction_cannotCreatePatch));
		popup.cancelPopup();
	}

	private IFile[] getAllFiles() {
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
	public void testClipboard() throws Exception {
		touchAndSubmit("oldContent", null);
		touch("newContent");
		waitInUI();
		CreatePatchWizard createPatchWizard = openCreatePatchWizard();
		createPatchWizard.finish();
		waitInUI();

		StringBuilder sb = new StringBuilder();
		sb.append(
				"diff --git a/GeneralProject/folder/test.txt b/GeneralProject/folder/test.txt")
				.append("\n");
		sb.append("index e256dbb..d070357 100644").append("\n");
		sb.append("--- a/GeneralProject/folder/test.txt").append("\n");
		sb.append("+++ b/GeneralProject/folder/test.txt").append("\n");
		sb.append("@@ -1 +1 @@").append("\n");
		sb.append("-oldContent").append("\n");
		sb.append("\\ No newline at end of file").append("\n");
		sb.append("+newContent").append("\n");
		sb.append("\\ No newline at end of file");

		assertClipboard(sb.toString());
	}

	private CreatePatchWizard openCreatePatchWizard() throws Exception {
		CreatePatchWizard.openWizard(PROJ1);
		SWTBotShell shell = bot
				.shell(UIText.GitCreatePatchWizard_CreatePatchTitle);
		return new CreatePatchWizard(shell);
	}

	private static void assertClipboard(final String expected) {
		final String[] value = { null };
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Clipboard clp = new Clipboard(Display.getCurrent());
				value[0] = (String) clp.getContents(TextTransfer.getInstance());
				clp.dispose();
			}
		});
		assertEquals(expected, value[0]);
	}
}
