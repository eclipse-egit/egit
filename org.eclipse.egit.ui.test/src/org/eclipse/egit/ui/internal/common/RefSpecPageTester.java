/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.common;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.waits.Conditions;

public class RefSpecPageTester {

	private final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	public void waitUntilPageIsReady(int nrOfEcpectedTableEntries) {
		bot.waitUntil(Conditions.tableHasRows(bot.table(), nrOfEcpectedTableEntries), 20000);
	}
}
