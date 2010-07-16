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

import java.io.File;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
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
		bot.button("Finish").click();

		SWTBotShell shell = bot.shell("Cloning from " + cloneUrl);

		// This is not a performance test. Allow lots of time to complete
		bot.waitUntil(shellCloses(shell), 120000);
	}

	@SuppressWarnings("boxing")
	public void assertWorkingCopyExists(String uri) {
		// get the destination directory from the wizard
		String dirName = bot.textWithLabel("Directory:").getText();
		File dir = new File(dirName);
		System.out.println(dir);

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
