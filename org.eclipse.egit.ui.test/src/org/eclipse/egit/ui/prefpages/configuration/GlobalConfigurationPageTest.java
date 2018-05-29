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
package org.eclipse.egit.ui.prefpages.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.ui.common.EGitTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.preferences.GlobalConfigurationPreferencePage;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class GlobalConfigurationPageTest {

	private static final String TESTSECTION = "testsection";

	private static final String TESTSUBSECTION = "testsubsection";

	private static final String TESTNAME = "testname";

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	private static File configFile;

	private static FileBasedConfig config;

	private SWTBotShell preferencePage;

	@BeforeClass
	public static void beforeClass() throws Exception {
		EGitTestCase.closeWelcomePage();
		configFile = File.createTempFile("gitconfigtest", "config");
		configFile.deleteOnExit();
		SystemReader.setInstance(new MockSystemReader() {
			@Override
			public FileBasedConfig openUserConfig(Config parent, FS fs) {
				return new FileBasedConfig(parent, configFile, fs);
			}
		});
		config = SystemReader.getInstance().openUserConfig(null, FS.DETECTED);
		config.load();
		clean();
	}

	private static void clean() throws Exception {
		config.unsetSection(TESTSECTION, TESTSUBSECTION + '.' + TESTNAME);
		config.unsetSection(TESTSECTION, TESTSUBSECTION);
		config.unsetSection(TESTSECTION, null);
		config.save();
	}

	private void getGitConfigurationPreferencePage() {
		if (preferencePage != null) {
			preferencePage.close();
			bot.waitUntil(Conditions.shellCloses(preferencePage));
		}
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				PreferencesUtil.createPreferenceDialogOn(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getShell(),
						GlobalConfigurationPreferencePage.ID, null, null)
						.open();
			}
		});
		bot.waitUntil(Conditions.shellIsActive("Preferences"));
		preferencePage = bot.shell("Preferences");
	}

	@After
	public void after() throws Exception {
		if (preferencePage != null) {
			preferencePage.close();
			bot.waitUntil(Conditions.shellCloses(preferencePage));
			preferencePage = null;
		}
		TestUtil.processUIEvents();
		clean();
	}

	@AfterClass
	public static void afterTest() throws Exception {
		configFile.delete();
		SystemReader.setInstance(null);
		// reset saved preferences state
		SWTBotShell preferencePage = new Eclipse().openPreferencePage(null);
		preferencePage.bot().tree(0).getTreeItem("General").select();
		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
		TestUtil.processUIEvents();
	}

	@Test
	public void testNodes() throws Exception {
		getGitConfigurationPreferencePage();
		SWTBotTree configTree = preferencePage.bot().tree(1);
		for (String section : config.getSections()) {
			SWTBotTreeItem sectionItem = configTree.getTreeItem(section);
			for (String subsection : config.getSubsections(section)) {
				SWTBotTreeItem subsectionItem = sectionItem.getNode(subsection);
				for (String entryName : config.getNames(section, subsection))
					try {
						subsectionItem.getNode(entryName);
					} catch (WidgetNotFoundException e) {
						subsectionItem.getNode(entryName + "[0]");
					}

			}
			for (String entryName : config.getNames(section))
				try {
					sectionItem.getNode(entryName);
				} catch (WidgetNotFoundException e) {
					sectionItem.getNode(entryName + "[0]");
				}
		}
	}

	@Test
	public void testAddSectionEntry() throws Exception {
		getGitConfigurationPreferencePage();
		preferencePage.bot().button(
				UIText.ConfigurationEditorComponent_AddButton).click();
		SWTBotShell addDialog = bot
				.shell(UIText.AddConfigEntryDialog_AddConfigTitle);
		addDialog.activate();
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION + "." + TESTNAME);
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_ValueLabel)
				.setText("true");
		// close the dialog
		addDialog.bot().button(IDialogConstants.OK_LABEL).click();
		// close the editor
		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
		config.load();

		assertTrue("Missing section", config.getSections()
				.contains(TESTSECTION));
		assertTrue("Missing name", config.getNames(TESTSECTION).contains(
				TESTNAME));
		assertEquals("Wrong value", "true", config.getString(TESTSECTION, null,
				TESTNAME));
	}

	@Test
	public void testAddSubSectionEntry() throws Exception {
		getGitConfigurationPreferencePage();
		preferencePage.bot().button(
				UIText.ConfigurationEditorComponent_AddButton).click();
		SWTBotShell addDialog = bot
				.shell(UIText.AddConfigEntryDialog_AddConfigTitle);
		addDialog.activate();
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION + "." + TESTSUBSECTION + "." + TESTNAME);
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_ValueLabel)
				.setText("true");
		// close the dialog
		addDialog.bot().button(IDialogConstants.OK_LABEL).click();
		// close the editor
		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
		config.load();

		assertTrue("Missing section", config.getSections()
				.contains(TESTSECTION));
		assertTrue("Missing subsection", config.getSubsections(TESTSECTION)
				.contains(TESTSUBSECTION));
		assertTrue("Missing name", config.getNames(TESTSECTION, TESTSUBSECTION)
				.contains(TESTNAME));
		assertEquals("Wrong value", "true", config.getString(TESTSECTION,
				TESTSUBSECTION, TESTNAME));
	}

	@Test
	public void testAddSubSectionEntryWithSuggestion() throws Exception {
		config.setString(TESTSECTION, TESTSUBSECTION, TESTNAME, "true");
		config.save();
		getGitConfigurationPreferencePage();
		preferencePage.bot().tree(1).getTreeItem(TESTSECTION).getNode(
				TESTSUBSECTION).select();
		preferencePage.bot().button(
				UIText.ConfigurationEditorComponent_AddButton).click();
		SWTBotShell addDialog = bot
				.shell(UIText.AddConfigEntryDialog_AddConfigTitle);
		addDialog.activate();
		String suggested = addDialog.bot().textWithLabel(
				UIText.AddConfigEntryDialog_KeyLabel).getText();
		assertEquals(TESTSECTION + "." + TESTSUBSECTION + ".", suggested);
		addDialog.close();
	}

	@Test
	public void testCanCreateSameEntryValue() throws Exception {
		config.setString(TESTSECTION, null, TESTNAME, "already");
		config.save();
		getGitConfigurationPreferencePage();
		preferencePage.bot().button(
				UIText.ConfigurationEditorComponent_AddButton).click();
		SWTBotShell addDialog = bot
				.shell(UIText.AddConfigEntryDialog_AddConfigTitle);
		addDialog.activate();
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION + "." + TESTNAME);
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_ValueLabel)
				.setText("true");
		assertTrue(addDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
		addDialog.close();
	}

	@Test
	public void testChecksForKey() throws Exception {
		getGitConfigurationPreferencePage();
		preferencePage.bot().button(
				UIText.ConfigurationEditorComponent_AddButton).click();
		SWTBotShell addDialog = bot
				.shell(UIText.AddConfigEntryDialog_AddConfigTitle);
		addDialog.activate();
		assertFalse("Should be disabled when neither key nor value set",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_ValueLabel)
				.setText("Somevalue");
		assertFalse("Should be disabled when no key",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION);
		assertFalse("Should be disabled when no dot",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION + "." + TESTNAME);
		assertTrue("Should be enabled with one dot",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION + "." + TESTSUBSECTION + "." + TESTNAME);
		assertTrue("Should be enabled with two dots",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION
						+ ". some stuff with dots.. and . non-ASCII characters: àéè."
						+ TESTNAME);
		// ok: first and last section alphanumeric,subsection will be quoted
		assertTrue("Should be enabled with strange subsection",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText("föö.bar.baz");
		assertFalse("Should be disabled with non-ASCII in first segment",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText("foo.bar.bàz");
		assertFalse("Should be disabled with non-ASCII in last segment",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText("foo bar.baz");
		assertFalse("Should be disabled with blank in first segment",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText("foo.bar baz");
		assertFalse("Should be disabled with blank in last segment",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText("foo-bar.baz-");
		assertTrue("Should be enabled with dashes",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText("foo.bar.");
		assertFalse("Should be disabled when ending in dot",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(".foo.bar.");
		assertFalse("Should be disabled when beginning with dot",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText("..");
		assertFalse("Should be disabled for \"..\"",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText("foobar.9nines");
		assertFalse("Should be disabled for variable name starting with digit",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText("foobar.-bar");
		assertFalse("Should be disabled for variable name starting with a dash",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText("foobar.b-9");
		assertTrue("Should be enabled for variable name starting with a letter",
				addDialog.bot().button(IDialogConstants.OK_LABEL).isEnabled());
	}

	@Test
	public void testSubsectionWithDot() throws Exception {
		getGitConfigurationPreferencePage();
		preferencePage.bot()
				.button(UIText.ConfigurationEditorComponent_AddButton).click();
		SWTBotShell addDialog = bot
				.shell(UIText.AddConfigEntryDialog_AddConfigTitle);
		addDialog.activate();

		// subsection containing a dot
		String subsection = TESTSUBSECTION + "." + TESTNAME;
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION + "." + subsection + "." + TESTNAME);
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_ValueLabel)
				.setText("true");
		assertTrue(addDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());

		// close the dialog
		addDialog.bot().button(IDialogConstants.OK_LABEL).click();
		// close the editor
		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();

		config.load();
		assertTrue("Missing section", config.getSections()
				.contains(TESTSECTION));
		Set<String> subsections = config.getSubsections(TESTSECTION);
		assertTrue("Missing subsection", subsections.contains(subsection));
		assertEquals("Wrong value", "true",
				config.getString(TESTSECTION, subsection, TESTNAME));
		addDialog.close();
	}

	@Test
	public void testRemoveValue() throws Exception {
		List<String> values = new ArrayList<String>(2);
		values.add("true");
		values.add("false");
		config.setStringList(TESTSECTION, null, TESTNAME, values);
		config.save();
		getGitConfigurationPreferencePage();
		preferencePage.bot().tree(1).getTreeItem(TESTSECTION).getNode(
				TESTNAME + "[0]").select();

		preferencePage.bot()
				.button(UIText.ConfigurationEditorComponent_RemoveButton)
				.click();
		// close the editor
		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
		config.load();
		values = Arrays.asList(config
				.getStringList(TESTSECTION, null, TESTNAME));
		assertEquals("Wrong number of values", 1, values.size());
		assertTrue(values.contains("false"));
	}

	@Test
	public void testRemoveSubSection() throws Exception {
		List<String> values = new ArrayList<String>(2);
		values.add("true");
		values.add("false");
		config.setStringList(TESTSECTION, null, TESTNAME, values);
		config.setStringList(TESTSECTION, TESTSUBSECTION, TESTNAME, values);
		config.save();
		getGitConfigurationPreferencePage();
		preferencePage.bot().tree(1).getTreeItem(TESTSECTION).getNode(
				TESTSUBSECTION).select();

		preferencePage.bot()
				.button(UIText.ConfigurationEditorComponent_RemoveButton)
				.click();
		SWTBotShell confirm = bot
				.shell(UIText.ConfigurationEditorComponent_RemoveSubsectionTitle);
		confirm.activate();
		confirm.bot().button(IDialogConstants.OK_LABEL).click();
		// close the editor
		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
		config.load();
		assertTrue("Subsection should be deleted", !config.getSubsections(
				TESTSECTION).contains(TESTSUBSECTION));
	}

	@Test
	public void testRemoveSection() throws Exception {
		List<String> values = new ArrayList<String>(2);
		values.add("true");
		values.add("false");
		config.setStringList(TESTSECTION, null, TESTNAME, values);
		config.setStringList(TESTSECTION, TESTSUBSECTION, TESTNAME, values);
		config.save();
		getGitConfigurationPreferencePage();
		preferencePage.bot().tree(1).getTreeItem(TESTSECTION).select();

		preferencePage.bot()
				.button(UIText.ConfigurationEditorComponent_RemoveButton)
				.click();
		SWTBotShell confirm = bot
				.shell(UIText.ConfigurationEditorComponent_RemoveSectionTitle);
		confirm.activate();
		confirm.bot().button(IDialogConstants.OK_LABEL).click();
		// close the editor
		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
		config.load();
		assertTrue("Values in section should be deleted", config.getStringList(
				TESTSECTION, null, TESTNAME).length == 0);
	}

	@Test
	public void testOpenEditor() throws Exception {
		getGitConfigurationPreferencePage();
		try {
			preferencePage.bot().button(
					UIText.ConfigurationEditorComponent_OpenEditorButton)
					.click();
			preferencePage.close();
			assertEquals(config.getFile().getName(), bot.activeEditor()
					.getTitle());
		} finally {
			bot.activeEditor().close();
		}
	}

}
