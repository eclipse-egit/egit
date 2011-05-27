/*******************************************************************************
 * Copyright (c) 2010, SAP AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.share;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.common.ExistingOrNewPage;
import org.eclipse.egit.ui.common.ExistingOrNewPage.Row;
import org.eclipse.egit.ui.common.SharingWizard;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class SharingWizardTest {

	private static final String projectName0 = "TestProject";
	private static final String projectName1 = "TestProject1";
	private static final String projectName2 = "TestProject2";
	private static final String projectName3 = "TestProject3";

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	private SharingWizard sharingWizard;

	@BeforeClass
	public static void beforeClass() throws Exception {

		MockSystemReader mockSystemReader = new MockSystemReader();
		SystemReader.setInstance(mockSystemReader);
		mockSystemReader.setProperty(Constants.GIT_CEILING_DIRECTORIES_KEY,
				ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
						.getAbsoluteFile().toString());

		if (bot.activeView().getTitle().equals("Welcome"))
			bot.viewByTitle("Welcome").close();
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();

		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	private static String createProject(String projectName) {
		bot.menu("File").menu("New").menu("Project...").click();
		SWTBotShell createProjectDialogShell = bot.shell("New Project");
		bot.tree().getTreeItem("General").expand().getNode("Project").select();
		bot.button("Next >").click();

		bot.textWithLabel("Project name:").setText(projectName);

		String path = bot.textWithLabel("Location:").getText();
		bot.button("Finish").click();
		bot.waitUntil(Conditions.shellCloses(createProjectDialogShell), 10000);
		return path;
	}

	@After
	public void after() throws Exception {
		erase(projectName0);
		erase(projectName1);
		erase(projectName2);
		erase(projectName3);
		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
				IResource.DEPTH_INFINITE, null);
		new Eclipse().reset();
	}

	private void erase(String projectName) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
				projectName);
		if (project.exists()) {
			project.close(null);
			project.delete(false, null);
		}
	}

	@Before
	public void setupViews() {
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();
		sharingWizard = new SharingWizard();
	}

	@Test
	public void shareProjectAndCreateRepo() throws Exception {
		createProject(projectName0);
		ExistingOrNewPage existingOrNewPage = sharingWizard
				.openWizard(projectName0);

		// initial state
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		String projectPath = workspace.getRoot().getProject(projectName0)
				.getLocation().toOSString();

		existingOrNewPage.assertContents(false, projectName0, projectPath, "", "");
		existingOrNewPage.assertEnabling(false, false, false);

		// select project
		bot.tree().getTreeItem(projectName0).select();
		existingOrNewPage.assertContents(false, projectName0, projectPath, "",
				projectPath);
		existingOrNewPage.assertEnabling(true, true, false);

		// create repository
		bot.button("Create Repository").click();

		String repopath = workspace.getRoot().getProject(projectName0)
				.getLocation().append(Constants.DOT_GIT).toOSString();
		existingOrNewPage
				.assertContents(true, projectName0, projectPath, repopath, "");
		existingOrNewPage.assertEnabling(false, false, true);

		assertTrue((new File(repopath)).exists());

		// share project
		bot.button("Finish").click();
		Thread.sleep(1000);
		assertEquals("org.eclipse.egit.core.GitProvider",
				workspace.getRoot().getProject(projectName0)
						.getPersistentProperty(
								new QualifiedName("org.eclipse.team.core",
										"repository")));
	}

	@Test
	public void shareProjectWithAlreadyCreatedRepos() throws IOException,
			InterruptedException, NoFilepatternException, NoHeadException,
			NoMessageException, ConcurrentRefUpdateException,
			JGitInternalException, WrongRepositoryStateException {
		FileRepository repo1 = new FileRepository(new File(
				createProject(projectName1), "../.git"));
		repo1.create();
		repo1.close();
		FileRepository repo2 = new FileRepository(new File(
				createProject(projectName2), ".git"));
		repo2.create();
		repo2.close();
		FileRepository repo3 = new FileRepository(new File(
				createProject(projectName3), ".git"));
		repo3.create();
		Git git = new Git(repo3);
		git.add().addFilepattern(".").call();
		git.commit().setAuthor("A U Thior", "au.thor@example.com").setMessage("Created Project 3").call();
		repo3.close();

		ExistingOrNewPage existingOrNewPage = sharingWizard.openWizard(
				projectName1, projectName2, projectName3);

		// initial state
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		String projectPath1 = workspace.getRoot().getProject(projectName1)
				.getLocation().toOSString();
		String projectPath2 = workspace.getRoot().getProject(projectName2)
				.getLocation().toOSString();
		String projectPath3 = workspace.getRoot().getProject(projectName3)
				.getLocation().toOSString();
		existingOrNewPage.assertContents(
				new Row[] {
						new Row(true, projectName1, projectPath1, ".."
								+ File.separator + ".git"),
						new Row(false, projectName2, projectPath2, "", new Row[] {
								new Row(false, ".", "", ".git"),
								new Row(false, "..", "", ".." + File.separator
										+ ".git")}),
						new Row(false, projectName3, projectPath3, "", new Row[] {
								new Row(true, ".", "", ".git"),
								new Row(false, "..", "", ".." + File.separator
										+ ".git")
						})}, "");

		bot.tree().getAllItems()[1].getItems()[0].check();
		existingOrNewPage.assertEnabling(false, false, true);
		bot.button("Finish").click();
		Thread.sleep(1000);
		assertEquals(repo1.getDirectory().getCanonicalPath(), RepositoryMapping
				.getMapping(workspace.getRoot().getProject(projectName1))
				.getRepository().getDirectory().toString());
		assertEquals(repo2.getDirectory().getCanonicalPath(), RepositoryMapping
				.getMapping(workspace.getRoot().getProject(projectName2))
				.getRepository().getDirectory().toString());
	}
}
