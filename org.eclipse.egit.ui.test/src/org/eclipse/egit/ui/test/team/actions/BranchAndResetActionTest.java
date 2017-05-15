/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Chris Aniszczyk <caniszczyk@gmail.com> - tag API changes
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.LocalNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestUtils;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarDropDownButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Branch action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class BranchAndResetActionTest extends LocalRepositoryTestCase {

	private static String LOCAL_BRANCHES;
	private static String TAGS;

	private File repositoryFile;

	@Before
	public void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);

		TagBuilder tag = new TagBuilder();
		tag.setTag("SomeTag");
		tag.setTagger(RawParseUtils.parsePersonIdent(TestUtil.TESTAUTHOR));
		tag.setMessage("I'm just a little tag");
		tag.setObjectId(repo.resolve(repo.getFullBranch()),
				Constants.OBJ_COMMIT);
		TagOperation top = new TagOperation(repo, tag, false);
		top.execute(null);
		touchAndSubmit(null);

		RepositoriesViewLabelProvider provider = GitRepositoriesViewTestUtils
				.createLabelProvider();
		LOCAL_BRANCHES = provider.getText(new LocalNode(new RepositoryNode(
				null, repo), repo));
		TAGS = provider.getText(new TagsNode(new RepositoryNode(null, repo),
				repo));
	}

	@Test
	public void testCheckoutLocalBranches() throws Exception {
		checkoutAndVerify(new String[] { LOCAL_BRANCHES, "stable" });
		checkoutAndVerify(new String[] { LOCAL_BRANCHES, "master" });
	}

	@Test
	public void testCheckoutWithConflicts() throws Exception {
		String charset = ResourcesPlugin.getWorkspace().getRoot().getProject(
				PROJ1).getDefaultCharset();
		try {
			checkoutAndVerify(new String[] { LOCAL_BRANCHES, "stable" });
			ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
					.getFolder(FOLDER).getFile(FILE1).setContents(
							new ByteArrayInputStream("New content"
									.getBytes(charset)), IResource.NONE, null);
			checkout(new String[] { LOCAL_BRANCHES, "master" });
			SWTBotShell showConflicts = bot
					.shell(UIText.BranchResultDialog_CheckoutConflictsTitle);
			assertEquals(FILE1, showConflicts.bot().tree().getAllItems()[0]
					.getItems()[0].getItems()[0].getText());
			showConflicts.close();
		} finally {
			ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
					.getFolder(FOLDER).getFile(FILE1).setContents(
							new ByteArrayInputStream("Hello, world"
									.getBytes(charset)), IResource.NONE, null);
		}
	}

	@Test
	public void testCheckoutWithNonDeleted() throws Exception {
		// we need to check if this file system has problems to
		// delete a file with an open FileInputStrem
		IFile test = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFolder(FOLDER).getFile("temp.txt");
		test.create(new ByteArrayInputStream(new byte[0]), false, null);
		File testFile = new File(test.getLocation().toString());
		assertTrue(testFile.exists());
		FileInputStream fis = new FileInputStream(testFile);
		try {
			FileUtils.delete(testFile);
			return;
		} catch (IOException e) {
			// the test makes sense only if deletion of
			// a file with open stream fails
		} finally {
			fis.close();
			if (testFile.exists())
				FileUtils.delete(testFile);
		}
		final Image folderImage = PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJ_FOLDER);

		final Image projectImage = PlatformUI.getWorkbench().getSharedImages()
				.getImage(SharedImages.IMG_OBJ_PROJECT);
		// checkout stable
		checkoutAndVerify(new String[] { LOCAL_BRANCHES, "stable" });
		// add a file
		IFile toBeDeleted = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1).getFolder(FOLDER).getFile("ToBeDeleted");
		toBeDeleted.create(new ByteArrayInputStream(new byte[0]), false, null);

		ArrayList<IFile> untracked = new ArrayList<IFile>();
		untracked.add(toBeDeleted);
		// commit to stable
		CommitOperation op = new CommitOperation(new IFile[] { toBeDeleted },
				untracked, TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER,
				"Add to stable");
		op.execute(null);

		InputStream is = toBeDeleted.getContents();
		try {
			checkout(new String[] { LOCAL_BRANCHES, "master" });
			final SWTBotShell showUndeleted = bot
					.shell(UIText.NonDeletedFilesDialog_NonDeletedFilesTitle);
			// repo relative path
			assertEquals("ToBeDeleted", showUndeleted.bot().tree()
					.getAllItems()[0].getItems()[0].getItems()[0].getText());
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					assertSame(folderImage, showUndeleted.bot().tree()
							.getAllItems()[0].widget.getImage());
				}
			});

			SWTBotToolbarDropDownButton pathButton = showUndeleted.bot().toolbarDropDownButton();
			pathButton.menuItem(UIText.NonDeletedFilesTree_FileSystemPathsButton).click();
			// see http://www.eclipse.org/forums/index.php/t/159133/ why we need this
			pathButton.pressShortcut(KeyStroke.getInstance("ESC"));
			// fs path
			IPath path = new Path(lookupRepository(repositoryFile)
					.getWorkTree().getPath()).append(PROJ1).append(FOLDER)
					.append("ToBeDeleted");
			SWTBotTreeItem[] items = showUndeleted.bot().tree().getAllItems();
			for (int i = 0; i < path.segmentCount(); i++) {
				boolean found = false;
				String segment = path.segment(i);
				for (SWTBotTreeItem item : items)
					if (item.getText().equals(segment)) {
						found = true;
						items = item.getItems();
					}
				assertTrue(found);
			}
			pathButton.menuItem(UIText.NonDeletedFilesTree_ResourcePathsButton).click();
			// see http://www.eclipse.org/forums/index.php/t/159133/ why we need this
			pathButton.pressShortcut(KeyStroke.getInstance("ESC"));
			// resource path
			assertEquals("ToBeDeleted", showUndeleted.bot().tree()
					.getAllItems()[0].getItems()[0].getItems()[0].getText());
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					assertSame(projectImage, showUndeleted.bot().tree()
							.getAllItems()[0].widget.getImage());
				}
			});

			ICondition treeEmpty = new ICondition() {

				@Override
				public boolean test() throws Exception {
					return showUndeleted.bot().tree().getAllItems().length == 0;
				}

				@Override
				public void init(SWTBot actBot) {
					// nothing
				}

				@Override
				public String getFailureMessage() {
					return "Not deleted";
				}
			};

			showUndeleted.bot().button(
					UIText.NonDeletedFilesDialog_RetryDeleteButton).click();
			try {
				showUndeleted.bot().waitUntil(treeEmpty, 1000, 100);
				fail("Should have failed");
			} catch (TimeoutException e) {
				// expected
			}
			is.close();
			showUndeleted.bot().button(
					UIText.NonDeletedFilesDialog_RetryDeleteButton).click();
			showUndeleted.bot().waitUntil(treeEmpty, 1000, 100);
			showUndeleted.close();
		} finally {
			is.close();
		}
	}

	@Test
	public void testResetToLocalBranch() throws Exception {
		checkoutAndVerify(new String[] { LOCAL_BRANCHES, "stable" });
		String stable = getTestFileContent();
		checkoutAndVerify(new String[] { LOCAL_BRANCHES, "master" });
		String master = getTestFileContent();
		assertFalse(stable.equals(master));
		SWTBotShell resetDialog = openResetDialog();
		SWTBotTreeItem localBranches = TestUtil.expandAndWait(
				resetDialog.bot().tree().getTreeItem(LOCAL_BRANCHES));
		TestUtil.getChildNode(localBranches, "stable").select();
		resetDialog.bot().radio(
				UIText.ResetTargetSelectionDialog_ResetTypeHardButton).click();

		resetDialog.bot().button(UIText.ResetTargetSelectionDialog_ResetButton)
				.click();

		bot.shell(UIText.ResetTargetSelectionDialog_ResetQuestion).bot()
				.button(UIText.CommandConfirmationHardResetDialog_resetButtonLabel)
				.click();

		Job.getJobManager().join(JobFamilies.RESET, null);
		String reset = getTestFileContent();
		assertEquals("Wrong content after reset", stable, reset);
	}

	@Test
	public void testCreateDeleteBranch() throws Exception {
		assertNull(lookupRepository(repositoryFile).resolve("newBranch"));

		SWTBotShell newBranchDialog = openCreateBranchDialog();
		newBranchDialog.bot().textWithId("BranchName").setText("newBranch");
		newBranchDialog.bot().checkBox(UIText.CreateBranchPage_CheckoutButton).deselect();
		newBranchDialog.bot().button(IDialogConstants.FINISH_LABEL).click();

		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		assertNotNull(lookupRepository(repositoryFile).resolve("newBranch"));

		SWTBotShell deleteBranchDialog = openDeleteBranchDialog();
		SWTBotTreeItem localBranches = TestUtil.expandAndWait(
				deleteBranchDialog.bot().tree().getTreeItem(LOCAL_BRANCHES));
		TestUtil.getChildNode(localBranches, "newBranch").select();
		deleteBranchDialog.bot().button(IDialogConstants.OK_LABEL).click();

		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		assertNull(lookupRepository(repositoryFile).resolve("newBranch"));
	}

	@Test
	public void testCreateBranchSelectSource() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		assertNull(repo.resolve("branch-from-tag"));

		SWTBotShell createBranchDialog = openCreateBranchDialog();
		createBranchDialog.bot()
				.button(UIText.CreateBranchPage_SourceSelectButton).click();

		SWTBotShell sourceSelectionDialog = bot
				.shell(UIText.CreateBranchPage_SourceSelectionDialogTitle);
		SWTBotTree tree = sourceSelectionDialog.bot().tree();
		SWTBotTreeItem tags = tree
				.expandNode(UIText.RepositoriesViewLabelProvider_TagsNodeText);
		TestUtil.getChildNode(tags, "SomeTag").select();
		sourceSelectionDialog.bot().button(IDialogConstants.OK_LABEL).click();

		SWTBotStyledText sourceLabel = createBranchDialog.bot().styledText(0);
		assertEquals("SomeTag", sourceLabel.getText());

		createBranchDialog.bot().textWithId("BranchName")
				.setText("branch-from-tag");
		createBranchDialog.bot()
				.checkBox(UIText.CreateBranchPage_CheckoutButton).deselect();
		createBranchDialog.bot().button(IDialogConstants.FINISH_LABEL).click();
		TestUtil.waitForJobs(100, 5000);

		ObjectId resolvedBranch = repo.resolve("branch-from-tag");
		ObjectId resolvedTagCommit = repo.resolve("SomeTag^{commit}");
		assertNotNull(resolvedBranch);
		assertEquals(resolvedTagCommit, resolvedBranch);
	}

	private SWTBotShell openCheckoutBranchDialog() {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("SwitchToMenu.label"),
				UIText.SwitchToMenu_OtherMenuLabel };
		ContextMenuHelper.clickContextMenu(projectExplorerTree, menuPath);
		SWTBotShell dialog = bot
				.shell(UIText.BranchSelectionAndEditDialog_WindowTitle);
		return dialog;
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

	private SWTBotShell openRenameBranchDialog() {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("AdvancedMenu.label"),
				util.getPluginLocalizedValue("RenameBranchMenu.label") };
		ContextMenuHelper.clickContextMenu(projectExplorerTree, menuPath);
		SWTBotShell dialog = bot.shell(UIText.RenameBranchDialog_WindowTitle);
		return dialog;
	}

	private SWTBotShell openDeleteBranchDialog() {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("AdvancedMenu.label"),
				util.getPluginLocalizedValue("DeleteBranchMenu.label") };
		ContextMenuHelper.clickContextMenu(projectExplorerTree, menuPath);
		SWTBotShell dialog = bot.shell(UIText.DeleteBranchDialog_WindowTitle);
		return dialog;
	}

	private SWTBotShell openResetDialog() {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util.getPluginLocalizedValue("ResetAction_label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, util
				.getPluginLocalizedValue("TeamMenu.label"), menuString);
		SWTBotShell dialog = bot
				.shell(UIText.ResetTargetSelectionDialog_WindowTitle);
		return dialog;
	}

	@Test
	public void testRenameBranch() throws Exception {
		SWTBotShell dialog = openRenameBranchDialog();

		SWTBotTreeItem localBranches = TestUtil
				.expandAndWait(dialog.bot().tree().getTreeItem(LOCAL_BRANCHES));
		TestUtil.getChildNode(localBranches, "stable").select();
		dialog.bot().button(UIText.RenameBranchDialog_RenameButtonLabel)
				.click();
		// rename stable to renamed
		SWTBotShell newNameDialog = bot.shell(UIText.BranchRenameDialog_Title);
		newNameDialog.bot().text().setText("master");
		assertFalse(newNameDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());

		newNameDialog.bot().text().setText("renamed");
		newNameDialog.bot().button(IDialogConstants.OK_LABEL).click();

		TestUtil.joinJobs(JobFamilies.CHECKOUT);

		dialog = openRenameBranchDialog();
		SWTBotTreeItem localBranches2 = TestUtil
				.expandAndWait(dialog.bot().tree().getTreeItem(LOCAL_BRANCHES));
		TestUtil.getChildNode(localBranches2, "renamed").select();
		dialog.bot().button(UIText.RenameBranchDialog_RenameButtonLabel)
				.click();
		// rename renamed to stable
		newNameDialog = bot.shell(UIText.BranchRenameDialog_Title);

		newNameDialog.bot().text().setText("stable");
		newNameDialog.bot().button(IDialogConstants.OK_LABEL).click();

		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		dialog = openRenameBranchDialog();
		SWTBotTreeItem localBranches3 = TestUtil
				.expandAndWait(dialog.bot().tree().getTreeItem(LOCAL_BRANCHES));
		TestUtil.getChildNode(localBranches3, "stable").select();
		dialog.close();
	}

	@Test
	public void testCheckoutTags() throws Exception {
		checkoutAndVerify(new String[] { TAGS, "SomeTag" });
	}

	private void checkoutAndVerify(String[] nodeTexts)
			throws IOException, Exception {
		SWTBotShell dialog = openCheckoutBranchDialog();
		TableCollection tc = dialog.bot().tree().selection();
		assertEquals("Wrong selection count", 0, tc.rowCount());

		TestUtil.navigateTo(dialog.bot().tree(), nodeTexts).select();
		tc = dialog.bot().tree().selection();
		assertEquals("Wrong selection count", 1, tc.rowCount());
		assertTrue("Wrong item selected", tc.get(0, 0).startsWith(nodeTexts[1]));

		Repository repo = lookupRepository(repositoryFile);

		dialog.bot().button(UIText.CheckoutDialog_OkCheckout).click();
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		if (ObjectId.isId(repo.getBranch())) {
			String mapped = Activator.getDefault().getRepositoryUtil()
					.mapCommitToRef(repo, repo.getBranch(), false);
			assertEquals("Wrong branch", nodeTexts[1],
					mapped.substring(mapped
					.lastIndexOf('/') + 1));
		} else
			assertEquals("Wrong branch", nodeTexts[1], repo.getBranch());
	}

	private void checkout(String[] nodeTexts) throws Exception {
		SWTBotShell dialog = openCheckoutBranchDialog();
		TestUtil.navigateTo(dialog.bot().tree(), nodeTexts).select();
		TableCollection tc = dialog.bot().tree().selection();
		assertEquals("Wrong selection count", 1, tc.rowCount());
		assertTrue("Wrong item selected", tc.get(0, 0).startsWith(nodeTexts[1]));

		dialog.bot().button(UIText.CheckoutDialog_OkCheckout).click();
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
	}

}
