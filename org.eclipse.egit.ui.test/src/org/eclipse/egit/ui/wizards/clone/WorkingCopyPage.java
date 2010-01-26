/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.clone;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertText;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

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

	public void doNotImportProjectsAfterClone() {
		bot.checkBox("Import projects after clone").deselect();
	}

	public void waitForCreate() {
		bot.button("Finish").click();

		// TODO: pass the repo url in via the constructor from the first page.
		SWTBotShell shell = bot.shell("Cloning from git://repo.or.cz/egit.git");

		// This is not a performance test. Allow lots of time to complete
		bot.waitUntil(shellCloses(shell), 120000);
	}

	public void assertWorkingCopyExists() {
		String dirName = bot.textWithLabel("Directory:").getText();
		String message = " " + dirName + " is not an empty directory.";
		// check if the text box exists
		bot.text(message);
	}

	public void setRemoteName(String string) {
		bot.textWithLabel("Remote name:").setText(string);
	}

	public void setDirectory(String string) {
		bot.textWithLabel("Directory:").setText(string);
	}

}
