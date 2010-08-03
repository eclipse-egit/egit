/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertEnabled;
import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertNotEnabled;
import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertText;
import static org.junit.Assert.assertEquals;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;

public class RepoPropertiesPage {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	public void setURI(String uri) {
		uri().setText(uri);
	}

	public void appendToURI(String toAppend) {
		SWTBotText uri = uri();
		uri.setText(uri.getText() + toAppend);
	}

	@SuppressWarnings("boxing")
	public void assertSourceParams(String message, String expectHost,
			String expectPath, String expectProtocol, String expectPort,
			boolean enablePort, String expectUser, String expectPassword,
			boolean enabledUser, boolean enabledPass) {
		if (message != null) {
			// TODO: magic number, looks dangerous!
			assertText(message, bot.text(6));
			assertNotEnabled(bot.button("Next >"));
		} else {
			assertEquals("Enter the location of the source repository.", bot
					.text(6).getText());
			assertEnabled(bot.button("Next >"));
		}
		assertText(expectHost, bot.textWithLabel("Host:"));
		assertText(expectPath, bot.textWithLabel("Repository path:"));
		assertText(expectProtocol, bot.comboBoxWithLabel("Protocol:"));
		assertText(expectPort, bot.textWithLabel("Port:"));
		assertText(expectUser, bot.textWithLabel("User:"));
		assertText(expectPassword, bot.textWithLabel("Password:"));

		assertEquals(enablePort, bot.textWithLabel("Port:").isEnabled());
		assertEquals(enabledUser, bot.textWithLabel("User:").isEnabled());
		assertEquals(enabledPass, bot.label("Password:").isEnabled());
		assertEquals(enabledPass, bot.textWithLabel("Password:").isEnabled());
	}

	public void assertURI(String expected) {
		assertText(expected, uri());
	}

	private SWTBotText uri() {
		return bot.textWithLabel("URI:");
	}

	public RepoRemoteBranchesPage nextToRemoteBranches(String string) {
		setURI(string);
		bot.button("Next >").click();
		return new RepoRemoteBranchesPage();
	}
}
