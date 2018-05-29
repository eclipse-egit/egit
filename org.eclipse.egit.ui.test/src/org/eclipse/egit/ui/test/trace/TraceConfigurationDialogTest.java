/*******************************************************************************
 * Copyright (c) 2010, SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.test.trace;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.common.EGitTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.GitTraceConfigurationDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class TraceConfigurationDialogTest {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	private SWTBotShell configurationDialog;

	@BeforeClass
	public static void beforeClass() throws Exception {
		EGitTestCase.closeWelcomePage();
		// make sure tracing is off globally
		Activator.getDefault().getDebugOptions().setDebugEnabled(false);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		// make sure tracing is off globally
		Activator.getDefault().getDebugOptions().setDebugEnabled(false);
	}

	@Before
	public void before() throws Exception {
		getDialog();
	}

	@Test
	public void testMainSwitch() throws Exception {
		SWTBotCheckBox box = configurationDialog.bot().checkBox(
				UIText.GitTraceConfigurationDialog_PlatformSwitchCheckbox);
		SWTBotTree tree = findTree();
		assertFalse(box.isChecked());
		assertFalse(tree.isEnabled());
		box.click();
		assertTrue(box.isChecked());
		assertTrue(tree.isEnabled());

		configurationDialog.close();
		getDialog();
		box = configurationDialog.bot().checkBox(
				UIText.GitTraceConfigurationDialog_PlatformSwitchCheckbox);

		tree = findTree();
		assertFalse(box.isChecked());
		assertFalse(tree.isEnabled());
		box.click();
		assertTrue(box.isChecked());
		assertTrue(tree.isEnabled());

		configurationDialog.bot().button(IDialogConstants.OK_LABEL).click();
		getDialog();
		box = configurationDialog.bot().checkBox(
				UIText.GitTraceConfigurationDialog_PlatformSwitchCheckbox);

		tree = findTree();
		assertTrue(box.isChecked());
		assertTrue(tree.isEnabled());
		box.click();
		assertFalse(box.isChecked());
		assertFalse(tree.isEnabled());
		configurationDialog.bot().button(IDialogConstants.OK_LABEL).click();
	}

	@Test
	public void testTreeNode() throws Exception {
		SWTBotCheckBox box = configurationDialog.bot().checkBox(
				UIText.GitTraceConfigurationDialog_PlatformSwitchCheckbox);
		SWTBotTree tree = findTree();
		assertFalse(box.isChecked());
		assertFalse(tree.isEnabled());
		box.click();
		assertTrue(box.isChecked());
		assertTrue(tree.isEnabled());

		SWTBotTreeItem item = tree.getAllItems()[0];
		assertFalse(item.isChecked());
		item.check();
		assertTrue(item.isChecked());

		configurationDialog.close();
		getDialog();
		box = configurationDialog.bot().checkBox(
				UIText.GitTraceConfigurationDialog_PlatformSwitchCheckbox);

		tree = findTree();
		assertFalse(box.isChecked());
		assertFalse(tree.isEnabled());
		box.click();
		assertTrue(box.isChecked());
		assertTrue(tree.isEnabled());

		item = tree.getAllItems()[0];
		assertFalse(item.isChecked());
		item.check();
		assertTrue(item.isChecked());

		configurationDialog.bot().button(IDialogConstants.OK_LABEL).click();
		getDialog();
		box = configurationDialog.bot().checkBox(
				UIText.GitTraceConfigurationDialog_PlatformSwitchCheckbox);

		tree = findTree();
		assertTrue(box.isChecked());
		assertTrue(tree.isEnabled());

		item = tree.getAllItems()[0];
		assertTrue(item.isChecked());

		configurationDialog.bot().button(
				UIText.GitTraceConfigurationDialog_DefaultButton).click();

		assertFalse(item.isChecked());
		box.click();
		configurationDialog.bot().button(IDialogConstants.OK_LABEL).click();
	}

	private SWTBotTree findTree() {
		return configurationDialog.bot().treeWithId("LocationTree");
	}

	private void getDialog() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				if (configurationDialog != null)
					configurationDialog.close();

				GitTraceConfigurationDialog dlg = new GitTraceConfigurationDialog(
						new Shell(Display.getDefault()));
				dlg.setBlockOnOpen(false);
				dlg.open();
				configurationDialog = bot.shell(
						UIText.GitTraceConfigurationDialog_ShellTitle)
						.activate();
			}
		});
	}
}
