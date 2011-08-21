/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.eclipse.compare.structuremergeviewer.Differencer.ADDITION;
import static org.eclipse.compare.structuremergeviewer.Differencer.CHANGE;
import static org.eclipse.compare.structuremergeviewer.Differencer.DELETION;
import static org.eclipse.compare.structuremergeviewer.Differencer.LEFT;
import static org.eclipse.compare.structuremergeviewer.Differencer.RIGHT;
import static org.eclipse.jgit.lib.ObjectId.zeroId;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.egit.core.synchronize.ChangeSetModelCache.Change;
import org.eclipse.egit.core.synchronize.ChangeSetModelCache.Commit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("boxing")
public class ChangeSetModelCacheTest extends LocalDiskRepositoryTestCase {

	private FileRepository db;

	private static final String INITIAL_TAG = "initial-tag";

	private static final AbbreviatedObjectId ZERO_ID = AbbreviatedObjectId.fromObjectId(zeroId());

	@Before
	@Override
	// copied from org.eclipse.jgit.lib.RepositoryTestCase
	public void setUp() throws Exception {
		super.setUp();
		db = createWorkRepository();
		Git git = new Git(db);
		git.commit().setMessage("initial commit").call();
		git.tag().setName(INITIAL_TAG).call();
	}

	@Test
	public void shouldReturnEmptyListForSameSrcAndDstCommit() throws Exception {
		// given
		Git git = new Git(db);
		RevCommit c = commit(git, "second commit");

		// when
		List<Commit> result = ChangeSetModelCache.build(db, c, c);

		// then
		assertThat(result, notNullValue());
		assertThat(result.size(), is(0));
	}

	@Test
	public void shouldListOneEmptyCommit() throws Exception {
		// given
		Git git = new Git(db);
		RevCommit c = commit(git, "second commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, initialTagId(), c);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c, initialTagId());

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertThat(leftResult.size(), is(1));
		assertEmptyCommit(leftResult.get(0), c, LEFT);

		// right asserts
		assertThat(rightResult, notNullValue());
		assertThat(rightResult.size(), is(1));
		assertEmptyCommit(rightResult.get(0), c, RIGHT);
	}

	@Test
	public void shouldListTwoEmptyCommits() throws Exception {
		// given
		Git git = new Git(db);
		RevCommit c1 = commit(git, "second commit");
		RevCommit c2 = commit(git, "third commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, initialTagId(), c2);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c2, initialTagId());

		// then
		assertThat(leftResult, notNullValue());
		assertThat(leftResult.size(), is(2));
		assertEmptyCommit(leftResult.get(0), c2, LEFT);
		assertEmptyCommit(leftResult.get(1), c1, LEFT);

		assertThat(rightResult, notNullValue());
		assertThat(rightResult.size(), is(2));
		assertEmptyCommit(rightResult.get(0), c2, RIGHT);
		assertEmptyCommit(rightResult.get(1), c1, RIGHT);
	}

	@Test
	public void shouldListAdditionOrDeletionInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("a.txt", "content");
		git.add().addFilepattern("a.txt").call();
		RevCommit c = commit(git, "first commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, initialTagId(), c);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c, initialTagId());

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c, 1);
		assertFileAddition(leftResult.get(0).getChildren()[0], "a.txt", LEFT | ADDITION);

		// right asserts, after changing sides addition becomes deletion
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c, 1);
		assertFileDeletion(rightResult.get(0).getChildren()[0], "a.txt", RIGHT | DELETION);
	}

	@Test
	public void shouldListAdditionOrDeletionInsideFolderInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("folder/a.txt", "content");
		git.add().addFilepattern("folder/a.txt").call();
		RevCommit c = commit(git, "first commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, initialTagId(), c);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c, initialTagId());

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c, 1);
		assertFolderAddition(leftResult.get(0).getChildren()[0], "folder", LEFT | ADDITION);
		assertThat(leftResult.get(0).getChildren()[0].getChildren().length, is(1));
		assertFileAddition(leftResult.get(0).getChildren()[0].getChildren()[0], "a.txt", LEFT | ADDITION);

		// right asserts, after changing sides addition becomes deletion
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c, 1);
		assertFolderDeletion(rightResult.get(0).getChildren()[0], "folder", RIGHT | DELETION);
		assertThat(rightResult.get(0).getChildren()[0].getChildren().length, is(1));
		assertFileDeletion(rightResult.get(0).getChildren()[0].getChildren()[0], "a.txt", RIGHT | DELETION);
	}

	@Test
	public void shouldListAdditionsOrDeletionsInsideSeparateFoldersInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("folder/a.txt", "content");
		writeTrashFile("folder2/b.txt", "b content");
		git.add().addFilepattern("folder/a.txt").call();
		git.add().addFilepattern("folder2/b.txt").call();
		RevCommit c = commit(git, "first commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, initialTagId(), c);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c, initialTagId());

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertThat(leftResult.get(0).getShortMessage(), is("first commit"));

		assertThat(leftResult.get(0).getChildren(), notNullValue());
		assertThat(leftResult.get(0).getChildren().length, is(2));

		assertFolderAddition(leftResult.get(0).getChildren()[0], "folder", LEFT | ADDITION);
		assertFolderAddition(leftResult.get(0).getChildren()[1], "folder2", LEFT | ADDITION);

		assertThat(leftResult.get(0).getChildren()[0].getChildren().length, is(Integer.valueOf(1)));
		assertFileAddition(leftResult.get(0).getChildren()[0].getChildren()[0], "a.txt", LEFT | ADDITION);

		assertThat(leftResult.get(0).getChildren()[1].getChildren().length, is(Integer.valueOf(1)));
		assertFileAddition(leftResult.get(0).getChildren()[1].getChildren()[0], "b.txt",LEFT | ADDITION);

		// right asserts, after changing sides addition becomes deletion
		assertThat(rightResult, notNullValue());
		assertThat(Integer.valueOf(rightResult.size()), is(Integer.valueOf(1)));
		assertThat(rightResult.get(0).getShortMessage(), is("first commit"));

		assertThat(rightResult.get(0).getChildren(), notNullValue());
		assertThat(rightResult.get(0).getChildren().length, is(2));

		assertFolderDeletion(rightResult.get(0).getChildren()[0], "folder", RIGHT | DELETION);
		assertFolderDeletion(rightResult.get(0).getChildren()[1], "folder2", RIGHT | DELETION);

		assertThat(rightResult.get(0).getChildren()[0].getChildren().length, is(Integer.valueOf(1)));
		assertFileDeletion(rightResult.get(0).getChildren()[0].getChildren()[0], "a.txt", RIGHT | DELETION);

		assertThat(rightResult.get(0).getChildren()[1].getChildren().length, is(Integer.valueOf(1)));
		assertFileDeletion(rightResult.get(0).getChildren()[1].getChildren()[0], "b.txt",RIGHT | DELETION);
	}

	@Test
	public void shouldListAdditionsOrDeletionsInsideFolderInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("folder/a.txt", "content");
		writeTrashFile("folder/b.txt", "b content");
		git.add().addFilepattern("folder").call();
		RevCommit c = commit(git, "first commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, initialTagId(), c);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c, initialTagId());

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertCommit(leftResult.get(0), c, 1);

		assertFolderAddition(leftResult.get(0).getChildren()[0], "folder", LEFT | ADDITION);

		assertThat(leftResult.get(0).getChildren()[0].getChildren().length, is(2));

		assertFileAddition(leftResult.get(0).getChildren()[0].getChildren()[0], "a.txt", LEFT | ADDITION);
		assertFileAddition(leftResult.get(0).getChildren()[0].getChildren()[1], "b.txt", LEFT | ADDITION);

		// right asserts, after changing sides addition becomes deletion
		assertThat(rightResult, notNullValue());
		assertThat(Integer.valueOf(rightResult.size()), is(Integer.valueOf(1)));
		assertCommit(rightResult.get(0), c, 1);

		assertFolderDeletion(rightResult.get(0).getChildren()[0], "folder", RIGHT | DELETION);

		assertThat(rightResult.get(0).getChildren()[0].getChildren().length, is(2));

		assertFileDeletion(rightResult.get(0).getChildren()[0].getChildren()[0], "a.txt", RIGHT | DELETION);
		assertFileDeletion(rightResult.get(0).getChildren()[0].getChildren()[1], "b.txt", RIGHT | DELETION);
	}

	@Test
	public void shouldListChangeInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("a.txt", "content");
		git.add().addFilepattern("a.txt").call();
		RevCommit c1 = commit(git, "first commit");
		writeTrashFile("a.txt", "new content");
		RevCommit c2 = commit(git, "second commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, c1, c2);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c2, c1);

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c2, 1);
		assertFileChange(leftResult.get(0).getChildren()[0], "a.txt", LEFT | CHANGE);

		// right asserts
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c2, 1);
		assertFileChange(rightResult.get(0).getChildren()[0], "a.txt", RIGHT | CHANGE);
	}

	@Test
	public void shouldListChangeInsideFolderInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("folder/a.txt", "content");
		git.add().addFilepattern("folder/a.txt").call();
		RevCommit c1 = commit(git, "first commit");
		writeTrashFile("folder/a.txt", "new content");
		RevCommit c2 = commit(git, "second commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, c1, c2);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c2, c1);

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c2, 1);
		assertFolderChange(leftResult.get(0).getChildren()[0], "folder", LEFT | CHANGE);
		assertThat(leftResult.get(0).getChildren()[0].getChildren().length, is(1));
		assertFileChange(leftResult.get(0).getChildren()[0].getChildren()[0], "a.txt", LEFT | CHANGE);

		// right asserts
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c2, 1);
		assertFolderChange(rightResult.get(0).getChildren()[0], "folder", RIGHT | CHANGE);
		assertThat(rightResult.get(0).getChildren()[0].getChildren().length, is(1));
		assertFileChange(rightResult.get(0).getChildren()[0].getChildren()[0], "a.txt", RIGHT | CHANGE);
	}

	@Test
	public void shouldListChangesInsideSeparateFoldersInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("folder/a.txt", "content");
		writeTrashFile("folder2/b.txt", "b content");
		git.add().addFilepattern("folder/a.txt").call();
		git.add().addFilepattern("folder2/b.txt").call();
		RevCommit c1 = commit(git, "first commit");
		writeTrashFile("folder/a.txt", "new content");
		writeTrashFile("folder2/b.txt", "new b content");
		RevCommit c2 = commit(git, "second commit");


		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, c1, c2);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c2, c1);

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertThat(leftResult.get(0).getShortMessage(), is("second commit"));

		assertThat(leftResult.get(0).getChildren(), notNullValue());
		assertThat(leftResult.get(0).getChildren().length, is(2));

		assertFolderChange(leftResult.get(0).getChildren()[0], "folder", LEFT | CHANGE);
		assertFolderChange(leftResult.get(0).getChildren()[1], "folder2", LEFT | CHANGE);

		assertThat(leftResult.get(0).getChildren()[0].getChildren().length, is(Integer.valueOf(1)));
		assertFileChange(leftResult.get(0).getChildren()[0].getChildren()[0], "a.txt", LEFT | CHANGE);

		assertThat(leftResult.get(0).getChildren()[1].getChildren().length, is(Integer.valueOf(1)));
		assertFileChange(leftResult.get(0).getChildren()[1].getChildren()[0], "b.txt",LEFT | CHANGE);

		// right asserts
		assertThat(rightResult, notNullValue());
		assertThat(Integer.valueOf(rightResult.size()), is(Integer.valueOf(1)));
		assertThat(rightResult.get(0).getShortMessage(), is("second commit"));

		assertThat(rightResult.get(0).getChildren(), notNullValue());
		assertThat(rightResult.get(0).getChildren().length, is(2));

		assertFolderChange(rightResult.get(0).getChildren()[0], "folder", RIGHT | CHANGE);
		assertFolderChange(rightResult.get(0).getChildren()[1], "folder2", RIGHT | CHANGE);

		assertThat(rightResult.get(0).getChildren()[0].getChildren().length, is(Integer.valueOf(1)));
		assertFileChange(rightResult.get(0).getChildren()[0].getChildren()[0], "a.txt", RIGHT | CHANGE);

		assertThat(rightResult.get(0).getChildren()[1].getChildren().length, is(Integer.valueOf(1)));
		assertFileChange(rightResult.get(0).getChildren()[1].getChildren()[0], "b.txt",RIGHT | CHANGE);
	}

	@Test
	public void shouldListChangesInsideFolderInCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("folder/a.txt", "content");
		writeTrashFile("folder/b.txt", "b content");
		git.add().addFilepattern("folder").call();
		RevCommit c1 = commit(git, "first commit");
		writeTrashFile("folder/a.txt", "new content");
		writeTrashFile("folder/b.txt", "new b content");
		RevCommit c2 = commit(git, "second commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, c1, c2);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c2, c1);

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertCommit(leftResult.get(0), c2, 1);

		assertFolderChange(leftResult.get(0).getChildren()[0], "folder", LEFT | CHANGE);

		assertThat(leftResult.get(0).getChildren()[0].getChildren().length, is(2));

		assertFileChange(leftResult.get(0).getChildren()[0].getChildren()[0], "a.txt", LEFT | CHANGE);
		assertFileChange(leftResult.get(0).getChildren()[0].getChildren()[1], "b.txt", LEFT | CHANGE);

		// right asserts
		assertThat(rightResult, notNullValue());
		assertThat(Integer.valueOf(rightResult.size()), is(Integer.valueOf(1)));
		assertCommit(rightResult.get(0), c2, 1);

		assertFolderChange(rightResult.get(0).getChildren()[0], "folder", RIGHT | CHANGE);

		assertThat(rightResult.get(0).getChildren()[0].getChildren().length, is(2));

		assertFileChange(rightResult.get(0).getChildren()[0].getChildren()[0], "a.txt", RIGHT | CHANGE);
		assertFileChange(rightResult.get(0).getChildren()[0].getChildren()[1], "b.txt", RIGHT | CHANGE);
	}

	@Test
	public void shouldListAllTypeOfChangesInOneCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("a.txt", "a content");
		writeTrashFile("c.txt", "c content");
		git.add().addFilepattern("a.txt").call();
		git.add().addFilepattern("c.txt").call();
		RevCommit c1 = commit(git, "first commit");
		deleteTrashFile("a.txt");
		writeTrashFile("b.txt", "b content");
		writeTrashFile("c.txt", "new c content");
		git.add().addFilepattern("b.txt").call();
		RevCommit c2 = commit(git, "second commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, c1, c2);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c2, c1);

		// then

		// left asserts
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c2, 3);
		assertFileDeletion(leftResult.get(0).getChildren()[0], "a.txt", LEFT | DELETION);
		assertFileAddition(leftResult.get(0).getChildren()[1], "b.txt", LEFT | ADDITION);
		assertFileChange(leftResult.get(0).getChildren()[2], "c.txt", LEFT | CHANGE);

		// right asserts
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c2, 3);
		assertFileAddition(rightResult.get(0).getChildren()[0], "a.txt", RIGHT | ADDITION);
		assertFileDeletion(rightResult.get(0).getChildren()[1], "b.txt", RIGHT | DELETION);
		assertFileChange(rightResult.get(0).getChildren()[2], "c.txt", RIGHT | CHANGE);
	}

	@Test
	public void shouldListAllTypeOfChangesInsideFolderInOneCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("folder/a.txt", "a content");
		writeTrashFile("folder/c.txt", "c content");
		git.add().addFilepattern("folder/a.txt").call();
		git.add().addFilepattern("folder/c.txt").call();
		RevCommit c1 = commit(git, "first commit");
		deleteTrashFile("folder/a.txt");
		writeTrashFile("folder/b.txt", "b content");
		writeTrashFile("folder/c.txt", "new c content");
		git.add().addFilepattern("folder/b.txt").call();
		RevCommit c2 = commit(git, "second commit");

		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, c1, c2);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c2, c1);

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertCommit(leftResult.get(0), c2, 1);
		assertFolderChange(leftResult.get(0).getChildren()[0], "folder", LEFT | CHANGE);
		assertThat(leftResult.get(0).getChildren()[0].getChildren().length, is(3));
		assertFileDeletion(leftResult.get(0).getChildren()[0].getChildren()[0], "a.txt", LEFT | DELETION);
		assertFileAddition(leftResult.get(0).getChildren()[0].getChildren()[1], "b.txt", LEFT | ADDITION);
		assertFileChange(leftResult.get(0).getChildren()[0].getChildren()[2], "c.txt", LEFT | CHANGE);

		// right asserts
		assertThat(rightResult, notNullValue());
		assertCommit(rightResult.get(0), c2, 1);
		assertFolderChange(rightResult.get(0).getChildren()[0], "folder", RIGHT | CHANGE);
		assertThat(rightResult.get(0).getChildren()[0].getChildren().length, is(3));
		assertFileAddition(rightResult.get(0).getChildren()[0].getChildren()[0], "a.txt", RIGHT | ADDITION);
		assertFileDeletion(rightResult.get(0).getChildren()[0].getChildren()[1], "b.txt", RIGHT | DELETION);
		assertFileChange(rightResult.get(0).getChildren()[0].getChildren()[2], "c.txt", RIGHT | CHANGE);
	}

	@Test
	public void shouldListAllTypeOfChangesInsideSeparateFoldersInOneCommit() throws Exception {
		// given
		Git git = new Git(db);
		writeTrashFile("folder/a.txt", "a content");
		writeTrashFile("folder2/c.txt", "c content");
		git.add().addFilepattern("folder/a.txt").call();
		git.add().addFilepattern("folder2/c.txt").call();
		RevCommit c1 = commit(git, "first commit");
		deleteTrashFile("folder/a.txt");
		writeTrashFile("folder1/b.txt", "b content");
		writeTrashFile("folder2/c.txt", "new c content");
		git.add().addFilepattern("folder1/b.txt").call();
		RevCommit c2 = commit(git, "second commit");


		// when
		List<Commit> leftResult = ChangeSetModelCache.build(db, c1, c2);
		List<Commit> rightResult = ChangeSetModelCache.build(db, c2, c1);

		// then
		// left asserts
		assertThat(leftResult, notNullValue());
		assertThat(Integer.valueOf(leftResult.size()), is(Integer.valueOf(1)));
		assertThat(leftResult.get(0).getShortMessage(), is("second commit"));

		assertThat(leftResult.get(0).getChildren(), notNullValue());
		assertThat(leftResult.get(0).getChildren().length, is(3));

		assertFolderDeletion(leftResult.get(0).getChildren()[0], "folder", LEFT | DELETION);
		assertFolderAddition(leftResult.get(0).getChildren()[1], "folder1", LEFT | ADDITION);
		assertFolderChange(leftResult.get(0).getChildren()[2], "folder2", LEFT | CHANGE);

		assertThat(leftResult.get(0).getChildren()[0].getChildren().length, is(Integer.valueOf(1)));
		assertFileDeletion(leftResult.get(0).getChildren()[0].getChildren()[0], "a.txt", LEFT | DELETION);

		assertThat(leftResult.get(0).getChildren()[1].getChildren().length, is(Integer.valueOf(1)));
		assertFileAddition(leftResult.get(0).getChildren()[1].getChildren()[0], "b.txt", LEFT | ADDITION);

		assertThat(leftResult.get(0).getChildren()[2].getChildren().length, is(Integer.valueOf(1)));
		assertFileChange(leftResult.get(0).getChildren()[2].getChildren()[0], "c.txt", LEFT | CHANGE);

		// right asserts
		assertThat(rightResult, notNullValue());
		assertThat(Integer.valueOf(rightResult.size()), is(Integer.valueOf(1)));
		assertThat(rightResult.get(0).getShortMessage(), is("second commit"));

		assertThat(rightResult.get(0).getChildren(), notNullValue());
		assertThat(rightResult.get(0).getChildren().length, is(3));

		assertFolderAddition(rightResult.get(0).getChildren()[0], "folder", RIGHT | ADDITION);
		assertFolderDeletion(rightResult.get(0).getChildren()[1], "folder1", RIGHT | DELETION);
		assertFolderChange(rightResult.get(0).getChildren()[2], "folder2", RIGHT | CHANGE);

		assertThat(rightResult.get(0).getChildren()[0].getChildren().length, is(Integer.valueOf(1)));
		assertFileAddition(rightResult.get(0).getChildren()[0].getChildren()[0], "a.txt", RIGHT | ADDITION);

		assertThat(rightResult.get(0).getChildren()[1].getChildren().length, is(Integer.valueOf(1)));
		assertFileDeletion(rightResult.get(0).getChildren()[1].getChildren()[0], "b.txt",RIGHT | DELETION);

		assertThat(rightResult.get(0).getChildren()[2].getChildren().length, is(Integer.valueOf(1)));
		assertFileChange(rightResult.get(0).getChildren()[2].getChildren()[0], "c.txt", RIGHT | CHANGE);
	}

	private RevCommit commit(Git git, String msg) throws Exception {
		tick();
		return git.commit().setAll(true).setMessage(msg).setCommitter(committer).call();
	}

	private ObjectId initialTagId() throws AmbiguousObjectException, IOException {
		return db.resolve(INITIAL_TAG);
	}

	private void assertEmptyCommit(Commit commit, RevCommit actualCommit, int direction) {
		assertThat(commit.getShortMessage(), is(actualCommit.getShortMessage()));
		assertThat(commit.getChildren(), nullValue());
		assertThat(commit.getDirection(), is(direction));
		assertThat(commit.getId().toObjectId(), is(actualCommit.getId()));
	}

	private void assertCommit(Commit commit, RevCommit actualCommit, int childrenCount) {
		assertThat(commit.getShortMessage(), is(actualCommit.getShortMessage()));
		assertThat(commit.getChildren(), notNullValue());
		assertThat(commit.getChildren().length, is(childrenCount));
		assertThat(commit.getId().toObjectId(), is(actualCommit.getId()));
	}

	private void assertFolderChange(Change change, String name, int direction) {
		assertFolderAddition(change, name, direction);
	}

	private void assertFolderAddition(Change change, String name, int direction) {
		assertThat(change.getName(), is(name));
		assertThat(Boolean.valueOf(change.isTree()), is(TRUE));
		assertThat(change.getObjectId(), not(ZERO_ID));
		assertThat(change.getKind(), is(direction));
		assertThat(change.getChildren(), notNullValue());
	}

	private void assertFolderDeletion(Change change, String name, int direction) {
		assertThat(change.getName(), is(name));
		assertThat(Boolean.valueOf(change.isTree()), is(TRUE));
		assertThat(change.getObjectId(), is(ZERO_ID));
		assertThat(change.getKind(), is(direction));
		assertThat(change.getChildren(), notNullValue());
	}

	private void assertFileChange(Change change, String name, int direction) {
		assertFileAddition(change, name, direction);
	}

	private void assertFileAddition(Change change, String name, int direction) {
		assertThat(change.getName(), is(name));
		assertThat(Boolean.valueOf(change.isTree()), is(FALSE));
		assertThat(change.getObjectId(), not(ZERO_ID));
		assertThat(change.getKind(), is(direction));
		assertThat(change.getChildren(), nullValue());
	}

	private void assertFileDeletion(Change change, String name, int direction) {
		assertThat(change.getName(), is(name));
		assertThat(Boolean.valueOf(change.isTree()), is(FALSE));
		assertThat(change.getObjectId(), is(ZERO_ID));
		assertThat(change.getKind(), is(direction));
		assertThat(change.getChildren(), nullValue());
	}

	// copied from org.eclipse.jgit.lib.RepositoryTestCase
	private File writeTrashFile(final String name, final String data)
			throws IOException {
		File path = new File(db.getWorkTree(), name);
		write(path, data);
		return path;
	}

	private void deleteTrashFile(final String name) throws IOException {
		File path = new File(db.getWorkTree(), name);
		FileUtils.delete(path);
	}

}
