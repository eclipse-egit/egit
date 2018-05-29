/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.pull;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.waitForShell;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestBase;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.matchers.AbstractMatcher;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.hamcrest.Description;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class PullWizardTest extends GitRepositoriesViewTestBase {

	private static final class ShellWithTextPrefixMatcher
			extends AbstractMatcher<Shell> {
		private String prefix;

		private SWTBotShell foundShell;

		public ShellWithTextPrefixMatcher(@NonNull String prefix) {
			this.prefix = prefix;
		}

		@Override
		public void describeTo(Description arg0) {
			arg0.appendText("With title starting by '" + this.prefix + "'");
		}

		@Override
		protected boolean doMatch(Object item) {
			SWTBotShell shell = new SWTBotShell((Shell) item);
			if (shell.getText().startsWith(this.prefix)) {
				this.foundShell = shell;
				return true;
			}
			return false;
		}

		public SWTBotShell getFoundShell() {
			return this.foundShell;
		}
	}

	@AfterClass
	public static void afterClass() {
		SystemReader.setInstance(null);
	}

	private File repositoryFile;

	@Before
	public void setUp() throws Exception {
		deleteAllProjects();
		clearView();
		repositoryFile = createProjectAndCommitToRepository();
		createRemoteRepository(repositoryFile);
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
		refreshAndWait();
		assertHasRepo(repositoryFile);
	}

	@Test
	public void pullFromProjectRepo() throws Exception {
		SWTBotTree repositoriesTree = getOrOpenView().bot().tree();
		repositoriesTree.select(0);
		repositoriesTree.contextMenu("Pull...").click();
		bot.shell("Pull").setFocus();
		assertTrue("Remote combo misses items",
				bot.comboBox().items().length > 1);
		bot.textWithLabel(UIText.PullWizardPage_referenceLabel)
				.setText("master");
		bot.button(IDialogConstants.FINISH_LABEL).click();
		ShellWithTextPrefixMatcher shellWithTextPrefixMatcher = new ShellWithTextPrefixMatcher(
				"Pull Result");
		bot.waitUntil(waitForShell(shellWithTextPrefixMatcher));
		shellWithTextPrefixMatcher.getFoundShell().setFocus();
		bot.button(IDialogConstants.CLOSE_LABEL).click();
	}


}
