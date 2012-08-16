/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.op.DeleteResourcesOperation;
import org.eclipse.egit.core.test.DualRepositoryTestCase;
import org.eclipse.egit.core.test.JobSchedulingAssert;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DeleteResourcesOperationTest extends DualRepositoryTestCase {

	File workdir;

	IProject project;

	String projectName = "DeleteResourcesOperationTest";

	@Before
	public void setUp() throws Exception {

		workdir = testUtils.createTempDir("Repository1");

		repository1 = new TestRepository(new File(workdir, Constants.DOT_GIT));
		project = testUtils.createProjectInLocalFileSystem(workdir, projectName);
		repository1.connect(project);
	}

	@After
	public void tearDown() throws Exception {
		Job.getJobManager().join(JobFamilies.INDEX_DIFF_CACHE_UPDATE, null);
		project.close(null);
		project.delete(false, false, null);
		repository1.dispose();
		repository1 = null;
		testUtils.deleteTempDirs();
	}

	@Test
	public void testDeleteResourceOfProject() throws Exception {
		IResource resource = testUtils.addFileToProject(project, "file.txt", "Hello world 1");

		deleteResources(Arrays.asList(resource));

		File file = resource.getFullPath().toFile();
		assertFalse("File should have been deleted", file.exists());
	}

	@Test
	public void testDeleteResourceOutsideOfProject() throws Exception {
		File outsideOfProject = new File(workdir, "outside-of-project.txt");
		outsideOfProject.createNewFile();

		Path path = new Path(outsideOfProject.getAbsolutePath());
		IWorkspaceRoot root = project.getWorkspace().getRoot();
		IResource resource = root.getFile(path);

		// Make sure the cache has at least be refreshed once, otherwise the
		// assertion at the end is not effective
		initIndexDiffCache(repository1.getRepository());

		JobSchedulingAssert jobSchedulingAssertion = JobSchedulingAssert
				.forFamily(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		deleteResources(Arrays.asList(resource));

		assertFalse("File should have been deleted", outsideOfProject.exists());
		jobSchedulingAssertion
				.assertScheduled("Delete of file outside of workspace should have cause an index diff cache job.");
	}

	private static void initIndexDiffCache(Repository repository)
			throws Exception {
		IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
		IndexDiffCacheEntry cacheEntry = cache.getIndexDiffCacheEntry(repository);
		assertNotNull(cacheEntry);
		Job.getJobManager().join(JobFamilies.INDEX_DIFF_CACHE_UPDATE, null);
	}

	private void deleteResources(Collection<IResource> resources) throws CoreException {
		DeleteResourcesOperation operation = new DeleteResourcesOperation(resources);
		operation.execute(null);
	}
}
