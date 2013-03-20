/*******************************************************************************
 * Copyright (c) 2010, SAP AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.common;

import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

public class SharingWizard {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	public ExistingOrNewPage openWizard(String ... projectNames) {
		SWTBotTree tree = bot.viewByTitle("Package Explorer").bot().tree();

		tree.select(projectNames);
		ContextMenuHelper.clickContextMenu(tree, "Team", "Share Project...");

		bot.table().getTableItem("Git").select();
		bot.button("Next >").click();

		return new ExistingOrNewPage();
	}
}
