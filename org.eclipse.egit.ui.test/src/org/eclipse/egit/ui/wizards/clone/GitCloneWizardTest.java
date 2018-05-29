/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.clone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.ui.common.RepoPropertiesPage;
import org.eclipse.egit.ui.common.RepoRemoteBranchesPage;
import org.eclipse.egit.ui.common.WorkingCopyPage;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.osgi.util.NLS;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class GitCloneWizardTest extends GitCloneWizardTestBase {

	@BeforeClass
	public static void setup() throws Exception {
		r = new SampleTestRepository(NUMBER_RANDOM_COMMITS, false);
	}

	@Test
	public void updatesParameterFieldsInImportDialogWhenURIIsUpdated()
			throws Exception {

		importWizard.openWizard();
		RepoPropertiesPage propertiesPage = importWizard.openRepoPropertiesPage();

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
				"/EGIT", "git", "", true, "", "", false, false);

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
		propertiesPage.assertURI("git://example.com:99/EGIT");
		propertiesPage.assertSourceParams(null, "example.com", "/EGIT",
				"git", "99", true, "", "", false, false);

		// change protocol to one without host capability
		bot.comboBoxWithLabel("Protocol:").setSelection("file");
		propertiesPage.assertURI("file:///EGIT");
		propertiesPage.assertSourceParams(" /EGIT does not exist.", "",
				"/EGIT",
				"file", "", false,
				"", "", false, false);

		// Local protocol with file: prefix. We need to make sure the
		// local path exists as a directory so we choose user.home as
		// that one should exist.
		if (Platform.getOS().equals(Platform.OS_WIN32))
			propertiesPage.setURI("file:///" + System.getProperty("user.home"));
		else
			propertiesPage.setURI("file://" + System.getProperty("user.home"));
		propertiesPage.assertSourceParams(null, "", System.getProperty(
				"user.home"), "file", "", false, "", "",
				false, false);

		// Local protocol without file: prefix
		propertiesPage.setURI(System.getProperty("user.home"));
		propertiesPage.assertSourceParams(null, "", System.getProperty(
				"user.home"), "file", "", false, "", "",
				false, false);

		// On windows the use can choose forward or backward slashes, so add
		// a case for forward slashes using the non prefixed local protocol.
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			propertiesPage.setURI(System.getProperty("user.home"));
			propertiesPage.assertSourceParams(null, "", System.getProperty(
					"user.home"), "file", "", false, "", "",
					false, false);
		}
		bot.button("Cancel").click();
	}

	@Test
	public void canCloneARemoteRepo() throws Exception {
		destRepo = new File(ResourcesPlugin.getWorkspace()
				.getRoot().getLocation().toFile(), "test1");

		importWizard.openWizard();
		RepoPropertiesPage propertiesPage = importWizard.openRepoPropertiesPage();

		RepoRemoteBranchesPage remoteBranches = propertiesPage
				.nextToRemoteBranches(r.getUri());

		cloneRepo(destRepo, remoteBranches);
		bot.button("Cancel").click();
	}

	@Test
	public void clonedRepositoryShouldExistOnFileSystem() throws Exception {
		importWizard.openWizard();
		RepoPropertiesPage repoProperties = importWizard.openRepoPropertiesPage();
		RepoRemoteBranchesPage remoteBranches = repoProperties
				.nextToRemoteBranches(r.getUri());
		remoteBranches.assertRemoteBranches(SampleTestRepository.FIX, Constants.MASTER);
		WorkingCopyPage workingCopy = remoteBranches.nextToWorkingCopy();
		workingCopy.assertWorkingCopyExists();
		bot.button("Cancel").click();
	}

	@Test
	public void alteringSomeParametersDuringClone() throws Exception {
		destRepo = new File(ResourcesPlugin.getWorkspace()
				.getRoot().getLocation().toFile(), "test2");

		importWizard.openWizard();
		RepoPropertiesPage repoProperties = importWizard.openRepoPropertiesPage();
		RepoRemoteBranchesPage remoteBranches = repoProperties
				.nextToRemoteBranches(r.getUri());
		remoteBranches.deselectAllBranches();
		remoteBranches
				.assertErrorMessage("At least one branch must be selected.");
		remoteBranches.assertNextIsDisabled();

		remoteBranches.selectBranches(SampleTestRepository.FIX);
		remoteBranches.assertNextIsEnabled();

		WorkingCopyPage workingCopy = remoteBranches.nextToWorkingCopy();
		workingCopy.setDirectory(destRepo.toString());
		workingCopy.assertBranch(SampleTestRepository.FIX);
		workingCopy.setRemoteName("src");
		workingCopy.waitForCreate();

		// Some random sampling to see we got something. We do not test
		// the integrity of the repository here. Only a few basic properties
		// we'd expect from a clone made this way, that would possibly
		// not hold true given other parameters in the GUI.
		Repository repository = FileRepositoryBuilder.create(new File(destRepo,
				Constants.DOT_GIT));
		assertNotNull(repository.resolve("src/" + SampleTestRepository.FIX));
		// we didn't clone that one
		assertNull(repository.resolve("src/master"));
		// and a local master initialized from origin/master (default!)
		assertEquals(repository.resolve("stable"), repository
				.resolve("src/stable"));
		// A well known tag
		assertNotNull(repository.resolve(Constants.R_TAGS + SampleTestRepository.v2_0_name).name());
		// lots of refs
		assertTrue(repository.getAllRefs().size() >= 4);
		bot.button("Cancel").click();
	}

	// TODO network timeouts seem to be longer on cental EGit build
	// Test is ignored to fix the build
	@Ignore
	@Test
	public void invalidHostnameFreezesDialog() throws Exception {
		importWizard.openWizard();
		RepoPropertiesPage repoProperties = importWizard.openRepoPropertiesPage();
		RepoRemoteBranchesPage remoteBranches = repoProperties
				.nextToRemoteBranches("git://no.example.com/EGIT");
		remoteBranches.assertErrorMessage(NLS.bind(
				UIText.SourceBranchPage_CompositeTransportErrorMessage,
				"Exception caught during execution of ls-remote command",
				"git://no.example.com/EGIT: unknown host"));
		remoteBranches.assertCannotProceed();
		remoteBranches.cancel();
	}

	// TODO network timeouts seem to be longer on cental EGit build
	// Test is ignored to fix the build
	@Ignore
	@Test
	public void invalidPortFreezesDialog() throws Exception {
		importWizard.openWizard();
		RepoPropertiesPage repoProperties = importWizard.openRepoPropertiesPage();
		RepoRemoteBranchesPage remoteBranches = repoProperties
				.nextToRemoteBranches("git://localhost:80/EGIT");
		remoteBranches.assertErrorMessage(NLS.bind(
				UIText.SourceBranchPage_CompositeTransportErrorMessage,
				"Exception caught during execution of ls-remote command",
				"git://localhost:80/EGIT: Connection refused"));
		remoteBranches.assertCannotProceed();
		remoteBranches.cancel();
	}

	// TODO: Broken, seems that this takes forever and does not come back with
	// an error. Perhaps set a higher timeout for this test ?
	@Ignore
	public void timeoutToASocketFreezesDialog() throws Exception {
		importWizard.openWizard();
		RepoPropertiesPage repoProperties = importWizard.openRepoPropertiesPage();
		RepoRemoteBranchesPage remoteBranches = repoProperties
				.nextToRemoteBranches("git://www.example.com/EGIT");
		remoteBranches
				.assertErrorMessage("git://www.example.com/EGIT: unknown host");
		remoteBranches.assertCannotProceed();
		remoteBranches.cancel();
	}

	@Test
	public void acceptCloneCommandAsURI() throws Exception {
		importWizard.openWizard();
		RepoPropertiesPage propertiesPage = importWizard
				.openRepoPropertiesPage();

		// remove git clone command
		propertiesPage.setURI("git clone git://www.jgit.org/EGIT");
		propertiesPage.assertSourceParams(null, "www.jgit.org", "/EGIT", "git",
				"", true, "", "", false, false);

		// leading and trailing whitespace should be stripped automatically
		propertiesPage.setURI(" git clone git://www.jgit.org/EGIT     ");
		propertiesPage.assertSourceParams(null, "www.jgit.org", "/EGIT", "git",
				"", true, "", "", false, false);

		// assert trimming works fine in all other places
		propertiesPage.setURI("   git clone    git://www.jgit.org/EGIT");
		propertiesPage.assertSourceParams(null, "www.jgit.org", "/EGIT", "git",
				"", true, "", "", false, false);

		bot.button("Cancel").click();
	}

	@Test
	public void gitStyleUriSelectsSshProtocol() throws Exception {
		importWizard.openWizard();

		RepoPropertiesPage propertiesPage = importWizard
				.openRepoPropertiesPage();

		propertiesPage.setURI("git@github.com:someone/somerepo.git");
		propertiesPage.assertSourceParams(null, "github.com",
				"someone/somerepo.git", "ssh", "", true, "git", "", true,
				true);
	}
}
