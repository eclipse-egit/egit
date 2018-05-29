/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.junit.JGitTestUtil.deleteTrashFile;
import static org.eclipse.jgit.junit.JGitTestUtil.writeTrashFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.jgit.api.Git;
import org.junit.Test;

@SuppressWarnings("boxing")
public class WorkingTreeChangeCacheTest extends AbstractCacheTest {

	@Test
	public void shouldListSingleWorkspaceAddition() throws Exception {
		// given
		writeTrashFile(db, "a.txt", "trash");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileAddition(result, "a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceAdditions() throws Exception {
		// given
		writeTrashFile(db, "a.txt", "trash");
		writeTrashFile(db, "b.txt", "trash");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileAddition(result, "a.txt", "a.txt");
		assertFileAddition(result, "b.txt", "b.txt");
	}

	@Test
	public void shouldListSingleWorkspaceAdditionInFolder() throws Exception {
		// given
		writeTrashFile(db, "folder/a.txt", "trash");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileAddition(result, "folder/a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceAdditionsInFolder() throws Exception {
		// given
		writeTrashFile(db, "folder/a.txt", "trash");
		writeTrashFile(db, "folder/b.txt", "trash");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileAddition(result, "folder/a.txt", "a.txt");
		assertFileAddition(result, "folder/b.txt", "b.txt");
	}

	@Test
	public void shouldListSingleWorkspaceDeletion() throws Exception {
		// given
		writeTrashFile(db, "a.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("a.txt").call();
		}
		deleteTrashFile(db, "a.txt");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileDeletion(result, "a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceDeletions() throws Exception {
		// given
		writeTrashFile(db, "a.txt", "trash");
		writeTrashFile(db, "b.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("a.txt").addFilepattern("b.txt").call();
		}
		deleteTrashFile(db, "a.txt");
		deleteTrashFile(db, "b.txt");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileDeletion(result, "a.txt", "a.txt");
		assertFileDeletion(result, "b.txt", "b.txt");
	}

	@Test
	public void shouldListSingleWorkspaceDeletionInFolder() throws Exception {
		// given
		writeTrashFile(db, "folder/a.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("folder/a.txt").call();
		}
		deleteTrashFile(db, "folder/a.txt");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileDeletion(result, "folder/a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceDeletionsInFolder() throws Exception {
		// given
		writeTrashFile(db, "folder/a.txt", "trash");
		writeTrashFile(db, "folder/b.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("folder/a.txt")
					.addFilepattern("folder/b.txt").call();
		}
		deleteTrashFile(db, "folder/a.txt");
		deleteTrashFile(db, "folder/b.txt");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileDeletion(result, "folder/a.txt", "a.txt");
		assertFileDeletion(result, "folder/b.txt", "b.txt");
	}

	@Test
	public void shouldListSingleWorkspaceChange() throws Exception {
		// given
		writeTrashFile(db, "a.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("a.txt").call();
		}
		writeTrashFile(db, "a.txt", "modification");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileChange(result, "a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceChanges() throws Exception {
		// given
		writeTrashFile(db, "a.txt", "trash");
		writeTrashFile(db, "b.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("a.txt").addFilepattern("b.txt").call();
		}
		writeTrashFile(db, "a.txt", "modification");
		writeTrashFile(db, "b.txt", "modification");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileChange(result, "a.txt", "a.txt");
		assertFileChange(result, "b.txt", "b.txt");
	}

	@Test
	public void shouldListSingleWorkspaceChangeInFolder() throws Exception {
		// given
		writeTrashFile(db, "folder/a.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("folder/a.txt").call();
		}
		writeTrashFile(db, "folder/a.txt", "modification");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileChange(result, "folder/a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceChagneInFolder() throws Exception {
		// given
		writeTrashFile(db, "folder/a.txt", "trash");
		writeTrashFile(db, "folder/b.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("folder/a.txt")
					.addFilepattern("folder/b.txt").call();
		}
		writeTrashFile(db, "folder/a.txt", "modification");
		writeTrashFile(db, "folder/b.txt", "modification");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileChange(result, "folder/a.txt", "a.txt");
		assertFileChange(result, "folder/b.txt", "b.txt");
	}

	@Test
	public void shouldNotListIgnorefFile() throws Exception {
		// given
		writeTrashFile(db, "a.txt", "content");
		writeTrashFile(db, ".gitignore", "a.txt");

		// when
		Map<String, Change> result = WorkingTreeChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileAddition(result, ".gitignore", ".gitignore");
	}

}
