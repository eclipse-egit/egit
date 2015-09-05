/*******************************************************************************
 * Copyright (C) 2015 Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.) : initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.importer.tests;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.taskdefs.Delete;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.SWTBotTestCase;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class EasymportWizardTest extends SWTBotTestCase {

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
		bot.tree().expandNode("Git").select("Projects from Git (with smart import)");
		bot.button("Next >").click();
		bot.tree().select("Clone URI");
		bot.button("Next >").click();
		bot.text().setText("https://git.eclipse.org/r/jgit/jgit");
		bot.button("Next >").click();
		bot.button("Deselect All").click();
		bot.tree().getTreeItem("master").check();
		bot.button("Next >").click();
		Path tmpDir = Files.createTempDirectory(getClass().getName());
		try {
			bot.text().setText(tmpDir.toString());
			bot.button("Next >").click();
			bot.waitWhile(new ICondition() {

				@Override
				public boolean test() throws Exception {
					return !bot.button("Next >").isEnabled();
				}

				@Override
				public void init(SWTBot bot) {
				}
				@Override
				public String getFailureMessage() {
					return null;
				}
			}, 180000); // Time to clone repo, up to 3 minutes
			bot.button("Next >").click();
			bot.button("Finish").click();

			bot.shell("Nested Projects");
			bot.button("OK").click();

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

}
