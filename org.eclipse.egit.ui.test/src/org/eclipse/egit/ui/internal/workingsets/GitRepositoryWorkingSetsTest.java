/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel <lars.vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.workingsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.op.DisconnectProviderOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkingSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitRepositoryWorkingSetsTest extends LocalRepositoryTestCase {

	private static final String PROJ3 = "ThirdProject";

	private IPreferenceStore store;

	@Before
	public void enableWorkingSets() throws Exception {
		store = Activator.getDefault().getPreferenceStore();
		store.setValue(UIPreferences.REPOSITORY_WORKING_SETS, true);
		GitRepositoryWorkingSets.getInstance().install();
		waitForUpdate();
	}

	@After
	public void disableWorkingSets() throws Exception {
		store.setToDefault(UIPreferences.REPOSITORY_WORKING_SETS);
		waitForUpdate();
	}

	@Test
	public void oneWorkingSetPerRepository() throws Exception {
		Repository repo1 = lookupRepository(
				createProjectAndCommitToRepository(REPO1));
		Repository repo2 = lookupRepository(
				createProjectAndCommitToRepository(REPO2, PROJ3));
		waitForUpdate();

		IWorkingSet first = GitRepositoryWorkingSets.getWorkingSet(repo1);
		assertNotNull(first);
		assertEquals(REPO1, first.getLabel());
		assertEquals(projects(PROJ1, PROJ2), elements(first));

		IWorkingSet second = GitRepositoryWorkingSets.getWorkingSet(repo2);
		assertNotNull(second);
		assertEquals(REPO2, second.getLabel());
		assertEquals(projects(PROJ3), elements(second));
	}

	@Test
	public void disconnectRemovesProjectAndEmptySet() throws Exception {
		Repository repo = lookupRepository(
				createProjectAndCommitToRepository(REPO1));
		waitForUpdate();
		assertEquals(projects(PROJ1, PROJ2),
				elements(GitRepositoryWorkingSets.getWorkingSet(repo)));

		disconnect(PROJ1);
		waitForUpdate();
		assertEquals(projects(PROJ2),
				elements(GitRepositoryWorkingSets.getWorkingSet(repo)));

		disconnect(PROJ2);
		waitForUpdate();
		assertNull(GitRepositoryWorkingSets.getWorkingSet(repo));
	}

	@Test
	public void disablingRemovesWorkingSets() throws Exception {
		Repository repo = lookupRepository(
				createProjectAndCommitToRepository(REPO1));
		waitForUpdate();
		assertNotNull(GitRepositoryWorkingSets.getWorkingSet(repo));

		store.setValue(UIPreferences.REPOSITORY_WORKING_SETS, false);
		waitForUpdate();
		assertNull(GitRepositoryWorkingSets.getWorkingSet(repo));
	}

	private static void disconnect(String projectName) throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		new DisconnectProviderOperation(List.of(project)).execute(null);
	}

	private static void waitForUpdate() throws Exception {
		TestUtil.joinJobs(org.eclipse.egit.core.JobFamilies.REPOSITORY_CHANGED);
		TestUtil.joinJobs(JobFamilies.REPOSITORY_WORKING_SETS);
	}

	private static Set<IAdaptable> elements(IWorkingSet workingSet) {
		assertNotNull(workingSet);
		return new HashSet<>(Arrays.asList(workingSet.getElements()));
	}

	private static Set<IAdaptable> projects(String... names) {
		Set<IAdaptable> result = new HashSet<>();
		for (String name : names) {
			result.add(ResourcesPlugin.getWorkspace().getRoot()
					.getProject(name));
		}
		return result;
	}
}
