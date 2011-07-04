/*******************************************************************************
 * Copyright (C) 2011, Tasktop Technologies Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Benjamin Muskalla (benjamin.muskalla@tasktop.com) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.common.EGitTestCase;
import org.eclipse.egit.ui.internal.operations.GitScopeOperation;
import org.eclipse.egit.ui.internal.operations.GitScopeOperationFactory;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.ui.IWorkbenchPart;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class GitScopeUtilTest extends EGitTestCase {

	private static final String PROJ1 = "modelproject";

	public static final String MODEL_FILE = "base.model";

	public static final String MODEL_EXTENSIONS_FILE = "base.model.extension";

	private IWorkbenchPart part;

	@Before
	public void setup() {
		part = bot.activeView().getViewReference().getPart(false);

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
		// close all editors/dialogs
		new Eclipse().reset();
		// cleanup
		IProject modelProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		modelProject.delete(false, false, null);
	}

	@Test
	// model provider already available via fragment.xml
	public void modelProviderWithExtensionFiles() throws Exception {
		IFile modelFile = createProjectAndModelFiles();

		IResource[] selectedResources = new IResource[] { modelFile };
		IResource[] relatedChanges = getRelatedChangesInUIThread(selectedResources);
		assertEquals(2, relatedChanges.length);
		assertEquals(MODEL_FILE, relatedChanges[1].getName());
		assertEquals(MODEL_EXTENSIONS_FILE, relatedChanges[0].getName());
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

	private IResource[] getRelatedChangesInUIThread(
			final IResource[] selectedResources) {
		final IResource[][] relatedChanges = new IResource[1][];
		UIThreadRunnable.syncExec(new VoidResult() {
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

	private IFile createProjectAndModelFiles() throws CoreException,
			UnsupportedEncodingException {
		IProject modelProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
	
		if (modelProject.exists())
			modelProject.delete(true, null);
		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription(PROJ1);
		// desc.setLocation(new Path(new File(myRepository.getWorkTree(), PROJ1)
		// .getPath()));
		modelProject.create(desc, null);
		modelProject.open(null);
	
		// IFolder folder = firstProject.getFolder(FOLDER);
		// folder.create(false, true, null);
		IFile modelFile = modelProject.getFile(MODEL_FILE);
		modelFile.create(
				new ByteArrayInputStream("This is the base model"
						.getBytes(modelProject.getDefaultCharset())), false,
				null);
		IFile modelExtensionFile = modelProject.getFile(MODEL_EXTENSIONS_FILE);
		modelExtensionFile.create(
				new ByteArrayInputStream("Some more content"
						.getBytes(modelProject.getDefaultCharset())), false,
				null);
		return modelFile;
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
			return (ResourceMapping) resource.getAdapter(ResourceMapping.class);
		}
	}
}
