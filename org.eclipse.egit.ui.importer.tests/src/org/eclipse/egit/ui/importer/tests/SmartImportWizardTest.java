/*******************************************************************************
 * Copyright (C) 2015 Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.) : initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.importer.tests;

import static org.junit.Assert.fail;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.taskdefs.Delete;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@Ignore
@RunWith(SWTBotJunit4ClassRunner.class)
public class SmartImportWizardTest {

	protected static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	private static volatile boolean welcomePageClosed = false;

	private static boolean initialAutobuild;

	@BeforeClass
	public static void prepareTest() throws CoreException {
		closeWelcomePage();
		initialAutobuild = setAutobuild(false);
	}

	@AfterClass
	public static void restoreState() throws CoreException {
		setAutobuild(initialAutobuild);
	}

	private static boolean setAutobuild(boolean value) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		boolean isAutoBuilding = desc.isAutoBuilding();
		if (isAutoBuilding != value) {
			desc.setAutoBuilding(value);
			workspace.setDescription(desc);
		}
		return isAutoBuilding;
	}

	private static void closeWelcomePage() {
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
	public void setBotAndAtivateShell() {
		SWTBotShell[] shells = bot.shells();
		for (SWTBotShell shell : shells) {
			if (isEclipseShell(shell)) {
				shell.activate();
				return;
			}
		}
		fail("No active Eclipse shell found!");
	}

	@After
	public void closeShells() {
		SWTBotShell[] shells = bot.shells();
		for (SWTBotShell shell : shells) {
			if (shell.isOpen() && !isEclipseShell(shell)) {
				shell.close();
			}
		}
	}

	@SuppressWarnings("boxing")
	protected static boolean isEclipseShell(final SWTBotShell shell) {
		return UIThreadRunnable.syncExec(new BoolResult() {
			@Override
			public Boolean run() {
				return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getShell() == shell.widget;
			}
		});
	}

	@Test
	public void test() throws Exception {
		try {
			new URL("https://git.eclipse.org/r/").openConnection();
		} catch (Exception ex) {
			Assume.assumeNoException("Internet access is required for that test", ex);
		}

		Set<IProject> initialProjects = new HashSet<>(Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()));
		Set<IProject> newProjects = null;

		bot.menu("File").menu("Import...").click();
		expandAndWait(bot.tree().getTreeItem("Git"))
				.select("Projects from Git (with smart import)");
		bot.button("Next >").click();
		bot.tree().select("Clone URI");
		bot.button("Next >").click();
		bot.text().setText("https://git.eclipse.org/r/jgit/jgit");
		bot.button("Next >").click();
		waitForButtonEnabled("Next >", 30); // Time to fetch branch info
		bot.button("Deselect All").click();
		bot.tree().getTreeItem("master").check();
		bot.button("Next >").click();
		Path tmpDir = Files.createTempDirectory(getClass().getName());
		try {
			bot.text().setText(tmpDir.toString());
			bot.button("Next >").click();
			waitForButtonEnabled("Finish", 180); // Time to clone repo
			bot.button("Finish").click();

			Job.getJobManager().join(SmartImportJob.class,
					new NullProgressMonitor());

			newProjects = new HashSet<>(Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()));
			newProjects.removeAll(initialProjects);
			Assert.assertTrue("There should be more than one project imported with jgit...", newProjects.size() > 1);

			IProject someProject = ResourcesPlugin.getWorkspace().getRoot().getProject("org.eclipse.jgit.ui");
			Assert.assertTrue("Project not found", someProject.exists());
		} finally {
			// clean up
			if (newProjects != null) {
				for (IProject p : newProjects) {
					p.delete(true, new NullProgressMonitor());
				}
			}

			Delete deleteTask = new Delete();
			deleteTask.setDir(tmpDir.toFile());
			deleteTask.execute();
		}
	}

	private void waitForButtonEnabled(final String buttonLabel,
			final long timeoutInSec) {
		bot.waitWhile(new ICondition() {
			@Override
			public boolean test() throws Exception {
				return !bot.button(buttonLabel).isEnabled();
			}

			@Override
			public void init(SWTBot swtBot) {
				// Nothing
			}

			@Override
			public String getFailureMessage() {
				return buttonLabel + " button not enabled within "
						+ timeoutInSec
						+ "sec";
			}
		}, timeoutInSec * 1000L);
	}

	private SWTBotTreeItem expandAndWait(final SWTBotTreeItem treeItem) {
		treeItem.expand();
		new SWTBot().waitUntil(new DefaultCondition() {

			@Override
			public boolean test() {
				SWTBotTreeItem[] children = treeItem.getItems();
				return children != null && children.length > 0;
			}

			@Override
			public String getFailureMessage() {
				return "No children found for " + treeItem.getText();
			}
		});
		return treeItem;
	}

}
