/*******************************************************************************
 * Copyright (C) 2014, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test.symlinks;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.ui.common.CommitDialogTester;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator.FileEntry;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class SymlinksUITest extends LocalRepositoryTestCase {

	private File repositoryFile;

	private static final String A = "a";

	private static final String B = "b";

	private static final String BRANCH_1 = "branch_1";

	@Before
	public void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);
		TestUtil.configureTestCommitterAsUser(repo);
	}

	@Before
	public void beforeMethod() {
		// If this assumption fails the tests are skipped. When running on a
		// filesystem not supporting symlinks I don't want this tests
		org.junit.Assume.assumeTrue(FS.DETECTED.supportsSymlinks());
	}

	/**
	 * Steps: 1.Add symlink 'a' 2.Commit 3.Create branch '1' 4.Replace symlink
	 * 'a' by file 'a' 5.Commit 6.Checkout branch '1'
	 *
	 * The working tree should contains 'a' with FileMode.SYMLINK after the
	 * checkout.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSymlinkThenFile() throws Exception {

		Repository repo = lookupRepository(repositoryFile);
		// 1.Add file 'b' & symlink 'a'
		IProject firstProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		testUtils.addFileToProject(firstProject, B,
				"hello, world b");
		File symlinkA = new File(firstProject.getLocation().toOSString(), A);
		FileUtils.createSymLink(symlinkA, B);
		firstProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// 2.Commit
		CommitDialogTester commitDialog = CommitDialogTester
				.openCommitDialog(PROJ1);
		commitDialog.setCommitMessage("add file b and symlink a");
		commitDialog.getBot()
				.toolbarButtonWithTooltip(UIText.CommitDialog_SelectAll)
				.click();
		commitDialog.commit();

		// 3.Create branch '1'
		SWTBotShell createBranchDialog = openCreateBranchDialog();
		createBranchDialog.bot()
				.textWithLabel(UIText.CreateBranchPage_BranchNameLabel)
				.setText(BRANCH_1);
		createBranchDialog.bot()
				.checkBox(UIText.CreateBranchPage_CheckoutButton).deselect();
		createBranchDialog.bot().button(IDialogConstants.FINISH_LABEL).click();

		// 4.Replace symlink 'a' by file 'a'
		symlinkA.delete();
		firstProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		testUtils.addFileToProject(firstProject, A,
				"hello, world a");
		firstProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());

		// 5.Commit
		commitDialog = CommitDialogTester.openCommitDialog(PROJ1);
		commitDialog.setCommitMessage("delete symlink a and add file a");
		commitDialog.getBot()
				.toolbarButtonWithTooltip(UIText.CommitDialog_SelectAll)
				.click();
		commitDialog.commit();

		// 'a' is now a regular file
		FileEntry entry = new FileTreeIterator.FileEntry(new File(firstProject
				.getLocation().toOSString(), A), repo.getFS());
		assertEquals(FileMode.REGULAR_FILE, entry.getMode());

		// 6.Checkout branch '1'
		checkoutBranch(BRANCH_1);

		firstProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());

		// 'a' is back to a symlink
		entry = new FileTreeIterator.FileEntry(new File(firstProject
				.getLocation().toOSString(), A), repo.getFS());
		assertEquals(FileMode.SYMLINK, entry.getMode());
	}

	private SWTBotShell openCreateBranchDialog() {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("SwitchToMenu.label"),
				UIText.SwitchToMenu_NewBranchMenuLabel };
		ContextMenuHelper.clickContextMenu(projectExplorerTree, menuPath);
		SWTBotShell dialog = bot
				.shell(UIText.CreateBranchWizard_NewBranchTitle);
		return dialog;
	}

	private void checkoutBranch(String branchName) {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("SwitchToMenu.label"), branchName };
		ContextMenuHelper.clickContextMenu(projectExplorerTree, menuPath);
	}
}
