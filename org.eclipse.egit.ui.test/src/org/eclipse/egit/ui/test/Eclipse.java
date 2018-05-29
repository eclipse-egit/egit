/*******************************************************************************
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import java.util.List;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

public class Eclipse {

	private final SWTWorkbenchBot bot;

	public Eclipse() {
		this.bot = new SWTWorkbenchBot();
	}

	/**
	 * Save and close all editors. Close all dialogs.
	 */
	public void reset() {
		saveAll();
		closeAllEditors();
		closeAllShells();
	}

	private void closeAllShells() {
		SWTBotShell[] shells = bot.shells();
		for (SWTBotShell shell : shells) {
			if (shell.isOpen() && !isEclipseShell(shell)) {
				shell.close();
			}
		}
	}

	@SuppressWarnings("boxing")
	public static boolean isEclipseShell(final SWTBotShell shell) {
		return UIThreadRunnable.syncExec(new BoolResult() {
			@Override
			public Boolean run() {
				return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getShell() == shell.widget;
			}
		});
	}

	public void closeAllEditors() {
		List<? extends SWTBotEditor> editors = bot.editors();
		for (SWTBotEditor editor : editors) {
			editor.close();
		}
	}

	public void saveAll() {
		List<? extends SWTBotEditor> editors = bot.editors();
		for (SWTBotEditor editor : editors) {
			editor.save();
		}
	}

	/**
	 * Opens the Eclipse Preferences and activates the dialog
	 *
	 * @param preferencePage
	 *            previous instance of preference page, maybe null, if passed it
	 *            will be reopened
	 * @return the preferencePage
	 */
	public SWTBotShell openPreferencePage(SWTBotShell preferencePage) {
		if (preferencePage != null)
			preferencePage.close();
		// This does not work on Mac
		// bot.menu("Window").menu("Preferences").click();
		// Launch preferences programmatically instead
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow();
				ActionFactory.PREFERENCES.create(workbenchWindow).run();

			}
		});
		TestUtil.processUIEvents();
		return bot.shell("Preferences").activate();
	}

}
