/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2013, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.eclipse.team.core.RepositoryProvider;
import org.junit.Assert;
import org.junit.Test;

public class ConnectProviderOperationTest extends GitTestCase {

	@Test
	public void testNoRepository() throws Exception {

		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(), new File("../..", Constants.DOT_GIT));
		try {
			operation.execute(null);
			Assert.fail("Connect without repository should fail!");
		} catch (CoreException e) {
			// expected
		}

		assertFalse(RepositoryProvider.isShared(project.getProject()));
		assertFalse(gitDir.exists());
	}

	@Test
	public void testNewRepository() throws CoreException, IOException {

		Repository repository = FileRepositoryBuilder.create(gitDir);
		repository.create();
		repository.close();
		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(), gitDir);
		operation.execute(null);

		assertTrue(RepositoryProvider.isShared(project.getProject()));

		assertTrue(gitDir.exists());
	}

	@Test
	public void testNewRepositoryCaseSensitive()
			throws CoreException, IOException {
		if (FS.detect().isCaseSensitive()) {
			return;
		}
		Repository repository = FileRepositoryBuilder.create(gitDir);
		repository.create();
		repository.close();

		IPath path = new Path(gitDir.toString());
		String device = path.getDevice();
		if (device == null) {
			// not windows???
			return;
		}
		if (!device.toLowerCase(Locale.ROOT).equals(device)) {
			path = path.setDevice(device.toLowerCase(Locale.ROOT));
		} else {
			path = path.setDevice(device.toUpperCase(Locale.ROOT));
		}
		assertNotEquals(path, new Path(gitDir.toString()));
		assertNotEquals(path.toFile().toString(),
				new Path(gitDir.toString()).toFile().toString());
		assertEquals(path.toFile(), gitDir);

		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(),
				path.toFile());
		operation.execute(null);

		assertTrue(RepositoryProvider.isShared(project.getProject()));

		assertTrue(gitDir.exists());
	}

	@Test
	public void testAutoIgnoresDerivedFolder() throws Exception {
		// enable auto-ignore
		IEclipsePreferences p = InstanceScope.INSTANCE.getNode(Activator
				.getPluginId());
		boolean autoignore = p.getBoolean(
				GitCorePreferences.core_autoIgnoreDerivedResources, false);
		if (!autoignore) {
			p.putBoolean(GitCorePreferences.core_autoIgnoreDerivedResources,
					true);
		}
		try {
			Repository repository = FileRepositoryBuilder.create(gitDir);
			repository.create();
			repository.close();
			project.setBinFolderDerived();
			project.createSourceFolder();

			// not connected: no ignore
			IFolder binFolder = project.getProject().getFolder("bin");
			IPath binPath = binFolder.getLocation();
			assertTrue(binFolder.exists());
			assertFalse(RepositoryUtil.canBeAutoIgnored(binPath));

			IFolder srcFolder = project.getProject().getFolder("src");
			IPath srcPath = srcFolder.getLocation();
			assertTrue(srcFolder.exists());
			assertFalse(RepositoryUtil.canBeAutoIgnored(srcPath));

			IFolder notThere = project.getProject().getFolder("notThere");
			IPath notTherePath = notThere.getLocation();
			assertFalse(notThere.exists());
			assertFalse(RepositoryUtil.canBeAutoIgnored(notTherePath));

			// connect to git
			ConnectProviderOperation operation = new ConnectProviderOperation(
					project.getProject(), gitDir);
			operation.execute(null);
			assertTrue(RepositoryProvider.isShared(project.getProject()));
			Job.getJobManager().join(JobFamilies.AUTO_IGNORE, null);

			// connected, and already automatically ignored
			assertFalse(RepositoryUtil.canBeAutoIgnored(binPath));
			// connected, and *can* be automatically ignored
			assertTrue(RepositoryUtil.canBeAutoIgnored(srcPath));
			// connected but not existing: we should not autoignore
			assertFalse(RepositoryUtil.canBeAutoIgnored(notTherePath));

			assertTrue(gitDir.exists());
		} finally {
			if (!autoignore) {
				p.putBoolean(GitCorePreferences.core_autoIgnoreDerivedResources,
						false);
			}
		}
	}

	@Test
	public void testNoAutoIgnoresDerivedFolder() throws Exception {
		// disable auto-ignore
		IEclipsePreferences p = InstanceScope.INSTANCE
				.getNode(Activator.getPluginId());
		boolean autoignore = p.getBoolean(
				GitCorePreferences.core_autoIgnoreDerivedResources, false);
		if (autoignore) {
			p.putBoolean(GitCorePreferences.core_autoIgnoreDerivedResources,
					false);
		}
		try {
			Repository repository = FileRepositoryBuilder.create(gitDir);
			repository.create();
			repository.close();
			project.setBinFolderDerived();
			project.createSourceFolder();

			// not connected: no ignore
			IFolder binFolder = project.getProject().getFolder("bin");
			IPath binPath = binFolder.getLocation();
			assertTrue(binFolder.exists());
			assertFalse(RepositoryUtil.canBeAutoIgnored(binPath));

			IFolder srcFolder = project.getProject().getFolder("src");
			IPath srcPath = srcFolder.getLocation();
			assertTrue(srcFolder.exists());
			assertFalse(RepositoryUtil.canBeAutoIgnored(srcPath));

			IFolder notThere = project.getProject().getFolder("notThere");
			IPath notTherePath = notThere.getLocation();
			assertFalse(notThere.exists());
			assertFalse(RepositoryUtil.canBeAutoIgnored(notTherePath));

			// connect to git
			ConnectProviderOperation operation = new ConnectProviderOperation(
					project.getProject(), gitDir);
			operation.execute(null);
			assertTrue(RepositoryProvider.isShared(project.getProject()));
			Job.getJobManager().join(JobFamilies.AUTO_IGNORE, null);

			// connected, and *can* be automatically ignored
			assertTrue(RepositoryUtil.canBeAutoIgnored(binPath));
			// connected, and *can* be automatically ignored
			assertTrue(RepositoryUtil.canBeAutoIgnored(srcPath));
			// connected but not existing: we should not autoignore
			assertFalse(RepositoryUtil.canBeAutoIgnored(notTherePath));

			assertTrue(gitDir.exists());
		} finally {
			if (autoignore) {
				p.putBoolean(GitCorePreferences.core_autoIgnoreDerivedResources,
						true);
			}
		}
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
