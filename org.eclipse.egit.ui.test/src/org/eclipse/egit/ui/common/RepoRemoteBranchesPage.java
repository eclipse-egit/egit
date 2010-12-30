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

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertEnabled;
import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertNotEnabled;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.widgetIsEnabled;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class RepoRemoteBranchesPage {
	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	public void assertRemoteBranches(String... branches) {
		SWTBotTree tree = bot.tree();
		bot.waitUntil(widgetIsEnabled(tree), 20000);
		for (String branch : branches) {
			tree.getTreeItem(branch);
		}
	}

	public void selectBranches(String... branches) {
		SWTBotTree tree = bot.tree();
		bot.waitUntil(widgetIsEnabled(tree));
		for (String branch : branches) {
			tree.getTreeItem(branch).check();
		}
	}

	public WorkingCopyPage nextToWorkingCopy() {
		bot.button("Next >").click();
		return new WorkingCopyPage();
	}

	public void deselectAllBranches() {
		SWTBotTree tree = bot.tree();

		bot.waitUntil(widgetIsEnabled(tree));

		SWTBotTreeItem[] items = tree.getAllItems();
		for (int i = 0; i < items.length; i++) {
			items[i].uncheck();
		}
	}

	public void assertErrorMessage(String errorMessage) {
		bot.text(" " + errorMessage);
	}

	public void assertNextIsDisabled() {
		assertNotEnabled(bot.button("Next >"));
	}

	public void assertNextIsEnabled() {
		assertEnabled(bot.button("Next >"));
	}

	public void assertCannotProceed() {
		assertEnabled(bot.button("Cancel"));
		assertEnabled(bot.button("< Back"));
		assertNotEnabled(bot.button("Next >"));
		assertNotEnabled(bot.button("Finish"));
	}

	public void cancel() {
		bot.button("Cancel").click();
	}
}
