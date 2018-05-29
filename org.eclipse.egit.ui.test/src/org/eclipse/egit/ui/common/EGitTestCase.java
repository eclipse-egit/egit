/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.junit.Assert.fail;

import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public abstract class EGitTestCase {

	protected static final SWTWorkbenchBot bot = new SWTWorkbenchBot();
	protected static final TestUtil util = new TestUtil();
	private static volatile boolean welcomePageClosed = false;

	@BeforeClass
	public static void closeWelcomePage() {
		if (welcomePageClosed)
			return;
		try {
			bot.viewByTitle("Welcome").close();
		} catch (WidgetNotFoundException e) {
			// somebody else probably closed it, lets not feel bad about it.
		} finally {
			welcomePageClosed = true;
		}
	}

	@Before
	public void activateShell() {
		SWTBotShell[] shells = bot.shells();
		for (SWTBotShell shell : shells) {
			if (Eclipse.isEclipseShell(shell)) {
				shell.activate();
				return;
			}
		}
		fail("No active Eclipse shell found!");
	}

	@After
	public void resetWorkbench() {
		new Eclipse().reset();
	}
}
