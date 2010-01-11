package org.eclipse.egit.ui.wizards.clone;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

public class GitImportRepoWizard {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	// TODO: speed it up by calling the wizard using direct eclipse API.
	public RepoPropertiesPage openWizard() {
		bot.menu("File").menu("Import...").click();
		bot.shell("Import").activate();

		bot.tree().expandNode("Git").select("Git Repository");

		bot.button("Next >").click();

		bot.shell("Import Git Repository").activate();

		return new RepoPropertiesPage();
	}

}
