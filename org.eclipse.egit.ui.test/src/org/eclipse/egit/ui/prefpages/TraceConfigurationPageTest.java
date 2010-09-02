package org.eclipse.egit.ui.prefpages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.EGitTestCase;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
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
public class TraceConfigurationPageTest {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	private SWTBotShell preferencePage;

	private static final TestUtil util = new TestUtil();

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
		getPreferencePage();
	}

	@Test
	public void testMainSwitch() throws Exception {
		SWTBotCheckBox box = preferencePage.bot().checkBox(
				UIText.GitTracePreferencePage_PlatformTraceCheckbox);
		SWTBotTree tree = findTree();
		assertFalse(box.isChecked());
		assertFalse(tree.isEnabled());
		box.click();
		assertTrue(box.isChecked());
		assertTrue(tree.isEnabled());

		preferencePage.close();
		getPreferencePage();

		box = preferencePage.bot().checkBox(
				UIText.GitTracePreferencePage_PlatformTraceCheckbox);
		tree = findTree();
		assertFalse(box.isChecked());
		assertFalse(tree.isEnabled());
		box.click();
		assertTrue(box.isChecked());
		assertTrue(tree.isEnabled());

		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
		getPreferencePage();

		box = preferencePage.bot().checkBox(
				UIText.GitTracePreferencePage_PlatformTraceCheckbox);
		tree = findTree();
		assertTrue(box.isChecked());
		assertTrue(tree.isEnabled());
		box.click();
		assertFalse(box.isChecked());
		assertFalse(tree.isEnabled());
		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
	}

	@Test
	public void testTreeNode() throws Exception {
		SWTBotCheckBox box = preferencePage.bot().checkBox(
				UIText.GitTracePreferencePage_PlatformTraceCheckbox);
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

		preferencePage.close();
		getPreferencePage();

		box = preferencePage.bot().checkBox(
				UIText.GitTracePreferencePage_PlatformTraceCheckbox);
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

		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
		getPreferencePage();

		box = preferencePage.bot().checkBox(
				UIText.GitTracePreferencePage_PlatformTraceCheckbox);
		tree = findTree();
		assertTrue(box.isChecked());
		assertTrue(tree.isEnabled());

		item = tree.getAllItems()[0];
		assertTrue(item.isChecked());

		preferencePage.bot().button(JFaceResources.getString("defaults"))
				.click();

		assertFalse(item.isChecked());
		box.click();
		preferencePage.bot().button(IDialogConstants.OK_LABEL).click();
	}

	private SWTBotTree findTree() {
		return preferencePage.bot().treeWithId("LocationTree");
	}

	private void getPreferencePage() {
		if (preferencePage != null)
			preferencePage.close();
		bot.perspectiveById("org.eclipse.ui.resourcePerspective").activate();
		bot.menu("Window").menu("Preferences").click();
		preferencePage = bot.shell("Preferences").activate();
		SWTBotTreeItem team = preferencePage.bot().tree().getTreeItem("Team");
		team
				.expand()
				.getNode(util.getPluginLocalizedValue("GitPreferences_name"))
				.expand()
				.getNode(
						util
								.getPluginLocalizedValue("TraceConfigurationPage.name"))
				.select();
	}
}
