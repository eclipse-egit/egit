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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

public class PushResultDialogTester {

	private final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	public void assertResultMessage(String expectedMessage) {
		bot.styledText(expectedMessage);
	}

	public void closeDialog() {
		bot.button(IDialogConstants.CLOSE_LABEL).click();
	}

}
