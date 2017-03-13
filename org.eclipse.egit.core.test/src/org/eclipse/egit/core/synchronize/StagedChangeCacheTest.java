/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.junit.JGitTestUtil.writeTrashFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.jgit.api.Git;
import org.junit.Test;

@SuppressWarnings("boxing")
public class StagedChangeCacheTest extends AbstractCacheTest {

	@Test
	public void shouldListSingleWorkspaceAddition() throws Exception {
		// given
		writeTrashFile(db, "a.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("a.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileAddition(result, "a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceAdditions() throws Exception {
		// given
		writeTrashFile(db, "a.txt", "trash");
		writeTrashFile(db, "b.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("a.txt").addFilepattern("b.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileAddition(result, "a.txt", "a.txt");
		assertFileAddition(result, "b.txt", "b.txt");
	}

	@Test
	public void shouldListSingleWorkspaceAdditionInFolder() throws Exception {
		// given
		writeTrashFile(db, "folder/a.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("folder/a.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileAddition(result, "folder/a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceAdditionsInFolder() throws Exception {
		// given
		writeTrashFile(db, "folder/a.txt", "trash");
		writeTrashFile(db, "folder/b.txt", "trash");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("folder/a.txt")
					.addFilepattern("folder/b.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileAddition(result, "folder/a.txt", "a.txt");
		assertFileAddition(result, "folder/b.txt", "b.txt");
	}

	@Test
	public void shouldListSingleWorkspaceDeletion() throws Exception {
		// given
		try (Git git = new Git(db)) {
			writeTrashFile(db, "a.txt", "trash");
			git.add().addFilepattern("a.txt").call();
			git.commit().setMessage("initial add").call();
			git.rm().addFilepattern("a.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileDeletion(result, "a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceDeletions() throws Exception {
		// given
		try (Git git = new Git(db)) {
			writeTrashFile(db, "a.txt", "trash");
			writeTrashFile(db, "b.txt", "trash");
			git.add().addFilepattern("a.txt").addFilepattern("b.txt").call();
			git.commit().setMessage("new commit").call();
			git.rm().addFilepattern("a.txt").addFilepattern("b.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileDeletion(result, "a.txt", "a.txt");
		assertFileDeletion(result, "b.txt", "b.txt");
	}

	@Test
	public void shouldListSingleWorkspaceDeletionInFolder() throws Exception {
		// given
		try (Git git = new Git(db)) {
			writeTrashFile(db, "folder/a.txt", "trash");
			git.add().addFilepattern("folder/a.txt").call();
			git.commit().setMessage("new commit").call();
			git.rm().addFilepattern("folder/a.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileDeletion(result, "folder/a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceDeletionsInFolder() throws Exception {
		// given
		try (Git git = new Git(db)) {
			writeTrashFile(db, "folder/a.txt", "trash");
			writeTrashFile(db, "folder/b.txt", "trash");
			git.add().addFilepattern("folder/a.txt")
					.addFilepattern("folder/b.txt").call();
			git.commit().setMessage("new commit").call();
			git.rm().addFilepattern("folder/a.txt").call();
			git.rm().addFilepattern("folder/b.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileDeletion(result, "folder/a.txt", "a.txt");
		assertFileDeletion(result, "folder/b.txt", "b.txt");
	}

	@Test
	public void shouldListSingleWorkspaceChange() throws Exception {
		// given
		try (Git git = new Git(db)) {
			writeTrashFile(db, "a.txt", "trash");
			git.add().addFilepattern("a.txt").call();
			git.commit().setMessage("initial a.txt commit").call();
			writeTrashFile(db, "a.txt", "modification");
			git.add().addFilepattern("a.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileChange(result, "a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceChanges() throws Exception {
		// given
		try (Git git = new Git(db)) {
			writeTrashFile(db, "a.txt", "trash");
			writeTrashFile(db, "b.txt", "trash");
			git.add().addFilepattern("a.txt").addFilepattern("b.txt").call();
			git.commit().setMessage("new commmit").call();
			writeTrashFile(db, "a.txt", "modification");
			writeTrashFile(db, "b.txt", "modification");
			git.add().addFilepattern("a.txt").addFilepattern("b.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileChange(result, "a.txt", "a.txt");
		assertFileChange(result, "b.txt", "b.txt");
	}

	@Test
	public void shouldListSingleWorkspaceChangeInFolder() throws Exception {
		// given
		try (Git git = new Git(db)) {
			writeTrashFile(db, "folder/a.txt", "trash");
			git.add().addFilepattern("folder/a.txt").call();
			git.commit().setMessage("new commit").call();
			writeTrashFile(db, "folder/a.txt", "modification");
			git.add().addFilepattern("folder/a.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(1));
		assertFileChange(result, "folder/a.txt", "a.txt");
	}

	@Test
	public void shouldListTwoWorkspaceChagneInFolder() throws Exception {
		// given
		try (Git git = new Git(db)) {
			writeTrashFile(db, "folder/a.txt", "trash");
			writeTrashFile(db, "folder/b.txt", "trash");
			git.add().addFilepattern("folder/a.txt")
					.addFilepattern("folder/b.txt").call();
			git.commit().setMessage("new commit").call();
			writeTrashFile(db, "folder/a.txt", "modification");
			writeTrashFile(db, "folder/b.txt", "modification");
			git.add().addFilepattern("folder/a.txt")
					.addFilepattern("folder/b.txt").call();
		}

		// when
		Map<String, Change> result = StagedChangeCache.build(db);

		// then
		assertThat(result.size(), is(2));
		assertFileChange(result, "folder/a.txt", "a.txt");
		assertFileChange(result, "folder/b.txt", "b.txt");
	}

}
