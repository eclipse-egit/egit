/*******************************************************************************
 * Copyright (C) 2012, 2013 IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.test.team.actions.LocationPage;
import org.eclipse.egit.ui.test.team.actions.OptionsPage;
import org.eclipse.jface.dialogs.IDialogConstants;
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

	protected static final TestUtil util = new TestUtil();

	private SWTBotShell shell;

	public CreatePatchWizard(SWTBotShell shell) {
		this.shell = shell;
	}

	public static void openWizard(final String... projects) {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
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

	public void finishWithNoneFormat() {
		LocationPage locationPage = getLocationPage();
		OptionsPage optionsPage = locationPage.nextToOptionsPage();
		optionsPage.setFormat(CoreText.DiffHeaderFormat_None);
		finish();
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
