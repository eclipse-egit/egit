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

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertEnabled;
import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertNotEnabled;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.widgetIsEnabled;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;

public class RepoRemoteBranchesPage {
	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	private final String cloneUrl;

	public RepoRemoteBranchesPage(String cloneUrl) {
		this.cloneUrl = cloneUrl;
	}

	public void assertRemoteBranches(String... branches) {
		SWTBotTable table = bot.table();
		bot.waitUntil(widgetIsEnabled(table));
		for (String branch : branches) {
			table.getTableItem(branch);
		}
	}

	public void selectBranches(String... branches) {
		SWTBotTable table = bot.table();
		bot.waitUntil(widgetIsEnabled(table));
		for (String branch : branches) {
			table.getTableItem(branch).check();
		}
	}

	public WorkingCopyPage nextToWorkingCopy() {
		bot.button("Next >").click();
		return new WorkingCopyPage(cloneUrl);
	}

	public void deselectAllBranches() {
		SWTBotTable table = bot.table();

		bot.waitUntil(widgetIsEnabled(table));

		int rowCount = table.rowCount();
		for (int i = 0; i < rowCount; i++) {
			table.getTableItem(i).uncheck();
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
