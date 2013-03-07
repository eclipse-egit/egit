/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.submodule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestBase;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests of running a submodule sync
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class SubmoduleSyncTest extends GitRepositoriesViewTestBase {

	private static final String SYNC_SUBMODULE_CONTEXT_MENU_LABEL = "SubmoduleSyncCommand.label";

	private File repositoryFile;

	@Before
	public void before() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
	}

	@Test
	public void syncSubmodule() throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ1, false);
		clearView();
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
		shareProjects(repositoryFile);
		assertProjectExistence(PROJ1, true);
		refreshAndWait();
		assertHasRepo(repositoryFile);
		FileRepository repo = lookupRepository(repositoryFile);

		SubmoduleAddCommand command = new SubmoduleAddCommand(repo);
		String path = "sub";
		command.setPath(path);
		String uri = new URIish(repo.getDirectory().toURI().toString())
				.toString();
		command.setURI(uri);
		Repository subRepo = command.call();
		assertNotNull(subRepo);

		String newUri = "git://server/repo.git";
		File modulesFile = new File(repo.getWorkTree(),
				Constants.DOT_GIT_MODULES);
		FileBasedConfig config = new FileBasedConfig(modulesFile, repo.getFS());
		config.load();
		config.setString(ConfigConstants.CONFIG_SUBMODULE_SECTION, path,
				ConfigConstants.CONFIG_KEY_URL, newUri);
		config.save();

		assertEquals(
				uri,
				repo.getConfig().getString(
						ConfigConstants.CONFIG_SUBMODULE_SECTION, path,
						ConfigConstants.CONFIG_KEY_URL));
		assertEquals(
				uri,
				subRepo.getConfig().getString(
						ConfigConstants.CONFIG_REMOTE_SECTION,
						Constants.DEFAULT_REMOTE_NAME,
						ConfigConstants.CONFIG_KEY_URL));

		refreshAndWait();
		SWTBotTree tree = getOrOpenView().bot().tree();
		tree.getAllItems()[0]
				.expand()
				.expandNode(
						UIText.RepositoriesViewLabelProvider_SubmodulesNodeText)
				.select();
		ContextMenuHelper.clickContextMenuSync(tree, myUtil
				.getPluginLocalizedValue(SYNC_SUBMODULE_CONTEXT_MENU_LABEL));
		TestUtil.joinJobs(JobFamilies.SUBMODULE_SYNC);
		refreshAndWait();

		assertEquals(
				newUri,
				repo.getConfig().getString(
						ConfigConstants.CONFIG_SUBMODULE_SECTION, path,
						ConfigConstants.CONFIG_KEY_URL));
		assertEquals(
				newUri,
				subRepo.getConfig().getString(
						ConfigConstants.CONFIG_REMOTE_SECTION,
						Constants.DEFAULT_REMOTE_NAME,
						ConfigConstants.CONFIG_KEY_URL));
	}
}
