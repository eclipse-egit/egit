/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for "Push Tags..." wizard.
 */
public class PushTagsWizardTest extends LocalRepositoryTestCase {

	private Repository repository;
	private Repository remoteRepository;

	@Before
	public void createRepositories() throws Exception {
		File repositoryFile = createProjectAndCommitToRepository();
		File remoteRepositoryFile = createRemoteRepository(repositoryFile);
		repository = lookupRepository(repositoryFile);
		remoteRepository = lookupRepository(remoteRepositoryFile);
	}

	@Test
	public void pushTag() throws Exception {
		try (Git git = new Git(repository)) {
			git.tag().setName("foo").setMessage("Foo tag").call();
		}

		PushTagsWizardTester wizard = PushTagsWizardTester
				.startWizard(selectProject());
		wizard.selectRemote("push");
		wizard.assertNextDisabled();
		wizard.checkTag("foo");
		wizard.next();
		wizard.finish();

		assertTagPushed("foo", remoteRepository);
	}

	private SWTBotTree selectProject() {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		return projectExplorerTree;
	}

	private void assertTagPushed(String tagName, Repository remoteRepo)
			throws Exception {
		ObjectId pushed = remoteRepo.resolve(tagName);
		assertNotNull("Expected '" + tagName
				+ "' to resolve to non-null ObjectId on remote repository",
				pushed);
		ObjectId local = repository.resolve(tagName);
		assertEquals(
				"Expected local tag to be the same as tag on remote after pushing",
				local, pushed);
	}
}
