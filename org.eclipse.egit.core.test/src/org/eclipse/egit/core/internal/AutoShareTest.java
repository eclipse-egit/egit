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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test for bug 574806/bug 575240.
 */
public class AutoShareTest extends GitTestCase {

	private static final String PROJECT_NAME = "ProjectInWorktree";

	private final static String PROJECT_FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<projectDescription>\n" + "\t<name>" + PROJECT_NAME + "</name>\n"
			+ "\t<comment></comment>\n" + "\t<projects>\n" + "\t</projects>\n"
			+ "\t<buildSpec>\n" + "\t</buildSpec>\n" + "\t<natures>\n"
			+ "\t</natures>\n" + "</projectDescription>";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private File repoPath;

	private Path projectPath;

	private IProject simpleProject;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		try (Repository repository = FileRepositoryBuilder
				.create(folder.newFolder(".git").getCanonicalFile())) {
			repository.create();
			prepareProject(repository);
			repoPath = repository.getDirectory();
		}
		System.out.println("Git repo at " + repoPath);
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if (simpleProject != null && simpleProject.exists()) {
			simpleProject.delete(false, true, null);
			simpleProject = null;
		}
		RepositoryUtil.INSTANCE.removeDir(repoPath);
		RepositoryCache.INSTANCE.clear();
		super.tearDown();
	}

	private void prepareProject(Repository repository) throws Exception {
		File projectDir = new File(repository.getWorkTree(), PROJECT_NAME)
				.getAbsoluteFile();
		assertTrue(projectDir.mkdirs());
		projectPath = projectDir.toPath();
		Files.write(
				projectPath.resolve(IProjectDescription.DESCRIPTION_FILE_NAME),
				PROJECT_FILE.getBytes(StandardCharsets.UTF_8));
		Files.write(projectPath.resolve("test.txt"),
				"Test".getBytes(StandardCharsets.UTF_8));
		try (Git git = new Git(repository)) {
			git.add()
					.addFilepattern(PROJECT_NAME + '/'
							+ IProjectDescription.DESCRIPTION_FILE_NAME)
					.addFilepattern(PROJECT_NAME + "/test.txt").call();
			RevCommit initial = git.commit().setMessage("Initial commit")
					.call();
			// Do something that will fire a WorkingTreeModifiedEvent, like a
			// hard reset
			Files.write(projectPath.resolve("test.txt"),
					"Test again".getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING);
			git.add().addFilepattern(PROJECT_NAME + "/test.txt").call();
			git.commit().setMessage("Second commit").call();
			git.reset().setMode(ResetType.HARD).setRef(initial.getName())
					.call();
		}
	}

	@Test
	public void testManualImport() throws Exception {
		RepositoryUtil.INSTANCE
				.addConfiguredRepository(repoPath);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription desc = workspace
				.newProjectDescription(PROJECT_NAME);
		desc.setLocationURI(URIUtil.toURI(projectPath.toString()));
		workspace.run(monitor -> {
			SubMonitor progress = SubMonitor.convert(monitor, 2);
			simpleProject = workspace.getRoot().getProject(PROJECT_NAME);
			simpleProject.create(desc, progress.newChild(1));
			simpleProject.open(progress.newChild(1));
		}, null, IWorkspace.AVOID_UPDATE, null);
		RepositoryCache knownRepos = RepositoryCache.INSTANCE;
		ConnectProviderOperation connect = new ConnectProviderOperation(
				simpleProject, repoPath);
		connect.setRefreshResources(false);
		connect.execute(null);
		RepositoryMapping mapping = RepositoryMapping.getMapping(simpleProject);
		assertNotNull(mapping);
		Repository repo = knownRepos.getRepository(repoPath);
		assertNotNull(repo);
		IndexDiffCacheEntry cache = IndexDiffCache.INSTANCE
				.getIndexDiffCacheEntry(repo);
		assertNotNull(cache);
		// Verify that doing something that triggers a resource change event
		// updates the index diff.
		IndexDiffData[] diff = { null };
		IndexDiffChangedListener listener = (r, d) -> {
			if (diff[0] == null) {
				diff[0] = d;
			}
		};
		try {
			cache.addIndexDiffChangedListener(listener);
			workspace.run(monitor -> {
				IFile newFile = simpleProject.getFile("newfile.txt");
				try (InputStream content = new ByteArrayInputStream(
						"newContent".getBytes(StandardCharsets.UTF_8))) {
					newFile.create(content, false, monitor);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}, null, IWorkspace.AVOID_UPDATE, null);
			TestUtils.waitForJobs(5000, JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		} finally {
			cache.removeIndexDiffChangedListener(listener);
		}
		assertNotNull(diff[0]);
		assertTrue(
				diff[0].getUntracked().contains(PROJECT_NAME + "/newfile.txt"));
	}
}
