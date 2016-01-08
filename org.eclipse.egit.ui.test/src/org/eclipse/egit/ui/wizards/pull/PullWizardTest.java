/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.pull;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.waitForShell;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestBase;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.forms.finder.SWTFormsBot;
import org.eclipse.swtbot.forms.finder.widgets.SWTBotHyperlink;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.matchers.AbstractMatcher;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.hamcrest.Description;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class PullWizardTest extends GitRepositoriesViewTestBase {

	private final class ShellWithTextPrefixMatcher
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

	private SWTBotView repoView;


	@Before
	public void setUp() throws Exception {
		clearView();
		setVerboseBranchMode(false);
		repositoryFile = createProjectAndCommitToRepository();
		createRemoteRepository(repositoryFile);
		repoView = TestUtil.showView(RepositoriesView.VIEW_ID);
		repoView.setFocus();
		SWTBotHyperlink link = new SWTFormsBot(repoView.bot().getFinder())
				.hyperlink("Add an existing local Git repository");
		// workaround for "click()" see bug 473290
		link.setFocus();
		link.pressShortcut(org.eclipse.swt.SWT.CR, org.eclipse.swt.SWT.LF);
		bot.shell("Add Git Repositories");
		// remove .git suffix
		bot.text().setText(repositoryFile.getPath().substring(0,
				repositoryFile.getPath().length() - 4));
		bot.button("Search").click();
		bot.waitWhile(new ICondition() {
			@Override
			public boolean test() throws Exception {
				return bot.tree().getAllItems().length == 0;
			}
			@Override
			public void init(SWTBot aBot) {
				// nothing to do
			}
			@Override
			public String getFailureMessage() {
				return "Tree wasn't populated with repository";
			}
		});
		bot.tree().getAllItems()[0].check();
		bot.button(IDialogConstants.FINISH_LABEL).click();
	}

	@Test
	public void pullFromProjectRepo() throws Exception {
		SWTBotTree repositoriesTree = repoView.bot().tree();
		repositoriesTree.select(0);
		repositoriesTree.contextMenu("Pull...").click();
		bot.shell("Pull").setFocus();
		assertTrue("Remote combo misses items",
				bot.comboBox().items().length > 1);
		bot.button(IDialogConstants.FINISH_LABEL).click();
		ShellWithTextPrefixMatcher shellWithTextPrefixMatcher = new ShellWithTextPrefixMatcher(
				"Pull Result");
		bot.waitUntil(waitForShell(shellWithTextPrefixMatcher));
		shellWithTextPrefixMatcher.getFoundShell().setFocus();
		bot.button(IDialogConstants.OK_LABEL).click();
	}


}
