/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.eclipse.egit.ui.variables;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.common.EGitTestCase;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class DynamicVariablesTest extends EGitTestCase {

	private File gitDir;
	private File gitDir2;
	private IProject project;
	private IProject project2;
	private FileRepository repository;
	private FileRepository repository2;
	private Git git;

	private static final String TEST_PROJECT = "TestProject";
	private static final String TEST_PROJECT_LOC = "Sub/TestProject";
	private static final String TEST_PROJECT2 = "TestProject2";

	private static final String TEST_FILE = "TestFile";
	private static final String TEST_FILE2 = "TestFile2";

	@Before
	public void setUp() throws Exception {

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		new File(root.getLocation().toFile(),"Sub").mkdir();
		gitDir = new File(new File(root.getLocation().toFile(), "Sub"), Constants.DOT_GIT);

		repository = new FileRepository(gitDir);
		repository.create();

		project = root.getProject(TEST_PROJECT);
		project.create(null);
		project.open(null);
		IProjectDescription description = project.getDescription();
		description.setLocation(root.getLocation().append(TEST_PROJECT_LOC));
		project.move(description, IResource.FORCE, null);

		project2 = root.getProject(TEST_PROJECT2);
		project2.create(null);
		project2.open(null);
		gitDir2 = new File(project2.getLocation().toFile().getAbsoluteFile(), Constants.DOT_GIT);
		repository2 = new FileRepository(gitDir2);
		repository2.create();

		RepositoryMapping mapping = new RepositoryMapping(project, gitDir);
		RepositoryMapping mapping2 = new RepositoryMapping(project2, gitDir2);

		GitProjectData projectData = new GitProjectData(project);
		GitProjectData projectData2 = new GitProjectData(project2);
		projectData.setRepositoryMappings(Collections.singletonList(mapping));
		projectData.store();
		projectData2.setRepositoryMappings(Collections.singletonList(mapping2));
		projectData2.store();
		GitProjectData.add(project, projectData);
		GitProjectData.add(project2, projectData2);

		RepositoryProvider.map(project, GitProvider.class.getName());
		RepositoryProvider.map(project2, GitProvider.class.getName());

		FileWriter fileWriter = new FileWriter(new File(repository.getWorkTree(), TEST_PROJECT+"/"+TEST_FILE));
		fileWriter.write("Some data");
		fileWriter.close();
		FileWriter fileWriter2 = new FileWriter(new File(repository2.getWorkTree(), TEST_FILE2));
		fileWriter2.write("Some other data");
		fileWriter2.close();
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		project2.refreshLocal(IResource.DEPTH_INFINITE, null);
		git = new Git(repository);
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Initial commit").call();

		git = new Git(repository2);
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Initial commit").call();
		git.branchRename().setNewName("main").call();
	}

	@After
	public void tearDown() throws Exception {

		Thread.sleep(1000); // FIXME: We need a good way to wait for things to settle

		RepositoryProvider.unmap(project);
		RepositoryProvider.unmap(project2);

		GitProjectData.delete(project);
		GitProjectData.delete(project2);

		project.delete(true, true, null);
		project2.delete(true, true, null);

		repository.close();
		repository2.close();

		org.eclipse.egit.core.Activator.getDefault().getRepositoryCache().clear();

		FileUtils.delete(gitDir, FileUtils.RECURSIVE);
		// gitDir2 is inside project, already gone
	}

	@Test
	public void testGitDir() throws CoreException {
		assertVariable(gitDir.getPath(), "git_dir", null);
		assertVariable(gitDir.getPath(), "git_dir", TEST_PROJECT);
		assertVariable(gitDir2.getPath(), "git_dir", TEST_PROJECT2);
		assertVariable(gitDir2.getPath(), "git_dir", TEST_PROJECT2 + "/" + TEST_FILE2);
	}

	@Test
	public void testGitWorkTree() throws CoreException {
		assertVariable(gitDir.getParentFile().getPath(), "git_work_tree", null);
		assertVariable(gitDir.getParentFile().getPath(), "git_work_tree", TEST_PROJECT);
		assertVariable(gitDir2.getParentFile().getPath(), "git_work_tree", TEST_PROJECT2);
	}

	@Test
	public void testGitPath() throws CoreException {
		assertVariable(TEST_PROJECT + File.separatorChar + TEST_FILE, "git_repo_relative_path", null);
		assertVariable(TEST_FILE2, "git_repo_relative_path", TEST_PROJECT2 + "/" + TEST_FILE2);
	}

	@Test
	public void testGitBranch() throws CoreException {
		assertVariable("master", "git_branch", null);
		assertVariable("master", "git_branch", TEST_PROJECT + "/" + TEST_FILE);
		assertVariable("main", "git_branch", TEST_PROJECT2 + "/" + TEST_FILE2);
		assertVariable("main", "git_branch", TEST_PROJECT2);
	}

	private void assertVariable(String expected, String variableName,
			String argument) throws CoreException {
		IResource findMember = project.findMember(TEST_FILE);

		final ISelectionProvider selectionProvider = bot.viewByTitle("Package Explorer").getViewReference().getView(true).getSite().getSelectionProvider();
		final StructuredSelection structuredSelection = new StructuredSelection(
				findMember);
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			public void run() {
				selectionProvider.setSelection(structuredSelection);
			}
		});

		IDynamicVariable dynamicVariable = VariablesPlugin.getDefault()
				.getStringVariableManager().getDynamicVariable(variableName);
		String value = dynamicVariable.getValue(argument);
		assertEquals(expected, value);
	}
}
