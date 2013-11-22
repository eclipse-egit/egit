/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

public class PushTagsWizardTester {

	private final SWTBot wizard;

	public static PushTagsWizardTester startWizard(SWTBotTree projectTree) {
		TestUtil util = new TestUtil();
		String remoteMenu = util.getPluginLocalizedValue("RemoteSubMenu.label");
		String pushBranchMenu = util
				.getPluginLocalizedValue("PushTagsCommand.name");
		ContextMenuHelper.clickContextMenu(projectTree, "Team", remoteMenu,
				pushBranchMenu);

		SWTWorkbenchBot bot = new SWTWorkbenchBot();
		SWTBot wizard = bot.shell(UIText.PushTagsWizard_WindowTitle).bot();
		return new PushTagsWizardTester(wizard);
	}

	public PushTagsWizardTester(SWTBot wizard) {
		this.wizard = wizard;
	}

	public void selectRemote(String remoteName) {
		SWTBotCombo remoteCombo = wizard
				.comboBoxWithLabel(UIText.PushTagsPage_RemoteLabel);
		String[] items = remoteCombo.items();
		for (String item : items) {
			if (item.startsWith(remoteName + ":")) {
				remoteCombo.setSelection(item);
				return;
			}
		}
		throw new IllegalStateException("Could not select remote '"
				+ remoteName + "', items were: " + Arrays.toString(items));
	}

	public void assertNextDisabled() {
		assertFalse("Expected Next button to be disabled",
				wizard.button(IDialogConstants.NEXT_LABEL).isEnabled());
	}

	public void selectTag(String tagName) {
		SWTBotTable table = wizard.table();
		for (int i = 0; i < table.rowCount(); i++) {
			SWTBotTableItem item = table.getTableItem(i);
			if (item.getText().startsWith(tagName + " ")) {
				item.check();
				return;
			}
		}
		fail("Could not find item for tag name " + tagName);
	}

	public void next() {
		wizard.button(IDialogConstants.NEXT_LABEL).click();
	}

	public void finish() {
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.PUSH, 60,
				TimeUnit.SECONDS);
		wizard.button(IDialogConstants.FINISH_LABEL).click();
		jobJoiner.join();
	}
}
