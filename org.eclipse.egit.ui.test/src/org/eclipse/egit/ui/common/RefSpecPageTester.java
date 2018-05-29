/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.waits.Conditions;

public class RefSpecPageTester {

	private final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	public void waitUntilPageIsReady(int nrOfEcpectedTableEntries) {
		bot.waitUntil(Conditions.tableHasRows(bot.table(), nrOfEcpectedTableEntries), 20000);
	}
}
