/*******************************************************************************
 * Copyright (C) 2011, 2013 Tasktop Technologies Inc. and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Benjamin Muskalla (benjamin.muskalla@tasktop.com) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.operations.GitScopeOperation;
import org.eclipse.egit.ui.internal.operations.GitScopeOperationFactory;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.ui.IWorkbenchPart;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class GitScopeUtilTest extends LocalRepositoryTestCase {

	public static final String MODEL_FILE = "base.model";

	public static final String MODEL_EXTENSIONS_FILE = "base.model.extension";

	private IWorkbenchPart part;

	private File repositoryFile;

	@Before
	public void setup() throws Exception {
		SWTBotView view = TestUtil.showExplorerView();
		part = view.getViewReference().getPart(false);

		repositoryFile = createProjectAndCommitToRepository();

		GitScopeOperationFactory.setFactory(new GitScopeOperationFactory() {
			@Override
			public GitScopeOperation createGitScopeOperation(
					IWorkbenchPart workbenchPart, SubscriberScopeManager manager) {
				return new GitScopeOperation(workbenchPart, manager) {
					@Override
					protected boolean promptForInputChange(
							String requestPreviewMessage,
							IProgressMonitor monitor) {
						// we will avoid the confirmation prompt in the tests
						return false;
					}
				};
			}
		});
	}

	@AfterClass
	public static void afterClassBase() throws Exception {
		GitScopeOperationFactory.setFactory(new GitScopeOperationFactory());
	}

	@Test
	// model provider already available via fragment.xml
	public void modelProviderWithExtensionFiles() throws Exception {
		IFile modelFile = createModelFiles();

		IResource[] selectedResources = new IResource[] { modelFile };
		IResource[] relatedChanges = getRelatedChangesInUIThread(selectedResources);
		assertEquals(2, relatedChanges.length);

		assertContainsResourceByName(relatedChanges, MODEL_FILE);
		assertContainsResourceByName(relatedChanges, MODEL_EXTENSIONS_FILE);
	}

	@Test
	public void relatedChangesWithNullResources() throws Exception {
		IResource[] resources = GitScopeUtil.getRelatedChanges(part, null);
		assertNotNull(resources);
		assertEquals(0, resources.length);
	}

	@Test
	public void relatedChangesWithEmptyResources() throws Exception {
		IResource[] res = new IResource[0];
		IResource[] resources = getRelatedChangesInUIThread(res);
		assertNotNull(resources);
		assertEquals(0, resources.length);
	}

	@Test(expected = IllegalArgumentException.class)
	public void relatedChangesWithInvalidParams() throws Exception {
		IResource[] res = new IResource[0];
		GitScopeUtil.getRelatedChanges(null, res);
	}

	@Test
	public void relatedChangesWithPrompt() throws Exception {
		GitScopeOperationFactory.setFactory(new GitScopeOperationFactory());

		final IFile modelFile = createModelFiles();

		Repository repository = lookupRepository(repositoryFile);
		Activator.getDefault().getIndexDiffCache()
				.getIndexDiffCacheEntry(repository);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		final IResource[] selectedResources = new IResource[] { modelFile };
		UIThreadRunnable.asyncExec(new VoidResult() {
			@Override
			public void run() {
				try {
					GitScopeUtil.getRelatedChanges(part, selectedResources);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Prompt because the files are untracked
		SWTBotShell dialog = bot.shell("Selection Adjustment Required");
		dialog.bot().button(IDialogConstants.OK_LABEL).click();

		IFile modelExtensionsFile = modelFile.getProject().getFile(
				MODEL_EXTENSIONS_FILE);
		addAndCommit(modelExtensionsFile, "add model extensions file");
		addAndCommit(modelFile, "add model file");
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		// Both files are committed, should no longer prompt now
		IResource[] relatedChanges = getRelatedChangesInUIThread(selectedResources);
		assertEquals(2, relatedChanges.length);

		assertContainsResourceByName(relatedChanges, MODEL_FILE);
		assertContainsResourceByName(relatedChanges, MODEL_EXTENSIONS_FILE);
	}

	private IResource[] getRelatedChangesInUIThread(
			final IResource[] selectedResources) {
		final IResource[][] relatedChanges = new IResource[1][];
		UIThreadRunnable.syncExec(new VoidResult() {
			@Override
			public void run() {
				try {
					relatedChanges[0] = GitScopeUtil.getRelatedChanges(part,
							selectedResources);
				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
		});
		return relatedChanges[0];
	}

	private IFile createModelFiles() throws CoreException,
			UnsupportedEncodingException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);

		IFile modelFile = project.getFile(MODEL_FILE);
		modelFile.create(
				new ByteArrayInputStream("This is the base model"
						.getBytes(project.getDefaultCharset())), false,
				null);
		IFile modelExtensionFile = project.getFile(MODEL_EXTENSIONS_FILE);
		modelExtensionFile.create(
				new ByteArrayInputStream("Some more content"
						.getBytes(project.getDefaultCharset())), false,
				null);

		return modelFile;
	}

	private void assertContainsResourceByName(IResource[] relatedChanges,
			String fileName) {
		for (IResource resource : relatedChanges)
			if (resource.getName().equals(fileName))
				return;
		fail("Resource " + fileName + " not found.");
	}

	/**
	 * Model provider that requires for all given {@link IResource}s an
	 * additional {@link IResource}s with an appended extension ".extension"
	 *
	 */
	public static class MockModelProvider extends ModelProvider {

		@Override
		public ResourceMapping[] getMappings(IResource resource,
				ResourceMappingContext context, IProgressMonitor monitor)
				throws CoreException {
			ResourceMapping[] mappings = new ResourceMapping[2];
			mappings[0] = getMappingAdapter(resource);

			IPath resourcePath = resource.getFullPath();
			IPath extensionFilePath = resourcePath
					.addFileExtension("extension");
			IWorkspaceRoot workspace = resource.getWorkspace().getRoot();
			IFile extensionFile = workspace.getFile(extensionFilePath);
			mappings[1] = getMappingAdapter(extensionFile);
			return mappings;
		}

		private ResourceMapping getMappingAdapter(IResource resource) {
			return Utils.getAdapter(resource, ResourceMapping.class);
		}
	}
}
