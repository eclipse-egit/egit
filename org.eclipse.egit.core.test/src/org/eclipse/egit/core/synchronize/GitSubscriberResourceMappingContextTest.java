package org.eclipse.egit.core.synchronize;

import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
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
	public void hasRemoteChanges() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1.sample");
		File file2 = testRepo.createFile(iProject, "file2.sample");

		String initialContent1 = "some content for the first file";
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(repoRelativePath1, iFile1, branchChanges
				+ initialContent1, "branch commit");
		setContentsAndCommit(repoRelativePath2, iFile2, branchChanges
				+ initialContent2, "branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(repoRelativePath1, iFile1, initialContent1
				+ masterChanges, "master commit");
		setContentsAndCommit(repoRelativePath2, iFile2, initialContent2
				+ masterChanges, "master commit");

		RemoteResourceMappingContext context = prepareContext(MASTER, BRANCH);
		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iFile2, new NullProgressMonitor()));

		assertTrue(context.hasRemoteChange(iFile1, new NullProgressMonitor()));
	}

	private RevCommit setContentsAndCommit(String repoRelativePath,
			IFile targetFile, String newContents, String commitMessage)
			throws Exception {
		targetFile.setContents(
				new ByteArrayInputStream(newContents.getBytes()),
				IResource.FORCE, new NullProgressMonitor());
		new Git(testRepo.getRepository()).add()
				.addFilepattern(repoRelativePath).call();
		testRepo.addToIndex(targetFile);
		return testRepo.commit(commitMessage);
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
}
