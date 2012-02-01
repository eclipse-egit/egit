/*******************************************************************************
 * Copyright (c) 2011, Tasktop Technologies
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Benjamin Muskalla (Tasktop Technologies) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.op.CreatePatchOperation;
import org.eclipse.egit.core.op.CreatePatchOperation.DiffHeaderFormat;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CreatePatchOperationTest extends GitTestCase {

	private static final String SIMPLE_GIT_PATCH_CONTENT = "From 6dcd097c7d39e9ba0b31a380981d3fb46017d6c2 Sat, 23 Jul 2011 20:33:24 -0330\n"
			+ "From: J. Git <j.git@egit.org>\n"
			+ "Date: Sat, 15 Aug 2009 20:12:58 -0330\n"
			+ "Subject: [PATCH] 2nd commit\n"
			+ "\n"
			+ "diff --git a/test-file b/test-file\n"
			+ "index e69de29..eb5f2c9 100644\n"
			+ "--- a/test-file\n"
			+ "+++ b/test-file\n"
			+ "@@ -0,0 +1 @@\n"
			+ "+another line\n"
			+ "\\ No newline at end of file";

	private static final String SIMPLE_ONELINE_PATCH_CONTENT = "6dcd097c7d39e9ba0b31a380981d3fb46017d6c2 2nd commit\n"
			+ "diff --git a/test-file b/test-file\n"
			+ "index e69de29..eb5f2c9 100644\n"
			+ "--- a/test-file\n"
			+ "+++ b/test-file\n"
			+ "@@ -0,0 +1 @@\n"
			+ "+another line\n"
			+ "\\ No newline at end of file";

	private static final String SIMPLE_PATCH_CONTENT = "diff --git a/test-file b/test-file\n"
			+ "index e69de29..eb5f2c9 100644\n"
			+ "--- a/test-file\n"
			+ "+++ b/test-file\n"
			+ "@@ -0,0 +1 @@\n"
			+ "+another line\n"
			+ "\\ No newline at end of file";

	private static final String SIMPLE_WORKSPACE_PATCH_CONTENT = "### Eclipse Workspace Patch 1.0\n"
			+ "#P Project-1\n"
			+ "diff --git test-file test-file\n"
			+ "index e69de29..eb5f2c9 100644\n"
			+ "--- test-file\n"
			+ "+++ test-file\n"
			+ "@@ -0,0 +1 @@\n"
			+ "+another line\n"
			+ "\\ No newline at end of file";

	private RevCommit commit;

	private File file;

	private TestRepository testRepository;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject().getLocationURI().getPath(),
				Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());

		file = testRepository.createFile(project.getProject(), "test-file");

		commit = testRepository.addAndCommit(project.getProject(), file,
				"new file");
	}

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	@Test
	public void testSimpleGitPatch() throws Exception {
		RevCommit secondCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "another line", "2nd commit");

		CreatePatchOperation operation = new CreatePatchOperation(
				testRepository.getRepository(), secondCommit);

		operation.execute(new NullProgressMonitor());

		String patchContent = operation.getPatchContent();
		assertNotNull(patchContent);
		assertGitPatch(SIMPLE_GIT_PATCH_CONTENT, patchContent);

		// repeat setting the header format explicitly
		operation = new CreatePatchOperation(
				testRepository.getRepository(), secondCommit);

		operation.setHeaderFormat(DiffHeaderFormat.EMAIL);
		operation.execute(new NullProgressMonitor());

		patchContent = operation.getPatchContent();
		assertNotNull(patchContent);
		assertGitPatch(SIMPLE_GIT_PATCH_CONTENT, patchContent);
	}

	@Test
	public void testSimplePatch() throws Exception {
		RevCommit secondCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "another line", "2nd commit");

		CreatePatchOperation operation = new CreatePatchOperation(
				testRepository.getRepository(), secondCommit);

		operation.setHeaderFormat(DiffHeaderFormat.NONE);
		operation.execute(new NullProgressMonitor());

		String patchContent = operation.getPatchContent();
		assertNotNull(patchContent);
		assertPatch(SIMPLE_PATCH_CONTENT, patchContent);
	}

	@Test
	public void testOnelineHeaderPatch() throws Exception {
		RevCommit secondCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "another line", "2nd commit");

		CreatePatchOperation operation = new CreatePatchOperation(
				testRepository.getRepository(), secondCommit);

		operation.setHeaderFormat(DiffHeaderFormat.ONELINE);
		operation.execute(new NullProgressMonitor());

		String patchContent = operation.getPatchContent();
		assertNotNull(patchContent);
		assertPatch(SIMPLE_ONELINE_PATCH_CONTENT, patchContent);
	}

	@Test(expected = IllegalStateException.class)
	public void testFirstCommit() throws Exception {
		CreatePatchOperation operation = new CreatePatchOperation(
				testRepository.getRepository(), commit);

		operation.execute(null);
	}

	@Test
	public void testNullCommit() throws Exception {
		new CreatePatchOperation(testRepository.getRepository(), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullRepo() throws Exception {
		new CreatePatchOperation(null, commit);
	}

	@Test(expected = IllegalStateException.class)
	public void testExecuteFirst() throws Exception {
		CreatePatchOperation operation = new CreatePatchOperation(
				testRepository.getRepository(), commit);
		operation.getPatchContent();
	}

	@Test
	public void testNullMonitor() throws Exception {
		RevCommit secondCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "another line", "2nd commit");
		CreatePatchOperation operation = new CreatePatchOperation(
				testRepository.getRepository(), secondCommit);
		operation.execute(null);
	}

	@Test
	public void testSuggestName() throws Exception {
		RevCommit aCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "another line", "2nd commit");
		assertEquals("2nd-commit.patch", CreatePatchOperation.suggestFileName(aCommit));

		aCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "another line", "[findBugs] Change visibility of repositoryFile to package");
		assertEquals("findBugs-Change-visibility-of-repositoryFile-to-pack.patch", CreatePatchOperation.suggestFileName(aCommit));

		aCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "another line", "Add collapse/expand all utility method for tree viewers.");
		assertEquals("Add-collapse-expand-all-utility-method-for-tree-view.patch", CreatePatchOperation.suggestFileName(aCommit));
	}

	@Test
	public void testProcessPath() throws Exception {
		DiffFormatter diffFmt = new DiffFormatter(null);
		IPath oldPath = CreatePatchOperation.processPath(
				new Path("a/test-file"), project.getProject(),
				new Path(diffFmt.getOldPrefix()));
		IPath newPath = CreatePatchOperation.processPath(
				new Path("b/test-file"), project.getProject(),
				new Path(diffFmt.getNewPrefix()));
		assertPatch("test-file", oldPath.toString());
		assertPatch("test-file", newPath.toString());
	}

	@Test
	public void testProcessPathRepoAboveProject() throws Exception {
		testRepository.disconnect(project.getProject());

		// new setUp
		project = new TestProject(true, "repo/bundles/Project-1", true, null);
		File repo = new File(project.getProject().getLocationURI().getPath()).getParentFile().getParentFile();
		gitDir = new File(repo, Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());


		DiffFormatter diffFmt = new DiffFormatter(null);
		IPath oldPath = CreatePatchOperation.processPath(new Path(
				"a/bundles/Project-1/test-file"), project.getProject(), new Path(
				diffFmt.getOldPrefix()));
		IPath newPath = CreatePatchOperation.processPath(new Path(
				"b/bundles/Project-1/test-file"), project.getProject(), new Path(
				diffFmt.getNewPrefix()));
		assertPatch("test-file", oldPath.toString());
		assertPatch("test-file", newPath.toString());
	}

	@Test
	public void testUpdateWorkspacePatchPrefixes() throws Exception {
		DiffFormatter diffFmt = new DiffFormatter(null);
		StringBuilder sb = new StringBuilder(SIMPLE_PATCH_CONTENT);
		CreatePatchOperation.updateWorkspacePatchPrefixes(sb, project
				.getProject().findMember("test-file"), diffFmt);
		// add workspace header
		StringBuilder sb1 = new StringBuilder("### Eclipse Workspace Patch 1.0\n#P ")
				.append(project.getProject().getName()).append("\n").append(sb);

		assertPatch(SIMPLE_WORKSPACE_PATCH_CONTENT, sb1.toString());
	}

	@Test
	public void testWorkspacePatch() throws Exception {
		RevCommit secondCommit = testRepository.appendContentAndCommit(
				project.getProject(), file, "another line", "2nd commit");

		CreatePatchOperation operation = new CreatePatchOperation(
				testRepository.getRepository(), secondCommit, project.getProject());

		operation.setHeaderFormat(DiffHeaderFormat.WORKSPACE);
		operation.execute(new NullProgressMonitor());

		String patchContent = operation.getPatchContent();
		assertNotNull(patchContent);
		assertPatch(SIMPLE_WORKSPACE_PATCH_CONTENT, patchContent);
	}

	private void assertGitPatch(String expected, String actual) {
		assertEquals(expected.substring(0,45), actual.substring(0,45));
		assertEquals(expected.substring(expected.indexOf("\n")), actual.substring(actual.indexOf("\n")));
	}

	private void assertPatch(String expected, String actual) {
		assertEquals(expected, actual);
	}

}
