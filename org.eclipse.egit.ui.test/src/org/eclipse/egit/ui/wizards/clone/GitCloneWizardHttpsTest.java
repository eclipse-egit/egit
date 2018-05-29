/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.clone;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellIsActive;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.common.RepoPropertiesPage;
import org.eclipse.egit.ui.common.RepoRemoteBranchesPage;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.BeforeClass;
import org.junit.Test;

public class GitCloneWizardHttpsTest extends GitCloneWizardTestBase {

	@BeforeClass
	public static void setup() throws Exception {
		TestUtil.disableProxy();
		r = new SampleTestRepository(NUMBER_RANDOM_COMMITS, true, true);
	}

	@Test
	public void canCloneARemoteRepo() throws Exception {
		destRepo = new File(ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toFile(), "test" + System.nanoTime());

		importWizard.openWizard();
		RepoPropertiesPage propertiesPage = importWizard.openRepoPropertiesPage();
		propertiesPage.setURI(r.getSecureUri());
		propertiesPage.setUser("agitter");
		propertiesPage.setPassword("letmein");
		propertiesPage.setStoreInSecureStore(false);
		SWTBotShell wizardShell = bot.activeShell();

		RepoRemoteBranchesPage remoteBranches = propertiesPage
				.nextToRemoteBranches();
		bot.waitUntil(
				shellIsActive(UIText.EGitCredentialsProvider_information));
		// SSL trust dialog
		bot.checkBox(1).select();
		bot.button("OK").click();
		bot.waitUntil(new ActiveShell(wizardShell, "import wizard shell"));
		cloneRepo(destRepo, remoteBranches);
	}

	private static class ActiveShell extends DefaultCondition {

		private SWTBotShell shell;

		private String msg;

		private final SWTBot localBot = new SWTBot();

		public ActiveShell(SWTBotShell shell, String msg) {
			this.shell = shell;
			this.msg = msg;
		}

		@Override
		public String getFailureMessage() {
			return "The expected shell did not activate: " + msg;
		}

		@Override
		public boolean test() throws Exception {
			try {
				return localBot.activeShell().widget == shell.widget;
			} catch (WidgetNotFoundException e) {
				// Ignore and return false below.
			}
			return false;
		}
	}

}
