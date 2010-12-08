package org.eclipse.egit.ui.wizards.clone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.common.GitImportRepoWizard;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.common.RepoRemoteBranchesPage;
import org.eclipse.egit.ui.common.WorkingCopyPage;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.junit.AfterClass;
import org.junit.Before;

public abstract class GitCloneWizardTestBase extends LocalRepositoryTestCase {

	protected static final int NUMBER_RANDOM_COMMITS = 100;
	protected static SampleTestRepository r;
	protected GitImportRepoWizard importWizard;

	@AfterClass
	public static void tearDown() throws Exception {
		r.shutDown();
	}

	public GitCloneWizardTestBase() {
		super();
	}

	protected void cloneRepo(File destRepo, RepoRemoteBranchesPage remoteBranches) throws Exception {
		remoteBranches.assertRemoteBranches(SampleTestRepository.FIX, Constants.MASTER);
		remoteBranches.selectBranches(SampleTestRepository.FIX, Constants.MASTER);

		WorkingCopyPage workingCopy = remoteBranches.nextToWorkingCopy();
		workingCopy.setDirectory(destRepo.toString());

		workingCopy.assertDirectory(destRepo.toString());
		workingCopy.assertBranch(Constants.MASTER);
		workingCopy.assertRemoteName(Constants.DEFAULT_REMOTE_NAME);
		workingCopy.waitForCreate();

		// Some random sampling to see we got something. We do not test
		// the integrity of the repository here. Only a few basic properties
		// we'd expect from a clone made this way, that would possibly
		// not hold true given other parameters in the GUI.
		Repository repository = new FileRepository(new File(destRepo, Constants.DOT_GIT));
		// we always have an origin/master
		assertNotNull(repository.resolve("origin/master"));
		// and a local master initialized from origin/master (default!)
		assertEquals(repository.resolve("master"), repository
				.resolve("origin/master"));
		// A well known tag
		assertNotNull(repository.resolve(Constants.R_TAGS + SampleTestRepository.v1_0_name).name());
		// lots of refs
		int refs = repository.getAllRefs().size();
		assertTrue(refs >= 4);
		// and a known file in the working dir
		assertTrue(new File(destRepo, SampleTestRepository.A_txt_name).exists());
		DirCacheEntry fileEntry = null;
		DirCache dc = repository.lockDirCache();
		fileEntry = dc.getEntry(SampleTestRepository.A_txt_name);
		dc.unlock();
		// check that we have the file in the index
		assertNotNull(fileEntry);
		// No project has been imported
		assertEquals(0,
				ResourcesPlugin.getWorkspace().getRoot().getProjects().length);
	}

	@Before
	public void setupViews() {
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();
		importWizard = new GitImportRepoWizard();
	}

}