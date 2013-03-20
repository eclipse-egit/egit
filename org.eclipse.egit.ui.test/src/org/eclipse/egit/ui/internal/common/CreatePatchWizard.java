/*******************************************************************************
 * Copyright (C) 2012, IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.common;

import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.test.team.actions.LocationPage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class CreatePatchWizard {

	public static class NoChangesPopup {
		private SWTBotShell shell;

		public NoChangesPopup(SWTBotShell shell) {
			this.shell = shell;
		}

		public void cancelPopup() {
			shell.close();
		}
	}

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	protected static final TestUtil util = new TestUtil();

	private SWTBotShell shell;

	public CreatePatchWizard(SWTBotShell shell) {
		this.shell = shell;
	}

	public static void openWizard(final String... projects) {
		SWTBotTree projectExplorerTree = bot
				.viewById("org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		SWTBotTreeItem[] items = util.getProjectItems(projectExplorerTree, projects);
		projectExplorerTree.select(items);

		String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("CreatePatchAction.label") };
		ContextMenuHelper.clickContextMenu(projectExplorerTree, menuPath);
	}

	public void finish() {
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
	}

	public void close() {
		shell.close();
	}

	public LocationPage getLocationPage() {
		return new LocationPage(shell);
	}

	public SWTBotShell getShell() {
		return shell;
	}
}
