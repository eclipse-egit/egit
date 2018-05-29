/*******************************************************************************
 * Copyright (c) 2010, SAP AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

public class SharingWizard {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	public ExistingOrNewPage openWizard(String ... projectNames) {
		SWTBotTree tree = bot.viewById(JavaUI.ID_PACKAGES).bot().tree();

		tree.select(projectNames);
		ContextMenuHelper.clickContextMenu(tree, "Team", "Share Project...");

		bot.table().getTableItem("Git").select();
		bot.button("Next >").click();

		return new ExistingOrNewPage();
	}
}
