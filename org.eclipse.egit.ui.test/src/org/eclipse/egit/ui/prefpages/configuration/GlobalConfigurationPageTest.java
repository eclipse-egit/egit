/*******************************************************************************
 * Copyright (c) 2010, SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.prefpages.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.egit.ui.common.EGitTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class GlobalConfigurationPageTest {

	private static final String TESTSECTION = "testsection";

	private static final String TESTSUBSECTION = "testsubsection";

	private static final String TESTNAME = "testname";

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	private static final TestUtil util = new TestUtil();

	private static FileBasedConfig config;

	private SWTBotShell preferencePage;

	@BeforeClass
	public static void beforeClass() throws Exception {
		EGitTestCase.closeWelcomePage();
		config = SystemReader.getInstance().openUserConfig(null, FS.DETECTED);
		config.load();
	}

	@Before
	public void before() throws Exception {
		config.unsetSection(TESTSECTION, TESTSUBSECTION);
		config.unsetSection(TESTSECTION, null);
		config.save();
		getGitConfigurationPreferencePage();
	}

	private void getGitConfigurationPreferencePage() {
		preferencePage = new Eclipse().openPreferencePage(preferencePage);
		SWTBotTreeItem team = preferencePage.bot().tree().getTreeItem("Team");
		team.expand()
				.getNode(util.getPluginLocalizedValue("GitPreferences_name"))
				.expand()
				.getNode(util.getPluginLocalizedValue("ConfigurationPage.name"))
				.select();
	}

	@After
	public void after() throws Exception {
		if (preferencePage != null)
			preferencePage.close();
	}

	@AfterClass
	public static void afterTest() throws Exception {
		// reset saved preferences state
		SWTBotShell preferencePage = new Eclipse().openPreferencePage(null);
		preferencePage.bot().tree(0).getTreeItem("General").select();
		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
	}

	@Test
	public void testNodes() throws Exception {
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
		preferencePage.bot().button(
				UIText.ConfigurationEditorComponent_AddButton).click();
		SWTBotShell addDialog = bot
				.shell(UIText.AddConfigEntryDialog_AddConfigTitle);
		addDialog.activate();
		// neither key nor value set
		assertTrue(!addDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_ValueLabel)
				.setText("Somevalue");
		// key empty
		assertTrue(!addDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION);
		// no dot
		assertTrue(!addDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION + "." + TESTNAME);
		// ok: one dot
		assertTrue(addDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(TESTSECTION + "." + TESTSUBSECTION + "." + TESTNAME);
		// ok: two dots
		assertTrue(addDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
		addDialog.bot().textWithLabel(UIText.AddConfigEntryDialog_KeyLabel)
				.setText(
						TESTSECTION + "." + TESTSUBSECTION + "." + TESTNAME
								+ "." + TESTNAME);
		// too many dots
		assertTrue(!addDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
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

		bot.button(UIText.ConfigurationEditorComponent_RemoveButton).click();
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

		bot.button(UIText.ConfigurationEditorComponent_RemoveButton).click();
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

		bot.button(UIText.ConfigurationEditorComponent_RemoveButton).click();
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
