/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

public class GitImportRepoWizard {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	// TODO: speed it up by calling the wizard using direct eclipse API.
	public void openWizard() {
		bot.menu("File").menu("Import...").click();
		bot.shell("Import").activate();

		bot.tree().expandNode("Git").select("Projects from Git");

		bot.button("Next >").click();
	}

	public RepoPropertiesPage openCloneWizard() {
		bot.shell("Import Projects from Git").activate();

		bot.button("Clone...").click();

		bot.shell("Clone Git Repository").activate();

		return new RepoPropertiesPage();
	}

	public int configuredRepoCount() {
		bot.shell("Import Projects from Git").activate();

		return bot.table(0).rowCount();
	}

	public void selectAndCloneRepository(int index) {
		bot.shell("Import Projects from Git").activate();

		bot.table(0).select(index);

		bot.button("Next >").click();

		bot.button("Next >").click();

		bot.button("Select All").click();
	}

	public void waitForCreate() {
		bot.button("Finish").click();

		SWTBotShell shell = bot.shell("Import Projects from Git");

		bot.waitUntil(shellCloses(shell), 120000);
	}

}
