/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.revision.EditableRevision;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.attributes.FilterCommand;
import org.eclipse.jgit.attributes.FilterCommandFactory;
import org.eclipse.jgit.attributes.FilterCommandRegistry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.util.IO;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for CompareUtils; in particular editing index content as it can be done
 * in the compare editor.
 */
public class CompareUtilsTest extends LocalRepositoryTestCase {

	private Repository repository;

	@Before
	public void setup() throws Exception {
		File repoFile = createProjectAndCommitToRepository();
		assertNotNull(repoFile);
		repository = Activator.getDefault().getRepositoryCache()
				.lookupRepository(repoFile);
		assertNotNull(repository);
	}

	private String get(InputStream in) throws IOException {
		ByteBuffer buffer = IO.readWholeStream(in, 1);
		return new String(buffer.array(), 0, buffer.limit(),
				StandardCharsets.UTF_8);
	}

	@Test
	public void testIndexEdit() throws Exception {
		IFile testFile = touch("a");
		stage(testFile);
		// Get the index file revision.
		ITypedElement element = CompareUtils.getIndexTypedElement(testFile);
		assert (element instanceof EditableRevision);
		EditableRevision revision = (EditableRevision) element;
		// Check that its contents are 'a'.
		try (InputStream in = revision.getContents()) {
			assertEquals("a", get(in));
		}
		// Change the contents to 'xx'
		revision.setContent("xx".getBytes(StandardCharsets.UTF_8));
		// Commit the index
		CommitOperation op = new CommitOperation(repository,
				TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER,
				"Commit modified index");
		op.execute(null);
		TestUtil.waitForJobs(50, 5000);
		// Do a reset --hard
		ResetOperation reset = new ResetOperation(repository, Constants.HEAD,
				ResetType.HARD);
		reset.execute(null);
		TestUtil.waitForJobs(50, 5000);
		// Should have 'xx' now
		try (InputStream in = testFile.getContents()) {
			assertEquals("xx", get(in));
		}
	}

	@Test
	public void testIndexEditWithAttributes() throws Exception {
		IFile testFile = touch("a");
		stage(testFile);
		// Set up .gitattributes such that 'a's are changed to 'x' on smudge
		IFile gitAttributes = touch(PROJ1, FOLDER + "/.gitattributes",
				FILE1 + " filter=test");
		try {
			FilterCommandRegistry.register("egitui://builtin/test/smudge",
					new TestCommandFactory('a', 'x'));
			StoredConfig config = repository.getConfig();
			config.setString("filter", "test", "smudge",
					"egitui://builtin/test/smudge");
			config.save();
			// Get the index file revision.
			ITypedElement element = CompareUtils.getIndexTypedElement(testFile);
			assert (element instanceof EditableRevision);
			EditableRevision revision = (EditableRevision) element;
			// Check that its contents are 'x'.
			try (InputStream in = revision.getContents()) {
				assertEquals("x", get(in));
			}
			// Modify the filter to transform 'x' to 'a' on clean
			FilterCommandRegistry.register("egitui://builtin/test/clean",
					new TestCommandFactory('x', 'a'));
			config.setString("filter", "test", "clean",
					"egitui://builtin/test/clean");
			config.save();
			// Change the contents to 'xx'. This should apply the above clean
			// filter.
			revision.setContent("xx".getBytes(StandardCharsets.UTF_8));
			// Commit the index
			CommitOperation op = new CommitOperation(repository,
					TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER,
					"Commit modified index");
			op.execute(null);
			TestUtil.waitForJobs(50, 5000);
			// Remove filters
			gitAttributes.delete(true, true, new NullProgressMonitor());
			config.unsetSection("filter", "test");
			config.save();
			// Do a reset --hard
			ResetOperation reset = new ResetOperation(repository,
					Constants.HEAD, ResetType.HARD);
			reset.execute(null);
			TestUtil.waitForJobs(50, 5000);
			// Should have 'aa' now since we never committed the .gitattributes
			try (InputStream in = testFile.getContents()) {
				assertEquals("aa", get(in));
			}
		} finally {
			FilterCommandRegistry.unregister("egitui://builtin/test/smudge");
			FilterCommandRegistry.unregister("egitui://builtin/test/clean");
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
