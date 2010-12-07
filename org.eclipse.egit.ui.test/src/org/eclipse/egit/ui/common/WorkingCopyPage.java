/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;

public class WorkingCopyPage {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	public void assertDirectory(String localDir) {
		assertText(localDir, bot.textWithLabel("Directory:"));
	}

	public void assertBranch(String branch) {
		assertText(branch, bot.comboBoxWithLabel("Initial branch:"));
	}

	public void assertRemoteName(String remoteName) {
		assertText(remoteName, bot.textWithLabel("Remote name:"));
	}

	public void waitForCreate() {
		// calculate the expected target directory
		String targetDir = bot.textWithLabel("Directory:").getText()
				+ File.separatorChar + Constants.DOT_GIT;
		assertFalse(
				"Clone target should not be in the configured repositories list",
				Activator.getDefault().getRepositoryUtil()
						.getConfiguredRepositories().contains(targetDir));

		SWTBotShell dialogShell = bot.shell("Import Projects from Git");
		SWTBotTable table = dialogShell.bot().table();
		int expectedRowCount = table.rowCount() + 1;
		bot.button("Finish").click();
		
		// wait until clone operation finished.
		// wizard executes clone operation using getContainer.run
		// wait until the repository table gets an additional entry
		bot.waitUntil(Conditions.tableHasRows(table, expectedRowCount), 20000);


		// depending on the timing, the clone job may already be run
		// but the repository is not yet added to our list, of
		// repositories. Wait until that happend.
		for (int i = 0; i < 3; i++) {
			if (Activator.getDefault().getRepositoryUtil()
					.getConfiguredRepositories().contains(targetDir))
				return;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// ignore here
			}
		}
	}

	@SuppressWarnings("boxing")
	public void assertWorkingCopyExists() {
		// get the destination directory from the wizard
		String dirName = bot.textWithLabel("Directory:").getText();
		File dir = new File(dirName);

		// wait for the clone to finish
		waitForCreate();
		// check if we find the working directory
		assertEquals(dir.exists() && dir.isDirectory(), true);
	}

	public void setRemoteName(String string) {
		bot.textWithLabel("Remote name:").setText(string);
	}

	public void setDirectory(String string) {
		bot.textWithLabel("Directory:").setText(string);
	}

}
