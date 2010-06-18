package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.JGitInternalException;
import org.eclipse.jgit.api.NoHeadException;
import org.eclipse.jgit.api.NoMessageException;
import org.eclipse.jgit.api.WrongRepositoryStateException;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.storage.file.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class T0004_BranchOperationTest extends GitTestCase{

	private static final String TEST = Constants.R_HEADS + "test";
	private static final String MASTER = Constants.R_HEADS + "master";
	Repository repository;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		repository = new Repository(gitDir);
		repository.create();
	}

	@After
	public void tearDown() throws Exception {
		repository.close();
		repository = null;
		super.tearDown();
	}


	@Test
	public void testBranchOperation() throws Exception {
		// create first commit containing a dummy file
		createInitialCommit();
		// create branch test and switch to branch test
		createBranch(MASTER, TEST);
		new BranchOperation(repository, TEST).execute(null);
		assertTrue(repository.getFullBranch().equals(TEST));
		// add .project to version control and commit
		String path = project.getProject().getLocation().append(".project").toOSString();
		File file = new File(path);
		track(file);
		commit("Add .project file");
		// switch back to master branch
		// .project must disappear, related Eclipse project must be deleted
		new BranchOperation(repository, MASTER).execute(null);
		assertFalse(file.exists());
		assertFalse(project.getProject().exists());
		// switch back to master test
		// .project must reappear
		new BranchOperation(repository, TEST).execute(null);
		assertTrue(file.exists());
	}

	private void createInitialCommit() throws IOException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, JGitInternalException, WrongRepositoryStateException {
		String repoPath = project.getProject().getWorkspace().getRoot().getLocation().toOSString();
		File file = new File(repoPath, "dummy");
		file.createNewFile();
		track(file);
		commit("testBranchOperation\n\nfirst commit\n");
	}

	private void createBranch(String refName, String newRefName) throws IOException {
		RefUpdate updateRef;
		updateRef = repository.updateRef(newRefName);
		Ref startRef = repository.getRef(refName);
		ObjectId startAt = repository.resolve(refName);
		String startBranch;
		if (startRef != null)
			startBranch = refName;
		else
			startBranch = startAt.name();
		startBranch = repository.shortenRefName(startBranch);
		updateRef.setNewObjectId(startAt);
		updateRef.setRefLogMessage("branch: Created from " + startBranch, false); //$NON-NLS-1$
		updateRef.update();
	}

	private void track(File file) throws IOException {
		GitIndex index = repository.getIndex();
		Entry entry = index.add(repository.getWorkDir(), file);
		entry.setAssumeValid(false);
		index.write();
	}

	private void commit(String message) throws NoHeadException, NoMessageException, UnmergedPathException, ConcurrentRefUpdateException, JGitInternalException, WrongRepositoryStateException {
		Git git = new Git(repository);
		CommitCommand commitCommand = git.commit();
		commitCommand.setAuthor("J. Git", "j.git@egit.org");
		commitCommand.setCommitter(commitCommand.getAuthor());
		commitCommand.setMessage(message);
		commitCommand.call();
	}

}
