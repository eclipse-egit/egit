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
package org.eclipse.egit.core.internal.test.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.op.CreatePatchOperation;
import org.eclipse.egit.core.internal.op.CreatePatchOperation.DiffHeaderFormat;
import org.eclipse.egit.core.internal.test.GitTestCase;
import org.eclipse.egit.core.internal.test.TestProject;
import org.eclipse.egit.core.internal.test.TestRepository;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FileUtils;
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
			+ "diff --git deleted-file deleted-file\n"
			+ "deleted file mode 100644\n"
			+ "index e69de29..0000000\n"
			+ "--- deleted-file\n"
			+ "+++ /dev/null\n"
			+ "diff --git new-file new-file\n"
			+ "new file mode 100644\n"
			+ "index 0000000..47d2739\n"
			+ "--- /dev/null\n"
			+ "+++ new-file\n"
			+ "@@ -0,0 +1 @@\n"
			+ "+new content\n"
			+ "\\ No newline at end of file\n"
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
	public void testComputeWorkspacePath() throws Exception {
		IPath oldPath = CreatePatchOperation.computeWorkspacePath(new Path(
				"test-file"), project.getProject());
		IPath newPath = CreatePatchOperation.computeWorkspacePath(new Path(
				"test-file"), project.getProject());
		assertPatch("test-file", oldPath.toString());
		assertPatch("test-file", newPath.toString());
	}

	@Test
	public void testComputeWorkspacePathRepoAboveProject() throws Exception {
		testRepository.disconnect(project.getProject());

		// new setup
		project = new TestProject(true, "repo/bundles/Project-1", true, null);
		File repo = new File(project.getProject().getLocationURI().getPath())
				.getParentFile().getParentFile();
		gitDir = new File(repo, Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());

		IPath oldPath = CreatePatchOperation.computeWorkspacePath(new Path(
				"bundles/Project-1/test-file"), project.getProject());
		IPath newPath = CreatePatchOperation.computeWorkspacePath(new Path(
				"bundles/Project-1/test-file"), project.getProject());
		assertPatch("test-file", oldPath.toString());
		assertPatch("test-file", newPath.toString());
	}

	@Test
	public void testUpdateWorkspacePatchPrefixes() throws Exception {
		// setup workspace
		File newFile = testRepository.createFile(project.getProject(), "new-file");
		testRepository.appendFileContent(newFile, "new content");
		File deletedFile = testRepository.createFile(project.getProject(), "deleted-file");
		commit = testRepository.addAndCommit(project.getProject(), deletedFile,
				"whatever");
		FileUtils.delete(deletedFile);

		// unprocessed patch
		DiffFormatter diffFmt = new DiffFormatter(null);
		diffFmt.setRepository(testRepository.getRepository());
		StringBuilder sb = new StringBuilder();
		sb.append("diff --git a/deleted-file b/deleted-file").append("\n");
		sb.append("deleted file mode 100644").append("\n");
		sb.append("index e69de29..0000000").append("\n");
		sb.append("--- a/deleted-file").append("\n");
		sb.append("+++ /dev/null").append("\n");
		sb.append("diff --git a/new-file b/new-file").append("\n");
		sb.append("new file mode 100644").append("\n");
		sb.append("index 0000000..47d2739").append("\n");
		sb.append("--- /dev/null").append("\n");
		sb.append("+++ b/new-file").append("\n");
		sb.append("@@ -0,0 +1 @@").append("\n");
		sb.append("+new content").append("\n");
		sb.append("\\ No newline at end of file").append("\n");
		sb.append(SIMPLE_PATCH_CONTENT);

		// update patch
		CreatePatchOperation op = new CreatePatchOperation(testRepository.getRepository(), null);
		op.updateWorkspacePatchPrefixes(sb, diffFmt);
		// add workspace header
		StringBuilder sb1 = new StringBuilder("### Eclipse Workspace Patch 1.0\n#P ")
				.append(project.getProject().getName()).append("\n").append(sb);

		assertPatch(SIMPLE_WORKSPACE_PATCH_CONTENT, sb1.toString());
	}

	@Test
	public void testWorkspacePatchForCommit() throws Exception {
		// setup workspace
		File deletedFile = testRepository.createFile(project.getProject(), "deleted-file");
		commit = testRepository.addAndCommit(project.getProject(), deletedFile,
				"whatever");
		FileUtils.delete(deletedFile);
		testRepository.appendFileContent(file, "another line");
		File newFile = testRepository.createFile(project.getProject(), "new-file");
		testRepository.appendFileContent(newFile, "new content");
		testRepository.untrack(deletedFile);
		testRepository.track(file);
		testRepository.track(newFile);
		commit = testRepository.commit("2nd commit");

		// create patch
		CreatePatchOperation operation = new CreatePatchOperation(
				testRepository.getRepository(), commit);

		operation.setHeaderFormat(DiffHeaderFormat.WORKSPACE);
		operation.execute(new NullProgressMonitor());

		assertPatch(SIMPLE_WORKSPACE_PATCH_CONTENT, operation.getPatchContent());
	}

	@Test
	public void testWorkspacePatchForWorkingDir() throws Exception {
		// setup workspace
		testRepository.addToIndex(project.getProject().findMember(".classpath"));
		testRepository.addToIndex(project.getProject().findMember(".project"));
		testRepository.commit("commit all");
		testRepository.appendFileContent(file, "another line");
		File newFile = testRepository.createFile(project.getProject(), "new-file");
		testRepository.appendFileContent(newFile, "new content");
		File deletedFile = testRepository.createFile(project.getProject(), "deleted-file");
		commit = testRepository.addAndCommit(project.getProject(), deletedFile,
				"whatever");
		FileUtils.delete(deletedFile);

		// create patch
		CreatePatchOperation operation = new CreatePatchOperation(
				testRepository.getRepository(), null);

		operation.setHeaderFormat(DiffHeaderFormat.WORKSPACE);
		operation.execute(new NullProgressMonitor());

		assertPatch(SIMPLE_WORKSPACE_PATCH_CONTENT, operation.getPatchContent());
	}

	private void assertGitPatch(String expected, String actual) {
		assertEquals(expected.substring(0,45), actual.substring(0,45));
		assertEquals(expected.substring(expected.indexOf("\n")), actual.substring(actual.indexOf("\n")));
	}

	private void assertPatch(String expected, String actual) {
		assertEquals(expected, actual);
	}

}
