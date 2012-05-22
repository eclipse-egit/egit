/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Replace With actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class ReplaceActionsTest extends LocalRepositoryTestCase {
	private static File repositoryFile;

	private static SWTBotPerspective perspective;

	private static ObjectId commitOfTag;

	private static IResourceChangeListener listener;

	private static boolean testFileChanged = false;

	private static IResourceDeltaVisitor deltaVisitor;

	@BeforeClass
	public static void setup() throws Exception {
		deltaVisitor = new IResourceDeltaVisitor() {
			public boolean visit(IResourceDelta delta) throws CoreException {
				if (delta.getResource().getFullPath()
						.equals(new Path("/GeneralProject/folder/test.txt")))
					testFileChanged = true;
				return !testFileChanged;
			}
		};
		listener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					event.getDelta().accept(deltaVisitor);
				} catch (CoreException e) {
					// ignore here
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener,
				IResourceChangeEvent.POST_CHANGE);
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);
		perspective = bot.activePerspective();
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();

		TagBuilder tag = new TagBuilder();
		tag.setTag("SomeTag");
		tag.setTagger(RawParseUtils.parsePersonIdent(TestUtil.TESTAUTHOR));
		tag.setMessage("I'm just a little tag");
		tag.setObjectId(repo.resolve(repo.getFullBranch()),
				Constants.OBJ_COMMIT);
		commitOfTag = tag.getObjectId();
		TagOperation top = new TagOperation(repo, tag, false);
		top.execute(null);
		touchAndSubmit(null);
		waitInUI();
	}

	@AfterClass
	public static void shutdown() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
		perspective.activate();
	}

	@Before
	public void prepare() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		if (!repo.getBranch().equals("master")) {
			BranchOperation bop = new BranchOperation(repo, "refs/heads/master");
			bop.execute(null);
		}
	}

	@Test
	public void testReplaceWithPrevious() throws Exception {
		String newContent = getTestFileContent();
		String menuLabel = util
				.getPluginLocalizedValue("replaceWithPreviousVersionAction.label");
		clickReplaceWith(menuLabel);
		bot.shell(UIText.DiscardChangesAction_confirmActionTitle).bot()
				.button(IDialogConstants.OK_ID).click();
		TestUtil.joinJobs(JobFamilies.DISCARD_CHANGES);
		ResourcesPlugin.getWorkspace().getRoot()
				.refreshLocal(IResource.DEPTH_INFINITE, null);
		waitInUI();
		String oldContent = getTestFileContent();
		assertFalse(newContent.equals(oldContent));
	}

	@Test
	public void testReplaceWithPreviousWithMerge() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		Git git = new Git(repo);
		ObjectId masterId = repo.resolve("refs/heads/master");
		Ref newBranch = git.checkout().setCreateBranch(true)
				.setStartPoint(commitOfTag.name()).setName("toMerge").call();
		ByteArrayInputStream bis = new ByteArrayInputStream(
				"Modified".getBytes());
		ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFolder(FOLDER).getFile(FILE2)
				.setContents(bis, false, false, null);
		bis.close();
		git.commit().setAll(true).setMessage("To be merged").call();
		git.merge().include(masterId).call();
		String newContent = getTestFileContent();
		String menuLabel = util
				.getPluginLocalizedValue("replaceWithPreviousVersionAction.label");
		clickReplaceWith(menuLabel);
		bot.shell(UIText.DiscardChangesAction_confirmActionTitle).bot()
				.button(IDialogConstants.OK_ID).click();
		SWTBotShell selectDialog = bot
				.shell(UIText.CommitSelectDialog_WindowTitle);
		assertEquals(2, selectDialog.bot().table().rowCount());
		selectDialog.close();
		// we have closed, so nothing should have changed
		String oldContent = getTestFileContent();
		assertTrue(newContent.equals(oldContent));

		testFileChanged = false;
		clickReplaceWith(menuLabel);
		bot.shell(UIText.DiscardChangesAction_confirmActionTitle).bot()
				.button(IDialogConstants.OK_ID).click();
		selectDialog = bot.shell(UIText.CommitSelectDialog_WindowTitle);
		selectDialog.bot().table().select(0);
		selectDialog.bot().button(IDialogConstants.OK_ID).click();
		TestUtil.joinJobs(org.eclipse.egit.ui.JobFamilies.DISCARD_CHANGES);
		waitForFileChanged();
		oldContent = getTestFileContent();
		assertFalse(newContent.equals(oldContent));
		// cleanup: checkout again master and delete merged branch
		git.checkout().setName("refs/heads/master").call();
		git.branchDelete().setBranchNames(newBranch.getName()).setForce(true)
				.call();
	}

	private void clickReplaceWith(String menuLabel) {
		SWTBotTree projectExplorerTree = bot
				.viewById("org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Replace With",
				menuLabel);
	}

	private void waitForFileChanged() throws InterruptedException {
		if (testFileChanged)
			return;
		for (int i = 0; i < 10; i++) {
			waitInUI();
			if (testFileChanged)
				return;
		}
		fail("No file change detected");
	}
}
