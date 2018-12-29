/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.attributes.FilterCommand;
import org.eclipse.jgit.attributes.FilterCommandFactory;
import org.eclipse.jgit.attributes.FilterCommandRegistry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.IO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for reading blobs from a commit with .gitattributes support.
 */
public class CommitFileRevisionTest extends GitTestCase {

	Repository repository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		repository = FileRepositoryBuilder.create(gitDir);
		repository.create();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		repository.close();
		super.tearDown();
	}

	private java.nio.file.Path createFile(IProject base, String name,
			String content) throws IOException {
		java.nio.file.Path path = base.getLocation().toFile().toPath()
				.resolve(name);
		Files.write(path, content.getBytes(StandardCharsets.UTF_8));
		return path;
	}

	private RevCommit setupFilter(Repository repo, IProject base,
			boolean commit) throws Exception {
		createFile(base, ".gitattributes", "*.txt filter=test");
		String builtinCommandName = "egit://builtin/test/smudge";
		FilterCommandRegistry.register(builtinCommandName,
				new TestCommandFactory('a', 'x'));
		StoredConfig config = repo.getConfig();
		config.setString("filter", "test", "smudge", builtinCommandName);
		config.save();
		if (commit) {
			try (Git git = new Git(repo)) {
				git.add().addFilepattern(".").call();
				return git.commit().setMessage("Add .gitattributes").call();
			}
		}
		return null;
	}

	@Test
	public void testWithAttributesCheckedIn() throws Exception {
		java.nio.file.Path filePath = createFile(project.getProject(),
				"attr.txt", "a");
		try (Git git = new Git(repository)) {
			git.add().addFilepattern(".").call();
			git.commit().setMessage("Initial commit").call();
			// Verify that we do have "a" in the repo: modify the file, do a
			// hard reset, verify the contents to be "a" again
			createFile(project.getProject(), "attr.txt", "aa");
			git.reset().setMode(ResetType.HARD).call();
			List<String> content = Files.readAllLines(filePath,
					StandardCharsets.UTF_8);
			assertEquals(1, content.size());
			assertEquals("a", content.get(0));
			// Now create a smudge filter that will replace all a's by x's, and
			// commit the .gitattributes file.
			RevCommit head = setupFilter(repository, project.getProject(),
					true);
			// Modify the file again and do a hard reset. We should end up with
			// a file containing "x".
			createFile(project.getProject(), "attr.txt", "aa");
			git.reset().setMode(ResetType.HARD).call();
			content = Files.readAllLines(filePath, StandardCharsets.UTF_8);
			assertEquals(1, content.size());
			assertEquals("x", content.get(0));
			// All right. Now get a CommitFileRevision and check its contents.
			String relativePath = getRevisionPath(filePath);
			CommitFileRevision fileRevision = new CommitFileRevision(repository,
					head, relativePath);
			ByteBuffer rawContent = null;
			try (InputStream blobStream = fileRevision
					.getStorage(new NullProgressMonitor()).getContents()) {
				rawContent = IO.readWholeStream(blobStream, 1);
			}
			assertNotNull(rawContent);
			String blobContent = new String(rawContent.array(), 0,
					rawContent.limit(), StandardCharsets.UTF_8);
			assertEquals("x", blobContent);
		} finally {
			FilterCommandRegistry.unregister("egit://builtin/test/smudge");
		}
	}

	@Test
	public void testWithAttributesNotCheckedIn() throws Exception {
		java.nio.file.Path filePath = createFile(project.getProject(),
				"attr.txt", "a");
		try (Git git = new Git(repository)) {
			git.add().addFilepattern(".").call();
			RevCommit head = git.commit().setMessage("Initial commit").call();
			// Verify that we do have "a" in the repo: modify the file, do a
			// hard reset, verify the contents to be "a" again
			createFile(project.getProject(), "attr.txt", "aa");
			git.reset().setMode(ResetType.HARD).call();
			List<String> content = Files.readAllLines(filePath,
					StandardCharsets.UTF_8);
			assertEquals(1, content.size());
			assertEquals("a", content.get(0));
			// Now create a smudge filter that will replace all a's by x's.
			setupFilter(repository, project.getProject(), false);
			// Modify the file again and do a hard reset. Interestingly we end
			// up with a file containing 'x': the checkout does apply the
			// not-yet-committed .gitattributes. I do not know whether that is
			// correct.
			createFile(project.getProject(), "attr.txt", "aa");
			git.reset().setMode(ResetType.HARD).call();
			content = Files.readAllLines(filePath, StandardCharsets.UTF_8);
			assertEquals(1, content.size());
			assertEquals("x", content.get(0));
			// All right. Now get a CommitFileRevision and check its contents.
			String relativePath = getRevisionPath(filePath);
			CommitFileRevision fileRevision = new CommitFileRevision(repository,
					head, relativePath);
			ByteBuffer rawContent = null;
			try (InputStream blobStream = fileRevision
					.getStorage(new NullProgressMonitor()).getContents()) {
				rawContent = IO.readWholeStream(blobStream, 1);
			}
			assertNotNull(rawContent);
			String blobContent = new String(rawContent.array(), 0,
					rawContent.limit(), StandardCharsets.UTF_8);
			// Prize question: what do we expect here? We explicitly say we
			// want to look at an older version. (OK, in this test it happens
			// to be HEAD, but that's immaterial.) I believe the correct
			// behavior is to apply only the attributes as they existed in that
			// commit, i.e., to disregard the .gitattributes in the file system.
			// Note, however, that this means that we get a different behavior
			// than if we checked out the commit. Also note that global/info
			// attributes get applied in any case -- that behavior is
			// unavoidable and appears to be wanted by the .gitattributes spec.
			assertEquals("a", blobContent);
		} finally {
			FilterCommandRegistry.unregister("egit://builtin/test/smudge");
		}
	}

	/**
	 * @param filePath
	 * @return path with {@code /} as separator
	 */
	private String getRevisionPath(Path filePath) {
		return repository.getWorkTree().toPath().relativize(filePath).toString()
				.replace('\\', '/');
	}

	@Test
	public void testWithAttributesNotCheckedInButWithGlobalAttributes()
			throws Exception {
		java.nio.file.Path filePath = createFile(project.getProject(),
				"attr.txt", "a");
		try (Git git = new Git(repository)) {
			git.add().addFilepattern(".").call();
			RevCommit head = git.commit().setMessage("Initial commit").call();
			// Verify that we do have "a" in the repo: modify the file, do a
			// hard reset, verify the contents to be "a" again
			createFile(project.getProject(), "attr.txt", "aa");
			git.reset().setMode(ResetType.HARD).call();
			List<String> content = Files.readAllLines(filePath,
					StandardCharsets.UTF_8);
			assertEquals(1, content.size());
			assertEquals("a", content.get(0));
			// Now create a smudge filter that will replace all a's by x's.
			setupFilter(repository, project.getProject(), false);
			// Set up the global attributes. For simplicity, we create the file
			// inside the work tree.
			java.nio.file.Path globalAttributes = createFile(
					project.getProject(), "globalattrs", "*.txt filter=test2");
			FilterCommandRegistry.register("egit://builtin/test/smudge2",
					new TestCommandFactory('a', 'y'));
			StoredConfig config = repository.getConfig();
			config.setString("core", null, "attributesFile",
					globalAttributes.toString());
			config.setString("filter", "test2", "smudge",
					"egit://builtin/test/smudge2");
			config.save();
			// Modify the file again and do a hard reset. File contains 'x'.
			createFile(project.getProject(), "attr.txt", "aa");
			git.reset().setMode(ResetType.HARD).call();
			content = Files.readAllLines(filePath, StandardCharsets.UTF_8);
			assertEquals(1, content.size());
			assertEquals("x", content.get(0));
			// All right. Now get a CommitFileRevision and check its contents.
			String relativePath = getRevisionPath(filePath);
			CommitFileRevision fileRevision = new CommitFileRevision(repository,
					head, relativePath);
			ByteBuffer rawContent = null;
			try (InputStream blobStream = fileRevision
					.getStorage(new NullProgressMonitor()).getContents()) {
				rawContent = IO.readWholeStream(blobStream, 1);
			}
			assertNotNull(rawContent);
			String blobContent = new String(rawContent.array(), 0,
					rawContent.limit(), StandardCharsets.UTF_8);
			// This should have ignored the not-yet-committed .gitignore and
			// applied the global smudge2 command.
			assertEquals("y", blobContent);
		} finally {
			FilterCommandRegistry.unregister("egit://builtin/test/smudge");
			FilterCommandRegistry.unregister("egit://builtin/test/smudge2");
		}
	}

	@Test
	public void testWithAttributesCheckedInAndWithGlobalAttributes()
			throws Exception {
		java.nio.file.Path filePath = createFile(project.getProject(),
				"attr.txt", "a");
		try (Git git = new Git(repository)) {
			git.add().addFilepattern(".").call();
			git.commit().setMessage("Initial commit").call();
			// Verify that we do have "a" in the repo: modify the file, do a
			// hard reset, verify the contents to be "a" again
			createFile(project.getProject(), "attr.txt", "aa");
			git.reset().setMode(ResetType.HARD).call();
			List<String> content = Files.readAllLines(filePath,
					StandardCharsets.UTF_8);
			assertEquals(1, content.size());
			assertEquals("a", content.get(0));
			// Now create a smudge filter that will replace all a's by x's.
			RevCommit head = setupFilter(repository, project.getProject(),
					true);
			// Set up the global attributes. For simplicity, we create the file
			// inside the work tree.
			java.nio.file.Path globalAttributes = createFile(
					project.getProject(), "globalattrs", "*.txt filter=test2");
			FilterCommandRegistry.register("egit://builtin/test/smudge2",
					new TestCommandFactory('a', 'y'));
			StoredConfig config = repository.getConfig();
			config.setString("core", null, "attributesFile",
					globalAttributes.toString());
			config.setString("filter", "test2", "smudge",
					"egit://builtin/test/smudge2");
			config.save();
			// Modify the file again and do a hard reset. File contains 'x'.
			createFile(project.getProject(), "attr.txt", "aa");
			git.reset().setMode(ResetType.HARD).call();
			content = Files.readAllLines(filePath, StandardCharsets.UTF_8);
			assertEquals(1, content.size());
			assertEquals("x", content.get(0));
			// All right. Now get a CommitFileRevision and check its contents.
			String relativePath = getRevisionPath(filePath);
			CommitFileRevision fileRevision = new CommitFileRevision(repository,
					head, relativePath);
			ByteBuffer rawContent = null;
			try (InputStream blobStream = fileRevision
					.getStorage(new NullProgressMonitor()).getContents()) {
				rawContent = IO.readWholeStream(blobStream, 1);
			}
			assertNotNull(rawContent);
			String blobContent = new String(rawContent.array(), 0,
					rawContent.limit(), StandardCharsets.UTF_8);
			assertEquals("x", blobContent);
		} finally {
			FilterCommandRegistry.unregister("egit://builtin/test/smudge");
			FilterCommandRegistry.unregister("egit://builtin/test/smudge2");
		}
	}

	private static class TestCommandFactory implements FilterCommandFactory {
		private final int toReplace;

		private final int replacement;

		public TestCommandFactory(int toReplace, int replacement) {
			this.toReplace = toReplace;
			this.replacement = replacement;
		}

		@Override
		public FilterCommand create(Repository repo, InputStream in,
				final OutputStream out) {
			FilterCommand cmd = new FilterCommand(in, out) {

				@Override
				public int run() throws IOException {
					int b = in.read();
					if (b == -1) {
						return b;
					} else if (b == toReplace) {
						out.write(replacement);
					} else {
						out.write(b);
					}
					return 1;
				}
			};
			return cmd;
		}
	}

}
