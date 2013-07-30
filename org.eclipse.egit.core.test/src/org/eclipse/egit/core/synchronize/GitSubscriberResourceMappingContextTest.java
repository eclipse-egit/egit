package org.eclipse.egit.core.synchronize;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;

public class GitSubscriberResourceMappingContextTest extends GitTestCase {

	private static final String MASTER = Constants.R_HEADS + Constants.MASTER;

	private static final String BRANCH = Constants.R_HEADS + "branch";

	private Repository repo;

	private IProject iProject;

	private TestRepository testRepo;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();

		// make initial commit
		new Git(repo).commit().setAuthor("JUnit", "junit@jgit.org")
				.setMessage("Initial commit").call();
	}

	@Test
	public void hasLocalChange() throws Exception {
		File file1 = testRepo.createFile(iProject, "a.txt");
		File file2 = testRepo.createFile(iProject, "b.txt");
		testRepo.appendContentAndCommit(iProject, file1, "content a",
				"commit a");
		testRepo.appendContentAndCommit(iProject, file2, "content b",
				"commit b");
		JGitTestUtil.write(file1, "changed content a");
		JGitTestUtil.write(file2, "changed content b");
		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		RemoteResourceMappingContext context = prepareContext(MASTER, MASTER);
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasLocalChange(iFile2, new NullProgressMonitor()));

		JGitTestUtil.write(file2, "content b");
		refresh(context, iFile2);
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
		assertFalse(context.hasLocalChange(iFile2, new NullProgressMonitor()));
	}

	@Test
	public void hasLocalChangeInNewFolder() throws Exception {
		File file2 = testRepo.createFile(iProject, "folder/b.txt");
		RemoteResourceMappingContext context = prepareContext(MASTER, MASTER);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		assertTrue(context.hasLocalChange(iFile2, new NullProgressMonitor()));

		testRepo.addToIndex(iProject, file2);
		refresh(context, iFile2);
		assertTrue(context.hasLocalChange(iFile2, new NullProgressMonitor()));

		JGitTestUtil.write(file2, "changed content b");
		refresh(context, iFile2);
		assertTrue(context.hasLocalChange(iFile2, new NullProgressMonitor()));
	}

	@Test
	public void hasRemoteChanges() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1.sample");
		File file2 = testRepo.createFile(iProject, "file2.sample");

		testRepo.appendContentAndCommit(iProject, file1,
				"initial content - file 1",
				"first file - initial commit MASTER");
		testRepo.appendContentAndCommit(iProject, file2,
				"initial content - file 2",
				"second file - initial commit MASTER");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		setContentsAndCommit(repoRelativePath1, iFile1,
				"change in branch - file 1", "branch commit - file1");
		setContentsAndCommit(repoRelativePath2, iFile2,
				"change in branch - file 2", "branch commit - file2");

		testRepo.checkoutBranch(MASTER);

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile2, new NullProgressMonitor()));
		assertFalse(context.hasLocalChange(iFile2, new NullProgressMonitor()));

		setContentsAndCommit(repoRelativePath1, iFile1,
				"change in master - file 1",
				"first file - second commit MASTER");
		refresh(context, iFile1);
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile2, new NullProgressMonitor()));
		assertFalse(context.hasLocalChange(iFile2, new NullProgressMonitor()));

		setContents(iFile2, "change in branch - file 2");
		refresh(context, iFile2);
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertFalse(context.hasRemoteChange(iFile2, new NullProgressMonitor()));
		assertFalse(context.hasLocalChange(iFile2, new NullProgressMonitor()));

		setContentsAndCommit(repoRelativePath2, iFile2,
				"change in branch - file 2",
				"change in master (same as in branch) - file 2");
		refresh(context, iFile2);
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertFalse(context.hasRemoteChange(iFile2, new NullProgressMonitor()));
		assertFalse(context.hasLocalChange(iFile2, new NullProgressMonitor()));
	}

	@Test
	public void hasRemoteChangeInNewFile() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1.sample");
		String initialContent1 = "some content for the first file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		File file2 = testRepo.createFile(iProject, "file2.sample");
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.checkoutBranch(MASTER);

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertFalse(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile2, new NullProgressMonitor()));
	}

	@Test
	public void hasRemoteChangeInNewFolder() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1.sample");
		String initialContent1 = "some content for the first file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		IFile iFile1 = testRepo.getIFile(iProject, file1);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		iProject.getFolder("folder").create(true, true,
				new NullProgressMonitor());
		File file2 = testRepo.createFile(iProject, "folder/file2.sample");
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.checkoutBranch(MASTER);

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertFalse(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile2, new NullProgressMonitor()));
	}

	@Test
	public void hasLocalAndRemoteChange() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1.sample");
		testRepo.appendContentAndCommit(iProject, file1, "initial content",
				"first commit in master");
		IFile iFile1 = testRepo.getIFile(iProject, file1);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);
		setContentsAndCommit(repoRelativePath1, iFile1,
				"changed content in branch", "first commit in BRANCH");

		testRepo.checkoutBranch(MASTER);
		setContentsAndCommit(repoRelativePath1, iFile1,
				"changed content in master", "second commit in MASTER");

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
	}

	@Test
	public void hasLocalAndRemoteChangeInSubFolder() throws Exception {
		File file1 = testRepo.createFile(iProject, "folder/file1.sample");
		testRepo.appendContentAndCommit(iProject, file1, "initial content",
				"first commit in master");
		IFile iFile1 = testRepo.getIFile(iProject, file1);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);
		setContentsAndCommit(repoRelativePath1, iFile1,
				"changed content in branch", "first commit in BRANCH");

		testRepo.checkoutBranch(MASTER);
		setContentsAndCommit(repoRelativePath1, iFile1,
				"changed content in master", "second commit in MASTER");

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasLocalChange(iFile1, new NullProgressMonitor()));
	}

	private RevCommit setContentsAndCommit(String repoRelativePath,
			IFile targetFile, String newContents, String commitMessage)
			throws Exception {
		setContents(targetFile, newContents);
		return addAndCommit(targetFile, repoRelativePath, commitMessage);
	}

	private RevCommit addAndCommit(IFile targetFile, String repoRelativePath,
			String commitMessage) throws Exception {
		new Git(testRepo.getRepository()).add()
				.addFilepattern(repoRelativePath).call();
		testRepo.addToIndex(targetFile);
		return testRepo.commit(commitMessage);
	}

	private void setContents(IFile targetFile, String newContents)
			throws CoreException {
		targetFile.setContents(
				new ByteArrayInputStream(newContents.getBytes()),
				IResource.FORCE, new NullProgressMonitor());
	}

	private RemoteResourceMappingContext prepareContext(String srcRev,
			String dstRev) throws Exception {
		GitSynchronizeData gsd = new GitSynchronizeData(repo, srcRev, dstRev,
				true);
		GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(gsd);
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsds);
		subscriber.init(new NullProgressMonitor());

		return new GitSubscriberResourceMappingContext(subscriber, gsds);
	}

	private void refresh(RemoteResourceMappingContext context, IFile... file)
			throws CoreException {
		context.refresh(new ResourceTraversal[] { new ResourceTraversal(file,
				1, 0) }, 0, new NullProgressMonitor());
	}
}
