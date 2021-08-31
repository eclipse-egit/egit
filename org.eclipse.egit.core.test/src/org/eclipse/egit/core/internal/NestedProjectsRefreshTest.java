/*******************************************************************************
 * Copyright (C) 2021, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for bug 574806/bug 575240.
 */
public class NestedProjectsRefreshTest extends GitTestCase {

	private final static String PROJECT_FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<projectDescription>\n"
			+ "\t<name>{0}</name>\n"
			+ "\t<comment></comment>\n"
			+ "\t<projects>\n"
			+ "\t</projects>\n"
			+ "\t<buildSpec>\n"
			+ "\t</buildSpec>\n"
			+ "\t<natures>\n"
			+ "\t</natures>\n"
			+ "{1}</projectDescription>";

	private final static String RESOURCE_FILTER = "\t<filteredResources>\n"
			+ "\t\t<filter>\n"
			+ "\t\t\t<id>1574249520208</id>\n"
			+ "\t\t\t<name></name>\n"
			+ "\t\t\t<type>10</type>\n"
			+ "\t\t\t<matcher>\n"
			+ "\t\t\t\t<id>org.eclipse.ui.ide.multiFilter</id>\n"
			+ "\t\t\t\t<arguments>1.0-projectRelativePath-matches-false-false-sub_*</arguments>\n"
			+ "\t\t\t</matcher>\n"
			+ "\t\t</filter>\n"
			+ "\t</filteredResources>\n";

	private File testDirectory;

	private File repoPath;

	private Path topPath;

	private Path aPath;

	private Path bPath;

	private IProject topProject;

	private IProject aProject;

	private IProject bProject;

	private String topOid;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		testDirectory = testUtils.createTempDir("NestedProjectsRefreshTest");
		File gitDirectory = new File(testDirectory, ".git");
		try (Repository repository = FileRepositoryBuilder
				.create(gitDirectory.getCanonicalFile())) {
			repository.create();
			prepareProjects(repository);
			repoPath = repository.getDirectory();
		}
		shareProjects();
	}

	private IProject removeProject(IProject p) throws CoreException {
		if (p != null && p.exists()) {
			p.delete(false, true, null);
		}
		return null;
	}

	@Override
	@After
	public void tearDown() throws Exception {
		aProject = removeProject(aProject);
		bProject = removeProject(bProject);
		topProject = removeProject(topProject);
		RepositoryUtil.INSTANCE.removeDir(repoPath);
		RepositoryCache.INSTANCE.clear();
		testUtils.deleteTempDirs();
		super.tearDown();
	}

	private void createProjectFile(Path path, String projectName,
			String extras) throws Exception {
		Files.write(path.resolve(IProjectDescription.DESCRIPTION_FILE_NAME),
				MessageFormat
						.format(PROJECT_FILE, projectName,
								extras == null ? "" : extras)
						.getBytes(StandardCharsets.UTF_8));
	}

	private void prepareProjects(Repository repository) throws Exception {
		File projectDir = new File(repository.getWorkTree(), "topProject")
				.getAbsoluteFile();
		assertTrue(projectDir.mkdirs());
		topPath = projectDir.toPath();
		createProjectFile(topPath, "topProject", RESOURCE_FILTER);
		Path subDir = topPath.resolve("sub_projects");
		Files.createDirectory(subDir);
		// Create two nested projects
		aPath = subDir.resolve("aProject");
		bPath = subDir.resolve("bProject");
		Files.createDirectory(aPath);
		Files.createDirectory(bPath);
		createProjectFile(aPath, "aProject", null);
		createProjectFile(bPath, "bProject", null);
		Files.write(aPath.resolve("a.txt"),
				"a".getBytes(StandardCharsets.UTF_8));
		Files.write(bPath.resolve("b.txt"),
				"b".getBytes(StandardCharsets.UTF_8));
		try (Git git = new Git(repository)) {
			git.add().addFilepattern(".").call();
			git.commit().setMessage("Initial commit").call();
			Files.write(bPath.resolve("d.txt"),
					"d".getBytes(StandardCharsets.UTF_8));
			git.add().addFilepattern("topProject/sub_projects/bProject/d.txt")
					.call();
			RevCommit base = git.commit().setMessage("Add d.txt").call();
			// Now modify a.txt and create c.txt in bProject
			Files.write(aPath.resolve("a.txt"),
					"A".getBytes(StandardCharsets.UTF_8));
			Files.write(bPath.resolve("c.txt"),
					"c".getBytes(StandardCharsets.UTF_8));
			git.add().addFilepattern("topProject/sub_projects/aProject/a.txt")
					.addFilepattern("topProject/sub_projects/bProject/c.txt")
					.call();
			RevCommit topCommit = git.commit().setMessage("Add c.txt").call();
			topOid = topCommit.getName();
			git.reset().setMode(ResetType.HARD).setRef(base.getName()).call();
		}
	}

	private void shareProjects() throws Exception {
		RepositoryUtil.INSTANCE.addConfiguredRepository(repoPath);
		// Import the projects
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.run(monitor -> {
			SubMonitor progress = SubMonitor.convert(monitor, 3);
			topProject = importProject(workspace, "topProject", topPath,
					progress.newChild(1));
			aProject = importProject(workspace, "aProject", aPath,
					progress.newChild(1));
			bProject = importProject(workspace, "bProject", bPath,
					progress.newChild(1));
		}, null, IWorkspace.AVOID_UPDATE, null);
		// Share them all
		ConnectProviderOperation connect = new ConnectProviderOperation(
				topProject, repoPath);
		connect.setRefreshResources(false);
		connect.execute(null);
		connect = new ConnectProviderOperation(aProject, repoPath);
		connect.setRefreshResources(false);
		connect.execute(null);
		connect = new ConnectProviderOperation(bProject, repoPath);
		connect.setRefreshResources(false);
		connect.execute(null);
		Job.getJobManager().join(JobFamilies.INDEX_DIFF_CACHE_UPDATE, null);
	}

	private IProject importProject(IWorkspace workspace, String name, Path path,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		IProject newProject = workspace.getRoot().getProject(name);
		IProjectDescription desc = workspace.newProjectDescription(name);
		desc.setLocationURI(URIUtil.toURI(path.toString()));
		newProject.create(desc, progress.newChild(1));
		newProject.open(progress.newChild(1));
		return newProject;
	}

	@Test
	public void testResetRefreshesProperly() throws Exception {
		aProject.close(null);
		ResetOperation reset = new ResetOperation(
				RepositoryCache.INSTANCE.lookupRepository(repoPath), topOid,
				ResetType.HARD);
		reset.execute(null);
		assertTrue(Files.exists(bPath.resolve("c.txt")));
		IResource cTxt = null;
		for (IResource resource : bProject.members()) {
			if (resource.getName().equals("c.txt")) {
				cTxt = resource;
				break;
			}
		}
		assertNotNull("c.txt not found in Eclipse resource tree", cTxt);
		assertTrue("c.txt does not exist in Eclipse resource tree",
				cTxt.exists());
	}
}
