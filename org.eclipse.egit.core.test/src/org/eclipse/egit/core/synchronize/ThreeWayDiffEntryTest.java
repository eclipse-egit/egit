package org.eclipse.egit.core.synchronize;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.egit.core.synchronize.ThreeWayDiffEntry.ChangeType;
import org.eclipse.egit.core.synchronize.ThreeWayDiffEntry.Direction;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Before;
import org.junit.Test;

public class ThreeWayDiffEntryTest extends LocalDiskRepositoryTestCase {

	private FileRepository db;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		db = createWorkRepository();
	}

	@Test
	public void shouldListOutgoingAddition() throws Exception {
		// given
		writeTrashFile("a.txt", "content");
		Git git = new Git(db);
		git.add().addFilepattern("a.txt").call();
		RevCommit c = git.commit().setMessage("initial commit").call();

		// when
		TreeWalk walk = new TreeWalk(db);
		walk.addTree(c.getTree());
		walk.addTree(new EmptyTreeIterator());
		walk.addTree(new EmptyTreeIterator());
		List<ThreeWayDiffEntry> result = ThreeWayDiffEntry.scan(walk);

		// then
		assertThat(result, notNullValue());
		assertThat(Integer.valueOf(result.size()), is(Integer.valueOf(1)));

		ThreeWayDiffEntry entry = result.get(0);
		assertThat(entry.getDirection(), is(Direction.OUTGOING));
		assertThat(entry.getChangeType(), is(ChangeType.ADD));
		assertThat(entry.getPath(), is("a.txt"));
	}

	@Test
	public void shouldListIncomingAddition() throws Exception {
		// given
		writeTrashFile("a.txt", "content");
		Git git = new Git(db);
		git.add().addFilepattern("a.txt").call();
		RevCommit c = git.commit().setMessage("initial commit").call();

		// when
		TreeWalk walk = new TreeWalk(db);
		walk.addTree(new EmptyTreeIterator());
		walk.addTree(new EmptyTreeIterator());
		walk.addTree(c.getTree());
		List<ThreeWayDiffEntry> result = ThreeWayDiffEntry.scan(walk);

		// then
		assertThat(result, notNullValue());
		assertThat(Integer.valueOf(result.size()), is(Integer.valueOf(1)));

		ThreeWayDiffEntry entry = result.get(0);
		assertThat(entry.getDirection(), is(Direction.INCOMING));
		assertThat(entry.getChangeType(), is(ChangeType.ADD));
		assertThat(entry.getPath(), is("a.txt"));
	}

	@Test
	public void shouldListOutgoingDelete() throws Exception {
		// given
		writeTrashFile("a.txt", "content");
		Git git = new Git(db);
		git.add().addFilepattern("a.txt").call();
		RevCommit c = git.commit().setMessage("initial commit").call();

		// when
		TreeWalk walk = new TreeWalk(db);
		walk.addTree(new EmptyTreeIterator());
		walk.addTree(c.getTree());
		walk.addTree(c.getTree());
		List<ThreeWayDiffEntry> result = ThreeWayDiffEntry.scan(walk);

		// then
		assertThat(result, notNullValue());
		assertThat(Integer.valueOf(result.size()), is(Integer.valueOf(1)));

		ThreeWayDiffEntry entry = result.get(0);
		assertThat(entry.getDirection(), is(Direction.OUTGOING));
		assertThat(entry.getChangeType(), is(ChangeType.DELETE));
		assertThat(entry.getPath(), is("a.txt"));
	}

	@Test
	public void shouldListIncomingDelete() throws Exception {
		// given
		writeTrashFile("a.txt", "content");
		Git git = new Git(db);
		git.add().addFilepattern("a.txt").call();
		RevCommit c = git.commit().setMessage("initial commit").call();

		// when
		TreeWalk walk = new TreeWalk(db);
		walk.addTree(c.getTree());
		walk.addTree(c.getTree());
		walk.addTree(new EmptyTreeIterator());
		List<ThreeWayDiffEntry> result = ThreeWayDiffEntry.scan(walk);

		// then
		assertThat(result, notNullValue());
		assertThat(Integer.valueOf(result.size()), is(Integer.valueOf(1)));

		ThreeWayDiffEntry entry = result.get(0);
		assertThat(entry.getDirection(), is(Direction.INCOMING));
		assertThat(entry.getChangeType(), is(ChangeType.DELETE));
		assertThat(entry.getPath(), is("a.txt"));
	}

	@Test
	public void shouldListConflictingChange() throws Exception {
		// given
		writeTrashFile("a.txt", "content");
		Git git = new Git(db);
		git.add().addFilepattern("a.txt").call();
		RevCommit c = git.commit().setMessage("initial commit").call();

		// when
		TreeWalk walk = new TreeWalk(db);
		walk.addTree(new EmptyTreeIterator());
		walk.addTree(c.getTree());
		walk.addTree(new EmptyTreeIterator());
		List<ThreeWayDiffEntry> result = ThreeWayDiffEntry.scan(walk);

		// then
		assertThat(result, notNullValue());
		assertThat(Integer.valueOf(result.size()), is(Integer.valueOf(1)));

		ThreeWayDiffEntry entry = result.get(0);
		assertThat(entry.getDirection(), is(Direction.CONFLICTING));
		assertThat(entry.getChangeType(), is(ChangeType.MODIFY));
		// assertThat(entry.getNewPath(), is("a.txt"));
		// assertThat(entry.getOldPath(), is(DEV_NULL));
	}

	@Test
	public void shouldListConflictingChange2() throws Exception {
		// given
		writeTrashFile("a.txt", "content");
		Git git = new Git(db);
		git.add().addFilepattern("a.txt").call();
		RevCommit c = git.commit().setMessage("initial commit").call();

		// when
		TreeWalk walk = new TreeWalk(db);
		walk.addTree(c.getTree());
		walk.addTree(new EmptyTreeIterator());
		walk.addTree(c.getTree());
		List<ThreeWayDiffEntry> result = ThreeWayDiffEntry.scan(walk);

		// then
		assertThat(result, notNullValue());
		assertThat(Integer.valueOf(result.size()), is(Integer.valueOf(1)));

		ThreeWayDiffEntry entry = result.get(0);
		assertThat(entry.getDirection(), is(Direction.CONFLICTING));
		assertThat(entry.getChangeType(), is(ChangeType.MODIFY));
		assertThat(entry.getPath(), is("a.txt"));
	}

	@Test
	public void shouldListIncomingModify() throws Exception {
		// given
		writeTrashFile("a.txt", "content");
		Git git = new Git(db);
		git.add().addFilepattern("a.txt").call();
		RevCommit c = git.commit().setMessage("initial commit").call();
		writeTrashFile("a.txt", "new line");
		RevCommit c1 = git.commit().setAll(true).setMessage("second commit")
				.call();

		// when
		TreeWalk walk = new TreeWalk(db);
		walk.addTree(c.getTree());
		walk.addTree(c.getTree());
		walk.addTree(c1.getTree());
		List<ThreeWayDiffEntry> result = ThreeWayDiffEntry.scan(walk);

		// then
		assertThat(result, notNullValue());
		assertThat(Integer.valueOf(result.size()), is(Integer.valueOf(1)));

		ThreeWayDiffEntry entry = result.get(0);
		assertThat(entry.getDirection(), is(Direction.INCOMING));
		assertThat(entry.getChangeType(), is(ChangeType.MODIFY));
		assertThat(entry.getPath(), is("a.txt"));
	}

	@Test
	public void shouldListOutgoingModify() throws Exception {
		// given
		writeTrashFile("a.txt", "content");
		Git git = new Git(db);
		git.add().addFilepattern("a.txt").call();
		RevCommit c = git.commit().setMessage("initial commit").call();
		writeTrashFile("a.txt", "newe line");
		RevCommit c1 = git.commit().setAll(true).setMessage("second commit")
				.call();

		// when
		TreeWalk walk = new TreeWalk(db);
		walk.addTree(c1.getTree());
		walk.addTree(c.getTree());
		walk.addTree(c.getTree());
		List<ThreeWayDiffEntry> result = ThreeWayDiffEntry.scan(walk);

		// then
		assertThat(result, notNullValue());
		assertThat(Integer.valueOf(result.size()), is(Integer.valueOf(1)));

		ThreeWayDiffEntry entry = result.get(0);
		assertThat(entry.getDirection(), is(Direction.OUTGOING));
		assertThat(entry.getChangeType(), is(ChangeType.MODIFY));
		assertThat(entry.getPath(), is("a.txt"));
	}

	@Test
	public void shouldListConflictingModify() throws Exception {
		// given
		writeTrashFile("a.txt", "content");
		Git git = new Git(db);
		git.add().addFilepattern("a.txt").call();
		RevCommit c = git.commit().setMessage("initial commit").call();
		writeTrashFile("a.txt", "new line");
		RevCommit c1 = git.commit().setAll(true).setMessage("second commit")
				.call();

		// when
		TreeWalk walk = new TreeWalk(db);
		walk.addTree(c1.getTree());
		walk.addTree(c.getTree());
		walk.addTree(c1.getTree());
		List<ThreeWayDiffEntry> result = ThreeWayDiffEntry.scan(walk);

		// then
		assertThat(result, notNullValue());
		assertThat(Integer.valueOf(result.size()), is(Integer.valueOf(1)));

		ThreeWayDiffEntry entry = result.get(0);
		assertThat(entry.getDirection(), is(Direction.CONFLICTING));
		assertThat(entry.getChangeType(), is(ChangeType.MODIFY));
		assertThat(entry.getPath(), is("a.txt"));
	}

	private File writeTrashFile(final String name, final String data)
			throws IOException {
		File path = new File(db.getWorkTree(), name);
		write(path, data);
		return path;
	}

}
