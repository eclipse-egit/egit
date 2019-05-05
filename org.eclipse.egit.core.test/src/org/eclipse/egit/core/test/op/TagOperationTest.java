/*******************************************************************************
 * Copyright (C) 2010, 2012 Chris Aniszczyk <caniszczyk@gmail.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.core.test.DualRepositoryTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.RawParseUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TagOperationTest extends DualRepositoryTestCase {

	File workdir;

	String projectName = "TagTest";

	IProject project;

	@Before
	public void setUp() throws Exception {

		workdir = testUtils.createTempDir("Repository1");

		repository1 = new TestRepository(new File(workdir, Constants.DOT_GIT));

		project = testUtils.createProjectInLocalFileSystem(workdir,
				projectName);
		testUtils.addFileToProject(project, "folder1/file1.txt", "Hello world");

		repository1.connect(project);
		repository1.trackAllFiles(project);
		repository1.commit("Initial commit");
	}

	@After
	public void tearDown() throws Exception {
		project.close(null);
		project.delete(false, false, null);
		project = null;
		// repositories and tempDirs are deleted in superclass
	}

	@Test
	public void addTag() throws Exception {
		assertTrue("Tags should be empty", repository1.getRepository()
				.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).isEmpty());
		TagBuilder newTag = new TagBuilder();
		newTag.setTag("TheNewTag");
		newTag.setMessage("Well, I'm the tag");
		newTag.setTagger(RawParseUtils.parsePersonIdent(TestUtils.AUTHOR));
		newTag.setObjectId(repository1.getRepository()
				.resolve("refs/heads/master"), Constants.OBJ_COMMIT);
		TagOperation top = new TagOperation(repository1.getRepository(),
				newTag, false);
		top.execute(new NullProgressMonitor());
		assertFalse("Tags should not be empty", repository1.getRepository()
				.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).isEmpty());

		top.execute(null);
		assertEquals(top.getResult(), RefUpdate.Result.NO_CHANGE);

		top = new TagOperation(repository1.getRepository(), newTag, true);

		top.execute(null);
		assertEquals(top.getResult(), RefUpdate.Result.NO_CHANGE);

		try (RevWalk walk = new RevWalk(repository1.getRepository())) {
			RevTag tag = walk.parseTag(repository1.getRepository().resolve(
					Constants.R_TAGS + "TheNewTag"));

			newTag.setMessage("Another message");
			assertFalse("Messages should differ",
					tag.getFullMessage().equals(newTag.getMessage()));
			top.execute(null);
			tag = walk.parseTag(repository1.getRepository().resolve(
					Constants.R_TAGS + "TheNewTag"));
			assertTrue("Messages be same",
					tag.getFullMessage().equals(newTag.getMessage()));
			walk.dispose();
		}
	}

	@Test
	public void addEmptyAnnotatedTag() throws Exception {
		assertTrue("Tags should be empty", repository1.getRepository()
				.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).isEmpty());
		TagBuilder newTag = new TagBuilder();
		newTag.setTag("TheNewTag");
		newTag.setMessage("");
		newTag.setTagger(RawParseUtils.parsePersonIdent(TestUtils.AUTHOR));
		ObjectId headCommit = repository1.getRepository()
				.resolve("refs/heads/master");
		newTag.setObjectId(headCommit, Constants.OBJ_COMMIT);
		TagOperation top = new TagOperation(repository1.getRepository(), newTag,
				false, true);
		top.execute(new NullProgressMonitor());
		assertFalse("Tags should not be empty", repository1.getRepository()
				.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).isEmpty());
		assertIsAnnotated("TheNewTag", headCommit, "");
	}

	@Test
	public void addNullAnnotatedTag() throws Exception {
		assertTrue("Tags should be empty", repository1.getRepository()
				.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).isEmpty());
		TagBuilder newTag = new TagBuilder();
		newTag.setTag("TheNewTag");
		newTag.setTagger(RawParseUtils.parsePersonIdent(TestUtils.AUTHOR));
		ObjectId headCommit = repository1.getRepository()
				.resolve("refs/heads/master");
		newTag.setObjectId(headCommit, Constants.OBJ_COMMIT);
		TagOperation top = new TagOperation(repository1.getRepository(), newTag,
				false, true);
		top.execute(new NullProgressMonitor());
		assertFalse("Tags should not be empty", repository1.getRepository()
				.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).isEmpty());
		assertIsAnnotated("TheNewTag", headCommit, null);
	}

	@Test
	public void addLightweightTag() throws Exception {
		assertTrue("Tags should be empty", repository1.getRepository()
				.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).isEmpty());
		TagBuilder newTag = new TagBuilder();
		newTag.setTag("TheNewTag");
		newTag.setTagger(RawParseUtils.parsePersonIdent(TestUtils.AUTHOR));
		ObjectId headCommit = repository1.getRepository()
				.resolve("refs/heads/master");
		newTag.setObjectId(headCommit, Constants.OBJ_COMMIT);
		TagOperation top = new TagOperation(repository1.getRepository(), newTag,
				false, false);
		top.execute(new NullProgressMonitor());
		assertFalse("Tags should not be empty", repository1.getRepository()
				.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).isEmpty());
		assertIsLightweight("TheNewTag", headCommit);
	}

	private void assertIsAnnotated(String tag, ObjectId target, String message)
			throws Exception {
		Ref ref = repository1.getRepository().exactRef(Constants.R_TAGS + tag);
		ObjectId obj = ref.getObjectId();
		try (RevWalk walk = new RevWalk(repository1.getRepository())) {
			RevTag t = walk.parseTag(obj);
			if (message != null) {
				assertEquals("Unexpected tag message", message,
						t.getFullMessage());
			}
			assertEquals("Unexpected commit for tag " + t.getName(), target,
					walk.peel(t));
		}
	}

	private void assertIsLightweight(String tag, ObjectId target)
			throws Exception {
		Ref ref = repository1.getRepository().exactRef(Constants.R_TAGS + tag);
		ObjectId obj = ref.getObjectId();
		assertEquals("Unexpected commit for tag " + ref.getName(), target, obj);
	}

}
