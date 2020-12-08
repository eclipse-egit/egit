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
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
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
		Repository repo = repository1.getRepository();
		assertTrue("Tags should be empty", repo.getRefDatabase()
				.getRefsByPrefix(Constants.R_TAGS).isEmpty());
		PersonIdent author = RawParseUtils.parsePersonIdent(TestUtils.AUTHOR);
		TagOperation top = new TagOperation(repo)
				.setAnnotated(true)
				.setForce(false).setName("TheNewTag")
				.setMessage("Well, I'm the tag")
				.setTagger(author)
				.setTarget(repo.parseCommit(repo.resolve("refs/heads/master")));
		top.execute(new NullProgressMonitor());
		assertFalse("Tags should not be empty", repo.getRefDatabase()
				.getRefsByPrefix(Constants.R_TAGS).isEmpty());

		// Execute it again: should create an identical tag object with the same
		// hash and be allowed
		top.execute(null);

		// Set the force flag and re-execute; should also be allowed.
		top.setForce(true);
		top.execute(null);

		// Change the message (force flag is still set)
		try (RevWalk walk = new RevWalk(repo)) {
			RevTag tag = walk
					.parseTag(repo.resolve(Constants.R_TAGS + "TheNewTag"));

			top.setMessage("Another message");
			assertFalse("Messages should differ",
					tag.getFullMessage().equals(top.getMessage()));
			top.execute(null);
			tag = walk.parseTag(repo.resolve(Constants.R_TAGS + "TheNewTag"));
			assertTrue("Messages be same",
					tag.getFullMessage().equals(top.getMessage()));
		}
	}

	@Test
	public void addEmptyAnnotatedTag() throws Exception {
		Repository repo = repository1.getRepository();
		assertTrue("Tags should be empty", repo.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).isEmpty());
		RevCommit commit = repo.parseCommit(repo.resolve("refs/heads/master"));
		TagOperation top = new TagOperation(repo)
				.setName("TheNewTag")
				.setAnnotated(true)
				.setMessage("")
				.setTagger(RawParseUtils.parsePersonIdent(TestUtils.AUTHOR))
				.setTarget(repo.parseCommit(repo.resolve("refs/heads/master")));
		top.execute(new NullProgressMonitor());
		assertFalse("Tags should not be empty", repo.getRefDatabase()
				.getRefsByPrefix(Constants.R_TAGS).isEmpty());
		assertIsAnnotated("TheNewTag", commit, "");
	}

	@Test
	public void addNullAnnotatedTag() throws Exception {
		Repository repo = repository1.getRepository();
		assertTrue("Tags should be empty", repo.getRefDatabase()
				.getRefsByPrefix(Constants.R_TAGS).isEmpty());
		RevCommit commit = repo.parseCommit(repo.resolve("refs/heads/master"));
		TagOperation top = new TagOperation(repo)
				.setName("TheNewTag")
				.setAnnotated(true)
				.setTagger(RawParseUtils.parsePersonIdent(TestUtils.AUTHOR))
				.setTarget(repo.parseCommit(repo.resolve("refs/heads/master")));
		top.execute(new NullProgressMonitor());
		assertFalse("Tags should not be empty", repo.getRefDatabase()
				.getRefsByPrefix(Constants.R_TAGS).isEmpty());
		assertIsAnnotated("TheNewTag", commit, "");
	}

	@Test
	public void addLightweightTag() throws Exception {
		Repository repo = repository1.getRepository();
		assertTrue("Tags should be empty", repo.getRefDatabase()
				.getRefsByPrefix(Constants.R_TAGS).isEmpty());
		RevCommit commit = repo.parseCommit(repo.resolve("refs/heads/master"));
		TagOperation top = new TagOperation(repo)
				.setName("TheNewTag")
				.setAnnotated(false)
				.setTagger(RawParseUtils.parsePersonIdent(TestUtils.AUTHOR))
				.setTarget(repo.parseCommit(repo.resolve("refs/heads/master")));
		top.execute(new NullProgressMonitor());
		assertFalse("Tags should not be empty", repo.getRefDatabase()
				.getRefsByPrefix(Constants.R_TAGS).isEmpty());
		assertIsLightweight("TheNewTag", commit);
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
