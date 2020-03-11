/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertEnabled;
import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertNotEnabled;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withText;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.waitForWidget;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.widgetIsEnabled;

import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Assert;

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

		bot.waitUntil(widgetIsEnabled(tree), 60000);

		SWTBotTreeItem[] items = tree.getAllItems();
		for (SWTBotTreeItem item : items) {
			item.uncheck();
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void assertErrorMessage(String errorMessage) {
		bot.waitUntil(
				waitForWidget(allOf(widgetOfType(Text.class), withText(" "
						+ errorMessage))), 20000);
	}

	public void selectTagOption(TagOpt option) {
		int index = 0;
		switch (option) {
		case AUTO_FOLLOW:
			break;
		case FETCH_TAGS:
			index = 1;
			break;
		case NO_TAGS:
			index = 2;
			break;
		default:
			Assert.fail("unsupported tag option");
			break;
		}
		bot.radio(index).click();
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
