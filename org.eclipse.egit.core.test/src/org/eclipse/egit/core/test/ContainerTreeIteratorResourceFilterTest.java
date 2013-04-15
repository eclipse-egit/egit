/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.eclipse.core.resources.IResourceFilterDescription.EXCLUDE_ALL;
import static org.eclipse.core.resources.IResourceFilterDescription.FILES;
import static org.eclipse.core.resources.IResourceFilterDescription.FOLDERS;
import static org.eclipse.core.resources.IResourceFilterDescription.INHERITABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.AdaptableFileTreeIterator;
import org.eclipse.egit.core.ContainerTreeIterator;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for how {@link ContainerTreeIterator} handles filtered resources.
 * <p>
 * The tricky thing is that they are not returned from API like
 * {@link IContainer#members()}. So we have to fall back to using an
 * {@link AdaptableFileTreeIterator} if there may be resource filters active.
 * <p>
 * In case of nested projects where the subproject is filtered in the parent
 * project with resource filters, we want the nested project to be walked with
 * {@link ContainerTreeIterator} again. That is, it should "recover" from
 * falling back to {@link AdaptableFileTreeIterator}.
 */
public class ContainerTreeIteratorResourceFilterTest extends GitTestCase {

	private Repository repository;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		repository = FileRepositoryBuilder.create(gitDir);
		repository.create();

		connectProject(project.getProject());
	}

	@Test
	public void simpleNonInheritableFilter() throws Exception {
		IProject p = project.getProject();
		IFile filtered = testUtils.addFileToProject(p, "filtered.txt", "");
		IFile unfiltered = testUtils.addFileToProject(p, "unfiltered.txt", "");
		assertTrue("IFile should exist before filtering.", filtered.exists());
		assertTrue("IFile should exist before filtering.", unfiltered.exists());

		createFilter(p, EXCLUDE_ALL | FILES, "filtered.txt");
		assertFalse("IFile should no longer exist after filtering.", filtered.exists());
		assertTrue("IFile should exist after filtering.", unfiltered.exists());

		List<Entry> entries = walkTree();

		assertThat(entries, hasItem(containerTreeEntry("Project-1/filtered.txt")));
		assertThat(entries, hasItem(containerTreeEntry("Project-1/unfiltered.txt")));
	}

	@Test
	public void simpleNonInheritableFolderFilter() throws Exception {
		IProject p = project.getProject();
		IFile filtered = testUtils.addFileToProject(p, "folder/filtered.txt", "");
		IFile unfiltered = testUtils.addFileToProject(p, "folder2/unfiltered.txt", "");

		createFilter(p, EXCLUDE_ALL | FOLDERS, "folder");
		assertFalse("IFile should no longer exist after filtering.", filtered.exists());
		assertTrue("IFile should exist after filtering.", unfiltered.exists());

		List<Entry> entries = walkTree();

		assertThat(entries, hasItem(adaptableFileTreeEntry("Project-1/folder/filtered.txt")));
		assertThat(entries, hasItem(containerTreeEntry("Project-1/folder2/unfiltered.txt")));
	}

	@Test
	public void inheritableFilter() throws Exception {
		IProject p = project.getProject();
		IFile filtered1 = testUtils.addFileToProject(p, "folder1/filtered.txt", "");
		IFile filtered2 = testUtils.addFileToProject(p, "folder1/folder2/filtered.txt", "");
		IFile unfiltered = testUtils.addFileToProject(p, "folder1/folder2/unfiltered.txt", "");

		createFilter(p, EXCLUDE_ALL | FILES | INHERITABLE, "filtered.txt");
		assertFalse("IFile should no longer exist after filtering.", filtered1.exists());
		assertFalse("IFile should no longer exist after filtering.", filtered2.exists());
		assertTrue("IFile should exist after filtering.", unfiltered.exists());

		List<Entry> entries = walkTree();

		assertThat(entries, hasItem(containerTreeEntry("Project-1/folder1/filtered.txt")));
		assertThat(entries, hasItem(containerTreeEntry("Project-1/folder1/folder2/filtered.txt")));
		assertThat(entries, hasItem(containerTreeEntry("Project-1/folder1/folder2/unfiltered.txt")));
	}

	@Test
	public void directlyNestedProject() throws Exception {
		IProject p = project.getProject();
		testUtils.addFileToProject(p, "file.txt", "");

		TestProject testProject2 = new TestProject(true, "Project-1/Project-2");
		testUtils.addFileToProject(testProject2.getProject(), "project2.txt", "");

		createFilter(p, EXCLUDE_ALL | FOLDERS, "Project-2");

		List<Entry> entries = walkTree();

		assertThat(entries, hasItem(containerTreeEntry("Project-1/file.txt")));
		// Should be handled by container tree iterator because it exists, even
		// when it's not returned by members() of Project-1.
		assertThat(entries, hasItem(containerTreeEntry("Project-1/Project-2/project2.txt")));
	}

	@Test
	public void nestedProject() throws Exception {
		IProject p = project.getProject();
		testUtils.addFileToProject(p, "folder1/file.txt", "");
		testUtils.addFileToProject(p, "folder1/subfolder/filtered.txt", "");

		TestProject testProject2 = new TestProject(true, "Project-1/folder1/subfolder/Project-2");
		connectProject(testProject2.getProject());
		IFile project2File = testUtils.addFileToProject(testProject2.getProject(), "project2.txt", "");
		assertThat(project2File.getProject(), is(testProject2.getProject()));

		createFilter(p.getFolder("folder1"), EXCLUDE_ALL | FOLDERS, "subfolder");
		assertFalse("IFolder should be filtered",
				p.getFolder(new Path("folder1/subfolder")).exists());

		List<Entry> entries = walkTree();

		assertThat(entries, hasItem(containerTreeEntry("Project-1/folder1/file.txt")));
		assertThat(entries, hasItem(adaptableFileTreeEntry("Project-1/folder1/subfolder/filtered.txt")));
		// Should be handled by container tree iterator again, because the project exists.
		assertThat(entries, hasItem(containerTreeEntry("Project-1/folder1/subfolder/Project-2/project2.txt")));

		testProject2.dispose();
	}

	private static void createFilter(IContainer container, int type, String regexFilterArguments) throws CoreException {
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(
				"org.eclipse.core.resources.regexFilterMatcher",
				regexFilterArguments);
		container.createFilter(type, matcherDescription, 0, null);
	}

	private void connectProject(IProject p) throws CoreException {
		final ConnectProviderOperation operation = new ConnectProviderOperation(
				p, gitDir);
		operation.execute(null);
	}

	private List<Entry> walkTree() throws IOException {
		TreeWalk treeWalk = new TreeWalk(repository);
		ContainerTreeIterator tree = new ContainerTreeIterator(repository, project.getProject());
		int treeIndex = treeWalk.addTree(tree);
		treeWalk.setRecursive(true);
		List<Entry> entries = new ArrayList<Entry>();
		while (treeWalk.next()) {
			AbstractTreeIterator it = treeWalk.getTree(treeIndex, AbstractTreeIterator.class);
			Entry entry = new Entry(treeWalk.getPathString(), it.getClass());
			entries.add(entry);
		}
		return entries;
	}

	private static Entry containerTreeEntry(String path) {
		return new Entry(path, ContainerTreeIterator.class);
	}

	private static Entry adaptableFileTreeEntry(String path) {
		return new Entry(path, AdaptableFileTreeIterator.class);
	}

	// Value object (case class).
	private static class Entry {
		private final String path;
		private Class<? extends AbstractTreeIterator> iteratorClass;

		public Entry(String path, Class<? extends AbstractTreeIterator> iteratorClass) {
			this.path = path;
			this.iteratorClass = iteratorClass;
		}

		@Override
		public String toString() {
			return path + " (" + iteratorClass.getSimpleName() + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((iteratorClass == null) ? 0 : iteratorClass.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Entry other = (Entry) obj;
			if (iteratorClass == null) {
				if (other.iteratorClass != null)
					return false;
			} else if (!iteratorClass.equals(other.iteratorClass))
				return false;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			return true;
		}
	}
}
