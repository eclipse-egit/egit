/*******************************************************************************
 * Copyright (C) 2009, Tor Arne Vestbø <torarnv@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.egit.core.AdaptableFileTreeIterator;
import org.eclipse.egit.core.ContainerTreeIterator;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.junit.Before;
import org.junit.Test;

public class AdaptableFileTreeIteratorTest extends GitTestCase {

	private Repository repository;

	private File file;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		repository = new FileRepository(gitDir);
		repository.create();

		file = new File(project.getProject().getLocation().toFile(), "a.txt");
		final FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("aaaaaaaaaaa");
		fileWriter.close();

		final ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(), gitDir);
		operation.execute(null);
	}

	@Test
	public void testFileTreeToContainerAdaptation() throws IOException {
		final IWorkspaceRoot root = project.getProject().getWorkspace()
				.getRoot();

		final TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(new AdaptableFileTreeIterator(repository, root));
		treeWalk.setRecursive(true);

		final IFile eclipseFile = project.getProject().getFile(file.getName());
		final RepositoryMapping mapping = RepositoryMapping
				.getMapping(eclipseFile);
		final Set<String> repositoryPaths = Collections.singleton(mapping
				.getRepoRelativePath(eclipseFile));

		assertTrue(repositoryPaths.size() == 1);
		treeWalk.setFilter(PathFilterGroup.createFromStrings(repositoryPaths, treeWalk.getPathEncoding()));

		assertTrue(treeWalk.next());

		final WorkingTreeIterator iterator = treeWalk.getTree(0,
				WorkingTreeIterator.class);
		assertTrue(iterator instanceof ContainerTreeIterator);
	}
}
