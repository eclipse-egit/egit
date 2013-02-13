/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2013, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.team.core.RepositoryProvider;
import org.junit.Test;

public class ConnectProviderOperationTest extends GitTestCase {

	@Test
	public void testNoRepository() throws CoreException {

		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(), new File("../..", Constants.DOT_GIT));
		operation.execute(null);

		assertFalse(RepositoryProvider.isShared(project.getProject()));
		assertFalse(gitDir.exists());
	}

	@Test
	public void testNewRepository() throws CoreException, IOException {

		Repository repository = new FileRepository(gitDir);
		repository.create();
		repository.close();
		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(), gitDir);
		operation.execute(null);

		assertTrue(RepositoryProvider.isShared(project.getProject()));

		assertTrue(gitDir.exists());
	}

	@Test
	public void testAutoIgnoresDerivedFolder() throws Exception {
		// enable auto-ignore
		IEclipsePreferences p = InstanceScope.INSTANCE.getNode(Activator
				.getPluginId());
		p.putBoolean(GitCorePreferences.core_autoIgnoreDerivedResources, true);
		Repository repository = new FileRepository(gitDir);
		repository.create();
		repository.close();
		project.setBinFolderDerived();
		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(), gitDir);
		operation.execute(null);

		assertTrue(RepositoryProvider.isShared(project.getProject()));
		Job.getJobManager().join(JobFamilies.AUTO_IGNORE, null);

		IPath binPath = project.getProject().getLocation().append("bin");
		assertTrue(RepositoryUtil.isIgnored(binPath));
		assertTrue(gitDir.exists());
		p.putBoolean(GitCorePreferences.core_autoIgnoreDerivedResources, false);
	}

	@Test
	public void testNewUnsharedFile() throws CoreException, Exception {

		project.createSourceFolder();
		IFile fileA = project.getProject().getFolder("src").getFile("A.java");
		String srcA = "class A {\n" + "}\n";
		fileA.create(new ByteArrayInputStream(srcA.getBytes("UTF-8")), false,
				null);

		TestRepository thisGit = new TestRepository(gitDir);

		File committable = new File(fileA.getLocationURI());

		thisGit.addAndCommit(project.project, committable,
				"testNewUnsharedFile\n\nJunit tests\n");

		assertNull(RepositoryProvider.getProvider(project.getProject()));

		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(), gitDir);
		operation.execute(null);

		assertNotNull(RepositoryProvider.getProvider(project.getProject()));
	}
}
