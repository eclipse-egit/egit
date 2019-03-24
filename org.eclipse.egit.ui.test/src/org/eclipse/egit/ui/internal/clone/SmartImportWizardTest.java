/*******************************************************************************
 * Copyright (C) 2015, 2019 Red Hat Inc. and others.
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
 * - Thomas Wolf <thomas.wolf@paranor.ch> : use a test repo and refactor after move.
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SmartImportWizardTest extends LocalRepositoryTestCase {

	private static final String PROJECT_FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<projectDescription>\n" + "<name></name>\n"
			+ "<comment></comment>\n" + "<buildSpec></buildSpec>\n"
			+ "<natures></natures>\n" + "</projectDescription>\n";

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private Repository repository;

	@Before
	public void prepareTest() throws Exception {
		repository = createLocalTestRepository(REPO1);
		File workingTree = repository.getWorkTree();
		createProject(workingTree, "Project1");
		createProject(workingTree, "Project2");
		try (Git git = new Git(repository)) {
			git.add().addFilepattern(".").call();
			git.commit().setMessage("Initial commit").call();
		}
	}

	@Test
	public void test() throws Exception {
		Set<IProject> initialProjects = new HashSet<>(Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()));
		Set<IProject> newProjects = null;
		Set<String> expectedProjectNames = new HashSet<>();
		expectedProjectNames.add("Project1");
		expectedProjectNames.add("Project2");
		bot.menu("File").menu("Import...").click();
		expandAndWait(bot.tree().getTreeItem("Git"))
				.select("Projects from Git (with smart import)");
		bot.button("Next >").click();
		bot.tree().select("Clone URI");
		bot.button("Next >").click();
		String path = Path
				.fromOSString(repository.getDirectory().getCanonicalPath())
				.toString();
		if (!path.startsWith("/")) {
			// Might be Windows: C:/foo...
			path = '/' + path;
		}
		bot.text().setText("file://" + path);
		bot.button("Next >").click();
		waitForButtonEnabled("Next >", 5);
		bot.button("Next >").click();
		File tmpDir = tmp.newFolder(getClass().getName());
		try {
			bot.text().setText(tmpDir.toString());
			bot.button("Next >").click();
			waitForButtonEnabled("Finish", 5);
			bot.button("Finish").click();

			Job.getJobManager().join(SmartImportJob.class,
					new NullProgressMonitor());

			newProjects = new HashSet<>(Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()));
			newProjects.removeAll(initialProjects);
			assertTrue("There should be more than one project imported",
					newProjects.size() > 1);

			for (IProject project : newProjects) {
				assertTrue("Project " + project.getName() + " not found",
						project.exists());
				expectedProjectNames.remove(project.getName());
			}
			assertEquals("Some projects not imported", "[]",
					expectedProjectNames.toString());
		} finally {
			// clean up
			if (newProjects != null) {
				for (IProject p : newProjects) {
					p.delete(true, new NullProgressMonitor());
				}
			}
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

	private void createProject(File workingTree, String name)
			throws IOException {
		File dir = new File(workingTree, name);
		Files.createDirectories(dir.toPath());
		File project = new File(dir, ".project");
		String prj = PROJECT_FILE.replace("</name>", name + "</name>");
		Files.write(project.toPath(), prj.getBytes(StandardCharsets.UTF_8));
	}
}
