/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.clone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.ui.common.EGitTestCase;
import org.eclipse.egit.ui.common.GitImportRepoWizard;
import org.eclipse.egit.ui.common.RepoPropertiesPage;
import org.eclipse.egit.ui.common.RepoRemoteBranchesPage;
import org.eclipse.egit.ui.common.WorkingCopyPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class GitCloneWizardTest extends EGitTestCase {

	private static final int NUMBER_RANDOM_COMMITS = 100;

	private static SampleTestRepository r;

	private GitImportRepoWizard importWizard;

	@BeforeClass
	public static void setup() throws Exception {
		r = new SampleTestRepository(NUMBER_RANDOM_COMMITS);
	}

	@AfterClass
	public static void tearDown() throws IOException {
		r.shutDown();
	}

	@Test
	public void updatesParameterFieldsInImportDialogWhenURIIsUpdated()
			throws Exception {

		importWizard.openWizard();
		RepoPropertiesPage propertiesPage = importWizard.openCloneWizard();

		propertiesPage.setURI("git://www.jgit.org/EGIT");
		propertiesPage.assertSourceParams(null, "www.jgit.org", "/EGIT", "git",
				"", true, "", "", false, false);

		propertiesPage.appendToURI("X");

		propertiesPage.assertSourceParams(null, "www.jgit.org", "/EGITX",
				"git", "", true, "", "", false, false);

		propertiesPage.setURI("git://www.jgit.org/EGIT");
		propertiesPage.assertSourceParams(null, "www.jgit.org", "/EGIT", "git",
				"", true, "", "", false, false);

		propertiesPage.setURI("git://user:hi@www.jgit.org/EGIT");
		propertiesPage.assertSourceParams(
				" User not supported on git protocol.", "www.jgit.org",
				"/EGIT", "git", "", true, "user", "hi", false, false);

		// UI doesn't change URI even when password is entered in clear text as
		// part of URI. Opinions on this may vary.
		propertiesPage.assertURI("git://user:hi@www.jgit.org/EGIT");

		propertiesPage.setURI("ssh://user@www.jgit.org/EGIT");
		propertiesPage.assertSourceParams(null, "www.jgit.org", "/EGIT",
				"ssh", "", true, "user", "", true, true);

		propertiesPage.setURI("ssh://user@www.jgit.org/EGIT");
		propertiesPage.assertSourceParams(null, "www.jgit.org", "/EGIT",
				"ssh", "", true, "user", "", true, true);

		propertiesPage.setURI("ssh://user:hi@www.jgit.org:33/EGIT");
		propertiesPage.assertSourceParams(null, "www.jgit.org", "/EGIT",
				"ssh", "33", true, "user", "hi", true, true);

		propertiesPage.setURI("ssh:///EGIT");
		propertiesPage.assertSourceParams(" Host required for ssh protocol.",
				"", "/EGIT", "ssh", "", true, "", "", true, true);

		propertiesPage.setURI("file:///some/place");
		if (Platform.getOS().equals(Platform.OS_WIN32))
			propertiesPage.assertSourceParams(" "
					+ System.getProperty("user.dir")
					+ "\\.\\some\\place does not exist.", "", "/some/place",
					"file", "", false, "", "", false, false);
		else
			propertiesPage.assertSourceParams(" /some/place does not exist.",
					"", "/some/place", "file", "", false, "", "", false, false);

		// Now try changing some fields other than URI and see how the URI field
		// gets changed
		propertiesPage.setURI("ssh://user@www.jgit.org/EGIT");

		// ..change host
		bot.textWithLabel("Host:").setText("example.com");
		propertiesPage.assertURI("ssh://user@example.com/EGIT");

		propertiesPage.assertSourceParams(null, "example.com", "/EGIT",
				"ssh", "", true, "user", "", true, true);

		// ..change user
		bot.textWithLabel("User:").setText("gitney");
		propertiesPage.assertURI("ssh://gitney@example.com/EGIT");
		propertiesPage.assertSourceParams(null, "example.com", "/EGIT",
				"ssh", "", true, "gitney", "", true, true);

		// ..change password
		bot.textWithLabel("Password:").setText("fsck");
		// Password is not written into the URL here!
		propertiesPage.assertURI("ssh://gitney@example.com/EGIT");
		propertiesPage.assertSourceParams(null, "example.com", "/EGIT",
				"ssh", "", true, "gitney", "fsck", true, true);

		// change port number
		bot.textWithLabel("Port:").setText("99");
		propertiesPage.assertURI("ssh://gitney@example.com:99/EGIT");
		propertiesPage.assertSourceParams(null, "example.com", "/EGIT",
				"ssh", "99", true, "gitney", "fsck", true, true);

		// change protocol to another with user/password capability
		bot.comboBoxWithLabel("Protocol:").setSelection("ftp");
		propertiesPage.assertURI("ftp://gitney@example.com:99/EGIT");
		propertiesPage.assertSourceParams(null, "example.com", "/EGIT", "ftp",
				"99", true, "gitney", "fsck", true, true);

		// change protocol to one without user/password capability
		bot.comboBoxWithLabel("Protocol:").setSelection("git");
		propertiesPage.assertURI("git://gitney@example.com:99/EGIT");
		propertiesPage.assertSourceParams(
				" User not supported on git protocol.", "example.com", "/EGIT",
				"git", "99", true, "gitney", "fsck", false, false);

		// change protocol to one without host capability
		bot.comboBoxWithLabel("Protocol:").setSelection("file");
		propertiesPage.assertURI("file://gitney@example.com:99/EGIT");
		propertiesPage.assertSourceParams(
				" Host not supported on file protocol.", "example.com",
				"/EGIT", "file", "99", false, "gitney", "fsck", false, false);

		// Local protocol with file: prefix. We need to make sure the
		// local path exists as a directory so we choose user.home as
		// that one should exist.
		if (Platform.getOS().equals(Platform.OS_WIN32))
			propertiesPage.setURI("file:///" + System.getProperty("user.home"));
		else
			propertiesPage.setURI("file://" + System.getProperty("user.home"));
		propertiesPage.assertSourceParams(null, "", System.getProperty(
				"user.home").replace('\\', '/'), "file", "", false, "", "",
				false, false);

		// Local protocol without file: prefix
		propertiesPage.setURI(System.getProperty("user.home"));
		propertiesPage.assertSourceParams(null, "", System.getProperty(
				"user.home").replace('\\', '/'), "file", "", false, "", "",
				false, false);

		// On windows the use can choose forward or backward slashes, so add
		// a case for forward slashes using the non prefixed local protocol.
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			propertiesPage.setURI(System.getProperty("user.home").replace('\\',
					'/'));
			propertiesPage.assertSourceParams(null, "", System.getProperty(
					"user.home").replace('\\', '/'), "file", "", false, "", "",
					false, false);
		}
		bot.button("Cancel").click();
	}

	@SuppressWarnings("boxing")
	@Test
	public void canCloneARemoteRepo() throws Exception {
		File destRepo = new File(ResourcesPlugin.getWorkspace()
				.getRoot().getLocation().toFile(), "test1");

		importWizard.openWizard();
		RepoPropertiesPage propertiesPage = importWizard.openCloneWizard();

		RepoRemoteBranchesPage remoteBranches = propertiesPage
				.nextToRemoteBranches(r.getUri());

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
		Repository repository = new Repository(new File(destRepo, Constants.DOT_GIT));
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
		assertFalse(repository.getIndex().isChanged());
		assertFalse(repository.getIndex().getEntry(SampleTestRepository.A_txt_name).isModified(
				destRepo));
		// No project have been imported
		assertEquals(0,
				ResourcesPlugin.getWorkspace().getRoot().getProjects().length);

	}

	@Test
	public void clonedRepositoryShouldExistOnFileSystem() throws Exception {
		importWizard.openWizard();
		RepoPropertiesPage repoProperties = importWizard.openCloneWizard();
		RepoRemoteBranchesPage remoteBranches = repoProperties
				.nextToRemoteBranches(r.getUri());
		remoteBranches.assertRemoteBranches(SampleTestRepository.FIX, Constants.MASTER);
		WorkingCopyPage workingCopy = remoteBranches.nextToWorkingCopy();
		workingCopy.assertWorkingCopyExists(r.getUri());
	}

	@Test
	public void alteringSomeParametersDuringClone() throws Exception {
		File destRepo = new File(ResourcesPlugin.getWorkspace()
				.getRoot().getLocation().toFile(), "test2");

		importWizard.openWizard();
		RepoPropertiesPage repoProperties = importWizard.openCloneWizard();
		RepoRemoteBranchesPage remoteBranches = repoProperties
				.nextToRemoteBranches(r.getUri());
		remoteBranches.deselectAllBranches();
		remoteBranches
				.assertErrorMessage("At least one branch must be selected.");
		remoteBranches.assertNextIsDisabled();

		// TODO the "fix" branch alone is cloning too fast if delay is used
		// remoteBranches.selectBranches(SampleTestRepository.FIX);
		remoteBranches.selectBranches(SampleTestRepository.FIX, Constants.MASTER);
		
		remoteBranches.assertNextIsEnabled();

		WorkingCopyPage workingCopy = remoteBranches.nextToWorkingCopy();
		workingCopy.setDirectory(destRepo.toString());
		// TODO see above
		// workingCopy.assertBranch(SampleTestRepository.FIX);
		workingCopy.setRemoteName("src");
		workingCopy.waitForCreate();

		// Some random sampling to see we got something. We do not test
		// the integrity of the repository here. Only a few basic properties
		// we'd expect from a clone made this way, that would possibly
		// not hold true given other parameters in the GUI.
		Repository repository = new Repository(new File(destRepo, Constants.DOT_GIT));
		assertNotNull(repository.resolve("src/" + SampleTestRepository.FIX));
		// we didn't clone that one
		// TODO above
		//assertNull(repository.resolve("src/master"));
		// and a local master initialized from origin/master (default!)
		assertEquals(repository.resolve("stable"), repository
				.resolve("src/stable"));
		// A well known tag
		assertNotNull(repository.resolve(Constants.R_TAGS + SampleTestRepository.v2_0_name).name());
		// lots of refs
		assertTrue(repository.getAllRefs().size() >= 4);
	}

	@Test
	public void invalidHostnameFreezesDialog() throws Exception {
		importWizard.openWizard();
		RepoPropertiesPage repoProperties = importWizard.openCloneWizard();
		RepoRemoteBranchesPage remoteBranches = repoProperties
				.nextToRemoteBranches("git://no.example.com/EGIT");
		remoteBranches
				.assertErrorMessage("git://no.example.com/EGIT: unknown host");
		remoteBranches.assertCannotProceed();
		remoteBranches.cancel();
	}

	// TODO: Broken, seems that this takes forever and does not come back with
	// an error. Perhaps set a higher timeout for this test ?
	@Ignore
	public void invalidPortFreezesDialog() throws Exception {
		importWizard.openWizard();
		RepoPropertiesPage repoProperties = importWizard.openCloneWizard();
		RepoRemoteBranchesPage remoteBranches = repoProperties
				.nextToRemoteBranches("git://localhost:80/EGIT");
		remoteBranches
				.assertErrorMessage("git://localhost:80/EGIT: not found.");
		remoteBranches.assertCannotProceed();
		remoteBranches.cancel();
	}

	// TODO: Broken, seems that this takes forever and does not come back with
	// an error. Perhaps set a higher timeout for this test ?
	@Ignore
	public void timeoutToASocketFreezesDialog() throws Exception {
		importWizard.openWizard();
		RepoPropertiesPage repoProperties = importWizard.openCloneWizard();
		RepoRemoteBranchesPage remoteBranches = repoProperties
				.nextToRemoteBranches("git://www.example.com/EGIT");
		remoteBranches
				.assertErrorMessage("git://www.example.com/EGIT: Connection timed out");
		remoteBranches.assertCannotProceed();
		remoteBranches.cancel();
	}

	@Before
	public void setupViews() {
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();
		importWizard = new GitImportRepoWizard();
	}

}
