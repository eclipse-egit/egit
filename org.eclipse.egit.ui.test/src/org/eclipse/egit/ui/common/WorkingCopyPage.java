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
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

public class WorkingCopyPage {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	private final String cloneUrl;

	public WorkingCopyPage(String cloneUrl) {
		this.cloneUrl = cloneUrl;
	}

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

		bot.button("Finish").click();

		try {
			SWTBotShell shell = bot.shell("Cloning from " + cloneUrl);

			// This is not a performance test. Allow lots of time to complete
			bot.waitUntil(shellCloses(shell), 120000);
		} catch (WidgetNotFoundException e1) {
			// if the "Cloning from" window opens and closes very quickly
			// (faster than the replay delay), we end up here, and we do an
			// alternate test to see if the repository was cloned
			// This is not a performance test. Allow lots of time to complete
			for (int i = 0; i < 10; i++) {
				if (Activator.getDefault().getRepositoryUtil()
						.getConfiguredRepositories().contains(targetDir))
					return;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// ignore here
				}
			}
			fail("The Repository was not created");
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
