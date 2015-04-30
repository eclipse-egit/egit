/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import static org.eclipse.jgit.lib.Constants.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.After;
import org.junit.Before;

abstract public class AbstractGitFlowOperationTest extends GitTestCase {
	protected TestRepository testRepository;

	protected static final String MY_FEATURE = "myFeature";

	protected static final String MY_RELEASE = "myRelease";

	protected static final String MY_VERSION_TAG = "v";

	protected static final String MY_MASTER = "master";

	protected static final String MY_HOTFIX = "myHotfix";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject().getLocationURI().getPath(),
				DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());
	}

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	protected Ref findBranch(Repository repository, String branchName)
			throws IOException {
		return repository.getRef(R_HEADS + branchName);
	}

	protected RevCommit findCommit(Repository repo, ObjectId head)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		try (RevWalk revWalk = new RevWalk(repo)) {
			RevCommit result = revWalk.parseCommit(head);
			return result;
		}
	}

	protected RevCommit addFileAndCommit(String fileName, String commitMessage)
			throws Exception, UnsupportedEncodingException {
		IFile file = project.createFile(fileName,
				"Hello, world".getBytes("UTF-8"));
		return testRepository.addAndCommit(project.project,
				new File(file.getLocationURI()), commitMessage);
	}
}