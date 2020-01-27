/*******************************************************************************
 * Copyright (C) 2011, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.ADDITION;
import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.CHANGE;
import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.DELETION;
import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.LEFT;
import static org.eclipse.egit.core.synchronize.GitCommitsModelCache.RIGHT;
import static org.eclipse.jgit.junit.JGitTestUtil.deleteTrashFile;
import static org.eclipse.jgit.junit.JGitTestUtil.writeTrashFile;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Commit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.junit.Test;

@SuppressWarnings("boxing")
public class GitCommitsModelCacheTest extends AbstractCacheTest {
	@Test
	public void shouldReturnEmptyListForSameSrcAndDstCommit() throws Exception {
		// given
		Git git = new Git(db);
		RevCommit c = commit(git, "second commit");
		// when
		List<Commit> result = GitCommitsModelCache.build(db, c, c, null);
		// then
		assertThat(result, notNullValue());
		assertThat(result.size(), is(0));
	}

	@Test
	public void shouldNotListEmptyCommits() throws Exception {
		// given
		Git git = new Git(db);
		RevCommit c = commit(git, "second commit");
		// when
		List<Commit> result = GitCommitsModelCache.build(db, initialTagId(), c,
				null);
		// then
		assertThat(result, notNullValue());
		assertThat(result.size(), is(0));
	}

	@Test
	public void shouldListAdditionOrDeletionInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "a.txt", "content");
		git.add().addFilepattern("a.txt").call();
		RevCommit c = commit(git, "first commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db,
				initialTagId(), c, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c,
				initialTagId(), null);
		// then
		// left assertions
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c, 1);
		assertFileAddition(c,
				leftResult.get(0).getChildren().get("a.txt"),
				"a.txt", LEFT);
		// right asserts, after changing sides addition becomes deletion
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c, 1);
		assertFileAddition(c, rightResult.get(0).getChildren().get("a.txt"),
				"a.txt", RIGHT);
	}

	@Test
	public void shouldListAdditionOrDeletionInsideFolderInCommit()
			throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "folder/a.txt", "content");
		git.add().addFilepattern("folder/a.txt").call();
		RevCommit c = commit(git, "first commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db,
				initialTagId(), c, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c,
				initialTagId(), null);
		// then
		// left assertions
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c, 1);
		assertThat(leftResult.get(0).getChildren().size(), is(1));
		assertFileAddition(c,
				leftResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				LEFT);
		// right asserts, after changing sides addition becomes deletion
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c, 1);
		assertThat(rightResult.get(0).getChildren().size(), is(1));
		assertFileAddition(c,
				rightResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				RIGHT);
	}

	@Test
	public void shouldListAdditionsOrDeletionsInsideSeparateFoldersInCommit()
			throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "folder/a.txt", "content");
		writeTrashFile(db, "folder2/b.txt", "b content");
		git.add().addFilepattern("folder/a.txt").call();
		git.add().addFilepattern("folder2/b.txt").call();
		RevCommit c = commit(git, "first commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db,
				initialTagId(), c, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c,
				initialTagId(), null);
		// then
		// left assertions
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertThat(leftResult.get(0).getShortMessage(), is("first commit"));
		assertThat(leftResult.get(0).getChildren(), notNullValue());
		assertThat(leftResult.get(0).getChildren().size(), is(2));
		assertFileAddition(c,
				leftResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				LEFT);
		assertFileAddition(c,
				leftResult.get(0).getChildren().get("folder2/b.txt"), "b.txt",
				LEFT);
		// right asserts, after changing sides addition becomes deletion
		assertThat(rightResult, notNullValue());
		assertThat(Integer.valueOf(rightResult.size()), is(Integer.valueOf(1)));
		assertThat(rightResult.get(0).getShortMessage(), is("first commit"));
		assertThat(rightResult.get(0).getChildren(), notNullValue());
		assertThat(rightResult.get(0).getChildren().size(), is(2));
		assertFileAddition(c,
				rightResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				RIGHT);
		assertFileAddition(c,
				rightResult.get(0).getChildren().get("folder2/b.txt"), "b.txt",
				RIGHT);
	}

	@Test
	public void shouldApplyPathFilter() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "folder/a.txt", "content");
		writeTrashFile(db, "folder2/b.txt", "b content");
		git.add().addFilepattern("folder/a.txt").call();
		git.add().addFilepattern("folder2/b.txt").call();
		RevCommit c = commit(git, "first commit");

		// when
		PathFilter pathFilter = PathFilter.create("folder");
		List<Commit> leftResult = GitCommitsModelCache.build(db,
				initialTagId(), c, pathFilter);
		// then
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertThat(leftResult.get(0).getShortMessage(), is("first commit"));
		assertThat(leftResult.get(0).getChildren(), notNullValue());
		assertThat(leftResult.get(0).getChildren().size(), is(1));
		assertFileAddition(c,
				leftResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				LEFT);
	}

	@Test
	public void shouldListAdditionsOrDeletionsInsideFolderInCommit()
			throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "folder/a.txt", "content");
		writeTrashFile(db, "folder/b.txt", "b content");
		git.add().addFilepattern("folder").call();
		RevCommit c = commit(git, "first commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db,
				initialTagId(), c, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c,
				initialTagId(), null);
		// then
		// left assertions
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertCommit(leftResult.get(0), c, 2);
		assertThat(leftResult.get(0).getChildren().size(), is(2));
		assertFileAddition(c,
				leftResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				LEFT);
		assertFileAddition(c,
				leftResult.get(0).getChildren().get("folder/b.txt"), "b.txt",
				LEFT);
		// right asserts, after changing sides addition becomes deletion
		assertThat(rightResult, notNullValue());
		assertThat(Integer.valueOf(rightResult.size()), is(Integer.valueOf(1)));
		assertCommit(rightResult.get(0), c, 2);
		assertThat(rightResult.get(0).getChildren().size(), is(2));
		assertFileAddition(c,
				rightResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				RIGHT);
		assertFileAddition(c,
				rightResult.get(0).getChildren().get("folder/b.txt"), "b.txt",
				RIGHT);
	}

	@Test
	public void shouldListChangeInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "a.txt", "content");
		git.add().addFilepattern("a.txt").call();
		RevCommit c1 = commit(git, "first commit");
		writeTrashFile(db, "a.txt", "new content");
		RevCommit c2 = commit(git, "second commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db, c1, c2, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c2, c1, null);
		// then
		// left assertions
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c2, 1);
		assertFileChange(c2, c1, leftResult.get(0).getChildren().get("a.txt"),
				"a.txt", LEFT);
		// right asserts
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c2, 1);
		assertFileChange(c2, c1, rightResult.get(0).getChildren().get("a.txt"),
				"a.txt", RIGHT);
	}

	@Test
	public void shouldListChangeInsideFolderInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "folder/a.txt", "content");
		git.add().addFilepattern("folder/a.txt").call();
		RevCommit c1 = commit(git, "first commit");
		writeTrashFile(db, "folder/a.txt", "new content");
		RevCommit c2 = commit(git, "second commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db, c1, c2, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c2, c1, null);
		// then
		// left assertions
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c2, 1);
		assertFileChange(c2, c1,
				leftResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				LEFT);
		// right asserts
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c2, 1);
		assertFileChange(c2, c1,
				rightResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				RIGHT);
	}

	@Test
	public void shouldListChangesInsideSeparateFoldersInCommit()
			throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "folder/a.txt", "content");
		writeTrashFile(db, "folder2/b.txt", "b content");
		git.add().addFilepattern("folder/a.txt").call();
		git.add().addFilepattern("folder2/b.txt").call();
		RevCommit c1 = commit(git, "first commit");
		writeTrashFile(db, "folder/a.txt", "new content");
		writeTrashFile(db, "folder2/b.txt", "new b content");
		RevCommit c2 = commit(git, "second commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db, c1, c2, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c2, c1, null);
		// then
		// left assertions
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertThat(leftResult.get(0).getShortMessage(), is("second commit"));
		assertThat(leftResult.get(0).getChildren(), notNullValue());
		assertThat(leftResult.get(0).getChildren().size(), is(2));
		assertFileChange(c2, c1,
				leftResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				LEFT);
		assertFileChange(c2, c1,
				leftResult.get(0).getChildren().get("folder2/b.txt"), "b.txt",
				LEFT);
		// right asserts
		assertThat(rightResult, notNullValue());
		assertThat(Integer.valueOf(rightResult.size()), is(Integer.valueOf(1)));
		assertThat(rightResult.get(0).getShortMessage(), is("second commit"));
		assertThat(rightResult.get(0).getChildren().size(), is(2));
		assertFileChange(c2, c1,
				rightResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				RIGHT);
		assertFileChange(c2, c1,
				rightResult.get(0).getChildren().get("folder2/b.txt"), "b.txt",
				RIGHT);
	}

	@Test
	public void shouldListChangesInsideFolderInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "folder/a.txt", "content");
		writeTrashFile(db, "folder/b.txt", "b content");
		git.add().addFilepattern("folder").call();
		RevCommit c1 = commit(git, "first commit");
		writeTrashFile(db, "folder/a.txt", "new content");
		writeTrashFile(db, "folder/b.txt", "new b content");
		RevCommit c2 = commit(git, "second commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db, c1, c2, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c2, c1, null);
		// then
		// left assertions
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertCommit(leftResult.get(0), c2, 2);
		assertFileChange(c2, c1,
				leftResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				LEFT);
		assertFileChange(c2, c1,
				leftResult.get(0).getChildren().get("folder/b.txt"), "b.txt",
				LEFT);
		// right asserts
		assertThat(rightResult, notNullValue());
		assertThat(Integer.valueOf(rightResult.size()), is(Integer.valueOf(1)));
		assertCommit(rightResult.get(0), c2, 2);
		assertFileChange(c2, c1,
				rightResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				RIGHT);
		assertFileChange(c2, c1,
				rightResult.get(0).getChildren().get("folder/b.txt"), "b.txt",
				RIGHT);
	}

	@Test
	public void shouldListAllTypeOfChangesInOneCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "a.txt", "a content");
		writeTrashFile(db, "c.txt", "c content");
		git.add().addFilepattern("a.txt").call();
		git.add().addFilepattern("c.txt").call();
		RevCommit c1 = commit(git, "first commit");
		deleteTrashFile(db, "a.txt");
		writeTrashFile(db, "b.txt", "b content");
		writeTrashFile(db, "c.txt", "new c content");
		git.add().addFilepattern("b.txt").call();
		RevCommit c2 = commit(git, "second commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db, c1, c2, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c2, c1, null);
		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c2, 3);
		assertFileDeletion(c2, c1,
				leftResult.get(0).getChildren().get("a.txt"), "a.txt", LEFT);
		assertFileAddition(c2, c1,
				leftResult.get(0).getChildren().get("b.txt"), "b.txt", LEFT);
		assertFileChange(c2, c1, leftResult.get(0).getChildren().get("c.txt"),
				"c.txt", LEFT);
		// right asserts
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c2, 3);
		assertFileDeletion(c2, c1, rightResult.get(0).getChildren()
				.get("a.txt"), "a.txt", RIGHT);
		assertFileAddition(c2, c1, rightResult.get(0).getChildren()
				.get("b.txt"), "b.txt", RIGHT);
		assertFileChange(c2, c1, rightResult.get(0).getChildren().get("c.txt"),
				"c.txt", RIGHT);
	}

	@Test
	public void shouldListAllTypeOfChangesInsideFolderInOneCommit()
			throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "folder/a.txt", "a content");
		writeTrashFile(db, "folder/c.txt", "c content");
		git.add().addFilepattern("folder/a.txt").call();
		git.add().addFilepattern("folder/c.txt").call();
		RevCommit c1 = commit(git, "first commit");
		deleteTrashFile(db, "folder/a.txt");
		writeTrashFile(db, "folder/b.txt", "b content");
		writeTrashFile(db, "folder/c.txt", "new c content");
		git.add().addFilepattern("folder/b.txt").call();
		RevCommit c2 = commit(git, "second commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db, c1, c2, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c2, c1, null);
		// then
		// left assertions
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c2, 3);
		assertFileDeletion(c2, c1,
				leftResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				LEFT);
		assertFileAddition(c2, c1,
				leftResult.get(0).getChildren().get("folder/b.txt"), "b.txt",
				LEFT);
		assertFileChange(c2, c1,
				leftResult.get(0).getChildren().get("folder/c.txt"), "c.txt",
				LEFT);
		// right asserts
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c2, 3);
		assertFileDeletion(c2, c1,
				rightResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				RIGHT);
		assertFileAddition(c2, c1,
				rightResult.get(0).getChildren().get("folder/b.txt"), "b.txt",
				RIGHT);
		assertFileChange(c2, c1,
				rightResult.get(0).getChildren().get("folder/c.txt"), "c.txt",
				RIGHT);
	}

	@Test
	public void shouldListAllTypeOfChangesInsideSeparateFoldersInOneCommit()
			throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile(db, "folder/a.txt", "a content");
		writeTrashFile(db, "folder2/c.txt", "c content");
		git.add().addFilepattern("folder/a.txt").call();
		git.add().addFilepattern("folder2/c.txt").call();
		RevCommit c1 = commit(git, "first commit");
		deleteTrashFile(db, "folder/a.txt");
		writeTrashFile(db, "folder1/b.txt", "b content");
		writeTrashFile(db, "folder2/c.txt", "new c content");
		git.add().addFilepattern("folder1/b.txt").call();
		RevCommit c2 = commit(git, "second commit");
		// when
		List<Commit> leftResult = GitCommitsModelCache.build(db, c1, c2, null);
		List<Commit> rightResult = GitCommitsModelCache.build(db, c2, c1, null);
		// then
		// left assertions
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertThat(leftResult.get(0).getShortMessage(), is("second commit"));
		assertThat(leftResult.get(0).getChildren(), notNullValue());
		assertThat(leftResult.get(0).getChildren().size(), is(3));
		assertFileDeletion(c2, c1,
				leftResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				LEFT);
		assertFileAddition(c2, c1,
				leftResult.get(0).getChildren().get("folder1/b.txt"), "b.txt",
				LEFT);
		assertFileChange(c2, c1,
				leftResult.get(0).getChildren().get("folder2/c.txt"), "c.txt",
				LEFT);
		// right asserts
		assertThat(rightResult, notNullValue());
		assertThat(Integer.valueOf(rightResult.size()), is(Integer.valueOf(1)));
		assertThat(rightResult.get(0).getShortMessage(), is("second commit"));
		assertThat(rightResult.get(0).getChildren(), notNullValue());
		assertThat(rightResult.get(0).getChildren().size(), is(3));
		assertFileDeletion(c2, c1,
				rightResult.get(0).getChildren().get("folder/a.txt"), "a.txt",
				RIGHT);
		assertFileAddition(c2, c1,
				rightResult.get(0).getChildren().get("folder1/b.txt"), "b.txt",
				RIGHT);
		assertFileChange(c2, c1,
				rightResult.get(0).getChildren().get("folder2/c.txt"), "c.txt",
				RIGHT);
	}

	private RevCommit commit(Git git, String msg) throws Exception {
		tick();
		return git.commit().setAll(true).setMessage(msg)
				.setCommitter(committer).call();
	}

	private ObjectId initialTagId() throws AmbiguousObjectException,
			IOException {
		return db.resolve(INITIAL_TAG);
	}

	private void assertCommit(Commit commit, RevCommit actualCommit,
			int childrenCount) {
		commonCommitAsserts(commit, actualCommit);
		assertThat(commit.getChildren(), notNullValue());
		assertThat(commit.getChildren().size(), is(childrenCount));
	}

	private void commonCommitAsserts(Commit commit, RevCommit actualCommit) {
		assertThat(commit.getShortMessage(), is(actualCommit.getShortMessage()));
		assertThat(commit.getId().toObjectId(), is(actualCommit.getId()));
		assertThat(commit.getAuthorName(), is(actualCommit.getAuthorIdent()
				.getName()));
		assertThat(commit.getCommitterName(), is(actualCommit
				.getCommitterIdent().getName()));
		assertThat(commit.getCommitDate(), is(actualCommit.getAuthorIdent()
				.getWhen()));
	}

	private void assertFileChange(RevCommit actual, RevCommit parent,
			Change change, String name, int direction) {
		commonFileAssertions(actual, parent, change, name);
		assertThat(change.getObjectId(), not(ZERO_ID));
		assertThat(change.getRemoteObjectId(), not(ZERO_ID));
		if (direction == LEFT)
			assertThat(change.getKind(), is(LEFT | CHANGE));
		else
			assertThat(change.getKind(), is(RIGHT | CHANGE));
	}

	private void assertFileAddition(RevCommit actual, Change change,
			String name, int direction) {
		assertFileAddition(actual, null, change, name, direction);
	}

	private void assertFileAddition(RevCommit actual, RevCommit parent,
			Change change, String name, int direction) {
		commonFileAssertions(actual, parent, change, name);
		if (direction == LEFT) {
			assertThat(change.getKind(), is(LEFT | ADDITION));
		} else { // should be Differencer.Right
			assertThat(change.getKind(), is(RIGHT | ADDITION));
		}
		assertThat(change.getObjectId(), not(ZERO_ID));
		assertThat(change.getRemoteObjectId(), nullValue());
	}

	private void assertFileDeletion(RevCommit actual, RevCommit parent,
			Change change, String name, int direction) {
		commonFileAssertions(actual, parent, change, name);
		if (direction == LEFT) {
			assertThat(change.getKind(), is(LEFT | DELETION));
		} else { // should be Differencer.Right
			assertThat(change.getKind(), is(RIGHT | DELETION));
		}
		assertThat(change.getObjectId(), nullValue());
		assertThat(change.getRemoteObjectId(), not(ZERO_ID));
	}

	private void commonFileAssertions(RevCommit actual, RevCommit parent,
			Change change, String name) {
		assertThat(change, notNullValue());
		if (parent != null)
			assertThat(change.getRemoteCommitId().toObjectId(),
					is(parent.getId()));
		if (actual != null && !ObjectId.zeroId().equals(actual))
			assertThat(change.getCommitId().toObjectId(), is(actual.getId()));
		assertThat(change.getName(), is(name));
	}
}
