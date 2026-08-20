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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.RepositoryUtil;
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
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
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

	private static final String SUBMODULE_PATH = "sub";

	private File repositoryFile;

	@Before
	public void before() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
	}

	@Test
	public void updateSubmodule() throws Exception {
		Repository repo = shareAndAddSubmodule();
		ObjectId repoHead = repo.resolve(Constants.HEAD);
		Repository subRepo = lookupSubmodule(repo);

		Ref head = subRepo.exactRef(Constants.HEAD);
		assertNotNull(head);
		assertTrue(head.isSymbolic());
		assertEquals(Constants.R_HEADS + Constants.MASTER, head.getLeaf()
				.getName());
		assertEquals(repoHead, head.getObjectId());

		selectSubmodulesNode();
		ContextMenuHelper.clickContextMenuSync(getOrOpenView().bot().tree(),
				myUtil.getPluginLocalizedValue(
						UPDATE_SUBMODULE_CONTEXT_MENU_LABEL));
		TestUtil.joinJobs(JobFamilies.SUBMODULE_UPDATE);
		refreshAndWait();

		head = subRepo.exactRef(Constants.HEAD);
		assertNotNull(head);
		assertFalse(head.isSymbolic());
		assertEquals(repoHead, head.getObjectId());
	}

	// Discarding uncommitted changes in a submodule schedules its own job. The
	// submodule update must not run concurrently with it, otherwise both
	// operations check out into the same work tree and fail on the index lock.
	@Test
	public void updateSubmoduleWaitsForDiscardChanges() throws Exception {
		Repository repo = shareAndAddSubmodule();
		Repository subRepo = lookupSubmodule(repo);
		// The submodule is not shared as a project, so its scheduling rule is a
		// RepositoryRule rather than a rule on workspace projects.
		Files.write(new File(subRepo.getWorkTree(), FILE1_PATH).toPath(),
				"changed in submodule".getBytes(StandardCharsets.UTF_8));

		List<Job> discardJobs = Collections.synchronizedList(new ArrayList<>());
		List<Job> updateJobs = Collections.synchronizedList(new ArrayList<>());
		JobChangeAdapter listener = new JobChangeAdapter() {

			@Override
			public void scheduled(IJobChangeEvent event) {
				Job job = event.getJob();
				if (job.belongsTo(JobFamilies.DISCARD_CHANGES)) {
					discardJobs.add(job);
				} else if (job.belongsTo(JobFamilies.SUBMODULE_UPDATE)) {
					updateJobs.add(job);
				}
			}
		};
		Job.getJobManager().addJobChangeListener(listener);
		try {
			selectSubmodulesNode();
			ContextMenuHelper.clickContextMenu(getOrOpenView().bot().tree(),
					myUtil.getPluginLocalizedValue(
							UPDATE_SUBMODULE_CONTEXT_MENU_LABEL));
			SWTBotShell cleanup = bot.shell(MessageFormat.format(
					UIText.SubmoduleUpdateCommand_UncommittedChanges,
					RepositoryUtil.INSTANCE.getRepositoryName(subRepo)));
			cleanup.bot()
					.button(UIText.BranchResultDialog_buttonDiscardChanges)
					.click();
			TestUtil.joinJobs(JobFamilies.DISCARD_CHANGES);
			TestUtil.joinJobs(JobFamilies.SUBMODULE_UPDATE);
		} finally {
			Job.getJobManager().removeJobChangeListener(listener);
		}

		assertEquals("Expected one discard changes job", 1, discardJobs.size());
		assertEquals("Expected one submodule update job", 1, updateJobs.size());
		ISchedulingRule discardRule = discardJobs.get(0).getRule();
		ISchedulingRule updateRule = updateJobs.get(0).getRule();
		assertNotNull(discardRule);
		assertNotNull(updateRule);
		assertTrue(
				"Submodule update must not run concurrently with discarding changes in the submodule",
				updateRule.isConflicting(discardRule));
	}

	private Repository shareAndAddSubmodule() throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ1, false);
		clearView();
		RepositoryUtil.INSTANCE.addConfiguredRepository(repositoryFile);
		shareProjects(repositoryFile);
		assertProjectExistence(PROJ1, true);
		refreshAndWait();
		assertHasRepo(repositoryFile);
		Repository repo = lookupRepository(repositoryFile);

		SubmoduleAddCommand command = new SubmoduleAddCommand(repo);
		command.setPath(SUBMODULE_PATH);
		command.setURI(new URIish(repo.getDirectory().toURI().toString())
				.toString());
		command.call().close();
		refreshAndWait();
		return repo;
	}

	private static Repository lookupSubmodule(Repository repo)
			throws Exception {
		Repository subRepo = SubmoduleWalk.getSubmoduleRepository(repo,
				SUBMODULE_PATH);
		assertNotNull(subRepo);
		return subRepo;
	}

	private void selectSubmodulesNode() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem item = TestUtil.expandAndWait(tree.getAllItems()[0]);
		TestUtil.expandAndWait(item.getNode(
				UIText.RepositoriesViewLabelProvider_SubmodulesNodeText))
				.select();
	}
}
