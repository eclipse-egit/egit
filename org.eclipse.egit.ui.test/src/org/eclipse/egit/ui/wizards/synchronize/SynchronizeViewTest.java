/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.synchronize;

import static org.junit.Assert.assertTrue;

import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.wizards.clone.GitImportRepoWizard;
import org.eclipse.egit.ui.wizards.clone.RepoPropertiesPage;
import org.eclipse.egit.ui.wizards.clone.RepoRemoteBranchesPage;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class SynchronizeViewTest {

	static {
		System.setProperty("org.eclipse.swtbot.playback.delay", "50");
	}

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	@Test
	public void fakeTest() {
		assertTrue(true);
	}

	@Before
	public void setupViews() {
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();
		GitImportRepoWizard importWizard = new GitImportRepoWizard();

		importWizard.openWizard();
		if (importWizard.configuredRepoCount() != 2) {
			addRepository(importWizard, "git://egit.eclipse.org/egit.git");
			addRepository(importWizard, "git://egit.eclipse.org/jgit.git");
		}

		importWizard.selectAndCloneRepository(0);
		importWizard.waitForCreate();

		importWizard.openWizard();
		importWizard.selectAndCloneRepository(1);
		importWizard.waitForCreate();
	}

	// TODO: push this in the junit class runner. This can then be shared across
	// all tests.
	@BeforeClass
	public static void closeWelcomePage() {
		try {
			bot.viewByTitle("Welcome").close();
		} catch (WidgetNotFoundException e) {
			// somebody else probably closed it, lets not feel bad about it.
		}
	}

	// TODO: push this in the junit class runner. This can then be shared across
	// all tests.
	@After
	public void resetWorkbench() {
		new Eclipse().reset();
	}

	private void addRepository(GitImportRepoWizard importWizard, String repoUrl) {
		RepoPropertiesPage propertiesPage = importWizard.openCloneWizard();

		RepoRemoteBranchesPage remoteBranches = propertiesPage
				.nextToRemoteBranches(repoUrl);
		remoteBranches.selectBranches("master");

		remoteBranches.nextToWorkingCopy().waitForCreate();
	}

}
