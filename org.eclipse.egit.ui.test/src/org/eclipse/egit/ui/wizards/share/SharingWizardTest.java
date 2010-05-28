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

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class SharingWizardTest {
	static {
		System.setProperty("org.eclipse.swtbot.playback.delay", "50");
	}

	private static final String projectName = "TestProject";

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

		bot.menu("File").menu("New").menu("Project...").click();
		bot.tree().getTreeItem("General").expand().getNode("Project").select();
		bot.button("Next >").click();

		bot.textWithLabel("Project name:").setText(projectName);

		bot.button("Finish").click();
	}

	@Before
	public void setupViews() {
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();
		sharingWizard = new SharingWizard();
	}

	@Test
	public void shareProjectWithNewlyCreatedRepo() throws Exception {
		ExistingOrNewPage existingOrNewPage = sharingWizard
				.openWizard(projectName);

		// initial state
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		String projectPath = workspace.getRoot().getProject(projectName)
				.getLocation().toOSString();

		existingOrNewPage.assertContents(projectName, projectPath, "", "");
		existingOrNewPage.assertEnabling(false, false, false);

		// select project
		bot.tree().getTreeItem(projectName).select();
		existingOrNewPage.assertContents(projectName, projectPath, "",
				projectPath);
		existingOrNewPage.assertEnabling(true, true, false);

		// create repository
		bot.button("Create Repository").click();

		String repopath = workspace.getRoot().getProject(projectName)
				.getLocation().append(Constants.DOT_GIT).toOSString();
		existingOrNewPage
				.assertContents(projectName, projectPath, repopath, "");
		existingOrNewPage.assertEnabling(false, false, true);

		assertTrue((new File(repopath)).exists());

		// share project
		bot.button("Finish").click();
		assertEquals("org.eclipse.egit.core.GitProvider",
				workspace.getRoot().getProject(projectName)
						.getPersistentProperty(
								new QualifiedName("org.eclipse.team.core",
										"repository")));
	}

	// TODO: push this in the junit class runner. This can then be shared across
	// all tests.
	@After
	public void resetWorkbench() {
		new Eclipse().reset();
	}

}
