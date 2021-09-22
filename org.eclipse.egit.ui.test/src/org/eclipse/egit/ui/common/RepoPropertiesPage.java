/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertEnabled;
import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertNotEnabled;
import static org.eclipse.swtbot.swt.finder.SWTBotAssert.assertText;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.osgi.framework.Version;

public class RepoPropertiesPage {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	public void setURI(String uri) {
		uri().setText(uri);
	}

	public void setUser(String user) {
		user().setText(user);
	}

	public void setPassword(String password) {
		password().setText(password);
	}

	public void setStoreInSecureStore(boolean store) {
		if (store)
			storeCheckBox().select();
		else
			storeCheckBox().deselect();
	}

	public void appendToURI(String toAppend) {
		SWTBotText uri = uri();
		uri.setText(uri.getText() + toAppend);
	}

	public void setPushDestination(String destination) {
		SWTBotCombo destinationCombo = bot.comboBox();
		String[] items = destinationCombo.items();
		for (int i = 0; i < items.length; i++)
			if (items[i].startsWith(destination)) {
				destinationCombo.setSelection(i);
				return;
			}
		fail("Could not find destination " + destination);
	}

	@SuppressWarnings("boxing")
	public void assertSourceParams(String message, String expectHost,
			String expectPath, String expectProtocol, String expectPort,
			boolean enablePort, String expectUser, String expectPassword,
			boolean enabledUser, boolean enabledPass) {
		if (message != null) {
			// TODO: magic number, looks dangerous!
			assertWizardDialogMessage(bot, message);
			assertNotEnabled(bot.button("Next >"));
		} else {
			assertWizardDialogMessage(bot,
					"Enter the location of the source repository.");
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

	private void assertWizardDialogMessage(SWTBot dialogBot,
			String expectedText) {
		// The TitleAreaDialog's title message was changed to Label in Eclipse
		// 4.18; changed back to Text in 4.21.
		Version jFaceVersion = Platform.getBundle("org.eclipse.jface")
				.getVersion();
		if (jFaceVersion.compareTo(Version.valueOf("3.22.0")) < 0
				|| jFaceVersion.compareTo(Version.valueOf("3.23.0")) >= 0) {
			dialogBot.text(expectedText);
		} else {
			// Unfortunately, there are many labels in the wizard, their number
			// is even dynamic, and the ones from the title area are at the end.
			boolean result = UIThreadRunnable
					.<Boolean> syncExec(() -> Boolean.valueOf(dialogBot
							.widgets(widgetOfType(Label.class)).stream()
							.anyMatch(l -> l.getText().equals(expectedText))))
					.booleanValue();
			assertTrue("No label with text '" + expectedText + "' found",
					result);
		}
	}

	public void assertURI(String expected) {
		assertText(expected, uri());
	}

	private SWTBotText uri() {
		return bot.textWithLabel("URI:");
	}

	private SWTBotText user() {
		return bot.textWithLabel("User:");
	}

	private SWTBotText password() {
		return bot.textWithLabel("Password:");
	}

	private SWTBotCheckBox storeCheckBox() {
		return bot.checkBox("Store in Secure Store");
	}

	public RepoRemoteBranchesPage nextToRemoteBranches(String string) {
		setURI(string);
		return nextToRemoteBranches();
	}

	public RepoRemoteBranchesPage nextToRemoteBranches() {
		bot.button("Next >").click();
		return new RepoRemoteBranchesPage();
	}

}
