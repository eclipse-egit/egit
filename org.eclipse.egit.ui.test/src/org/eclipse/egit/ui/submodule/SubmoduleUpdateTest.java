/******************************************************************************
 *  Copyright (c) 2012, 2013 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Laurent Goubet <laurent.goubet@obeo.fr - 404121
 *****************************************************************************/
package org.eclipse.egit.ui.submodule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestBase;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for running a submodule update
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class SubmoduleUpdateTest extends GitRepositoriesViewTestBase {

	private static final String UPDATE_SUBMODULE_CONTEXT_MENU_LABEL = "SubmoduleUpdateCommand.label";

	private File repositoryFile;

	@Before
	public void before() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
	}

	@Test
	public void updateSubmodule() throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ1, false);
		clearView();
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
		shareProjects(repositoryFile);
		assertProjectExistence(PROJ1, true);
		refreshAndWait();
		assertHasRepo(repositoryFile);
		Repository repo = lookupRepository(repositoryFile);
		ObjectId repoHead = repo.resolve(Constants.HEAD);

		SubmoduleAddCommand command = new SubmoduleAddCommand(repo);
		String path = "sub";
		command.setPath(path);
		String uri = new URIish(repo.getDirectory().toURI().toString())
				.toString();
		command.setURI(uri);
		Repository subRepo = command.call();
		assertNotNull(subRepo);
		subRepo.close();

		Ref head = subRepo.exactRef(Constants.HEAD);
		assertNotNull(head);
		assertTrue(head.isSymbolic());
		assertEquals(Constants.R_HEADS + Constants.MASTER, head.getLeaf()
				.getName());
		assertEquals(repoHead, head.getObjectId());

		refreshAndWait();
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem item = TestUtil.expandAndWait(tree.getAllItems()[0]);
		TestUtil.expandAndWait(item.getNode(
				UIText.RepositoriesViewLabelProvider_SubmodulesNodeText))
				.select();
		ContextMenuHelper.clickContextMenuSync(tree, myUtil
				.getPluginLocalizedValue(UPDATE_SUBMODULE_CONTEXT_MENU_LABEL));
		TestUtil.joinJobs(JobFamilies.SUBMODULE_UPDATE);
		refreshAndWait();

		head = subRepo.exactRef(Constants.HEAD);
		assertNotNull(head);
		assertFalse(head.isSymbolic());
		assertEquals(repoHead, head.getObjectId());
	}
}
