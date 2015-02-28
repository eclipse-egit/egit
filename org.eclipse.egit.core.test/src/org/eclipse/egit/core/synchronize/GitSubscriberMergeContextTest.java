/*******************************************************************************
 * Copyright (C) 2011, 2012 Benjamin Muskalla <benjamin.muskalla@tasktop.com>
 * and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.junit.Assert.assertTrue;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.IMergeStatus;
import org.eclipse.team.core.mapping.IResourceMappingMerger;
import org.eclipse.team.core.mapping.ResourceMappingMerger;
import org.eclipse.team.core.mapping.provider.MergeStatus;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitSubscriberMergeContextTest extends GitTestCase {

	private static final String MASTER = Constants.R_HEADS + Constants.MASTER;

	private static final String BRANCH = Constants.R_HEADS + "branch";

	private static final String SAMPLE_FILE_EXTENSION = "sample";

	private static final String SAMPLE_PROVIDER_ID = "egit.test.sample.provider";

	private Repository repo;

	private IProject iProject;

	private TestRepository testRepo;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();

		// make initial commit
		new Git(repo).commit().setAuthor("JUnit", "junit@jgit.org")
				.setMessage("Initial commit").call();

		IContentType textType = Platform.getContentTypeManager()
				.getContentType("org.eclipse.core.runtime.text");
		textType.addFileSpec(SAMPLE_FILE_EXTENSION,
				IContentType.FILE_EXTENSION_SPEC);
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;
		super.tearDown();
	}

	@Test
	public void markAsMerged() throws Exception {
		String fileName = "src/Main.java";
		File file = testRepo.createFile(iProject, fileName);
		testRepo.appendContentAndCommit(iProject, file, "class Main {}",
				"some file");
		testRepo.addToIndex(iProject.getFile(".classpath"));
		testRepo.addToIndex(iProject.getFile(".project"));
		testRepo.commit("project files");

		IFile workspaceFile = testRepo.getIFile(iProject, file);

		testRepo.appendFileContent(file, "some changes");
		Status status = new Git(repo).status().call();
		assertEquals(0, status.getAdded().size());
		assertEquals(1, status.getModified().size());
		String repoRelativePath = testRepo.getRepoRelativePath(workspaceFile
				.getLocation().toPortableString());
		assertTrue(status.getModified().contains(repoRelativePath));

		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());

		IMergeContext mergeContext = prepareContext(workspaceFile, HEAD, HEAD);
		IDiff node = mergeContext.getDiffTree().getDiff(workspaceFile);
		assertNotNull(node);
		// First of all, "markAsMerged" is not supposed to have any effect on a
		// folder.
		// Second, it should only be used on IDiff obtained from the context,
		// not created for the occasion.
		mergeContext.markAsMerged(node, true, null);

		status = new Git(repo).status().call();
		assertEquals(1, status.getChanged().size());
		assertEquals(0, status.getModified().size());
		assertTrue(status.getChanged().contains(repoRelativePath));
	}

	@Test
	public void mergeNoConflict() throws Exception {
		String fileName = "src/Main.java";
		File file = testRepo.createFile(iProject, fileName);
		final String initialContent = "class Main {}\n";
		testRepo.appendContentAndCommit(iProject, file, initialContent,
				"some file");
		testRepo.addToIndex(iProject.getFile(".classpath"));
		testRepo.addToIndex(iProject.getFile(".project"));
		testRepo.commit("project files");

		IFile workspaceFile = testRepo.getIFile(iProject, file);
		String repoRelativePath = testRepo.getRepoRelativePath(workspaceFile
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(workspaceFile, branchChanges + initialContent,
				"branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(workspaceFile, initialContent
				+ masterChanges, "master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		IMergeContext mergeContext = prepareContext(workspaceFile, MASTER,
				BRANCH);
		IDiff node = mergeContext.getDiffTree().getDiff(workspaceFile);
		assertNotNull(node);

		IStatus mergeStatus = mergeContext.merge(node, false,
				new NullProgressMonitor());
		assertEquals(IStatus.OK, mergeStatus.getSeverity());
		assertContentEquals(workspaceFile, branchChanges + initialContent
				+ masterChanges);

		Status status = new Git(repo).status().call();
		assertEquals(1, status.getChanged().size());
		assertEquals(0, status.getModified().size());
		assertTrue(status.getChanged().contains(repoRelativePath));
	}

	@Test
	public void mergeModelNoConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		String initialContent1 = "some content for the first file";
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(iFile1, branchChanges + initialContent1,
				"branch commit");
		setContentsAndCommit(iFile2, branchChanges + initialContent2,
				"branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(iFile1, initialContent1 + masterChanges,
				"master commit");
		setContentsAndCommit(iFile2, initialContent2 + masterChanges,
				"master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		IMergeContext mergeContext = prepareModelContext(iFile1, MASTER, BRANCH);
		IDiff node = mergeContext.getDiffTree().getDiff(iFile1);
		assertNotNull(node);
		node = mergeContext.getDiffTree().getDiff(iFile2);
		assertNotNull(node);

		IResourceMappingMerger merger = new SampleModelMerger(
				SAMPLE_PROVIDER_ID);
		IStatus mergeStatus = merger.merge(mergeContext,
				new NullProgressMonitor());
		assertEquals(IStatus.OK, mergeStatus.getSeverity());
		assertContentEquals(iFile1, branchChanges + initialContent1
				+ masterChanges);
		assertContentEquals(iFile2, branchChanges + initialContent2
				+ masterChanges);

		Status status = new Git(repo).status().call();
		assertEquals(2, status.getChanged().size());
		assertEquals(0, status.getModified().size());
		assertTrue(status.getChanged().contains(repoRelativePath1));
		assertTrue(status.getChanged().contains(repoRelativePath2));
	}

	@Test
	public void mergeWithConflict() throws Exception {
		String fileName = "src/Main.java";
		File file = testRepo.createFile(iProject, fileName);
		final String initialContent = "class Main {}\n";
		testRepo.appendContentAndCommit(iProject, file, initialContent,
				"some file");
		testRepo.addToIndex(iProject.getFile(".classpath"));
		testRepo.addToIndex(iProject.getFile(".project"));
		testRepo.commit("project files");

		IFile workspaceFile = testRepo.getIFile(iProject, file);
		String repoRelativePath = testRepo.getRepoRelativePath(workspaceFile
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(workspaceFile, initialContent + branchChanges,
				"branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(workspaceFile, initialContent + masterChanges,
				"master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		IMergeContext mergeContext = prepareContext(workspaceFile, MASTER,
				BRANCH);
		IDiff node = mergeContext.getDiffTree().getDiff(workspaceFile);
		assertNotNull(node);

		IStatus mergeStatus = mergeContext.merge(node, false,
				new NullProgressMonitor());
		assertEquals(IStatus.ERROR, mergeStatus.getSeverity());
		assertContentEquals(workspaceFile, initialContent + masterChanges);

		Status status = new Git(repo).status().call();
		assertEquals(0, status.getChanged().size());
		assertEquals(0, status.getModified().size());
		assertFalse(status.getChanged().contains(repoRelativePath));
	}

	@Test
	public void mergeModelWithConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		String initialContent1 = "some content for the first file";
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(iFile1, initialContent1 + branchChanges,
				"branch commit");
		setContentsAndCommit(iFile2, initialContent2 + branchChanges,
				"branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(iFile1, initialContent1 + masterChanges,
				"master commit");
		setContentsAndCommit(iFile2, initialContent2 + masterChanges,
				"master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		IMergeContext mergeContext = prepareModelContext(iFile1, MASTER, BRANCH);
		IDiff node = mergeContext.getDiffTree().getDiff(iFile1);
		assertNotNull(node);
		node = mergeContext.getDiffTree().getDiff(iFile2);
		assertNotNull(node);

		IResourceMappingMerger merger = new SampleModelMerger(
				SAMPLE_PROVIDER_ID);
		IStatus mergeStatus = merger.merge(mergeContext,
				new NullProgressMonitor());
		assertEquals(IStatus.ERROR, mergeStatus.getSeverity());

		assertTrue(mergeStatus instanceof IMergeStatus);
		assertEquals(2,
				((IMergeStatus) mergeStatus).getConflictingMappings().length);
		Set<IFile> conflictingFiles = new LinkedHashSet<IFile>();
		for (ResourceMapping conflictingMapping : ((IMergeStatus) mergeStatus)
				.getConflictingMappings()) {
			assertTrue(conflictingMapping instanceof SampleResourceMapping
					&& conflictingMapping.getModelObject() instanceof IFile);
			conflictingFiles.add((IFile) conflictingMapping.getModelObject());
		}
		assertTrue(conflictingFiles.contains(iFile1));
		assertTrue(conflictingFiles.contains(iFile2));

		assertContentEquals(iFile1, initialContent1 + masterChanges);
		assertContentEquals(iFile2, initialContent2 + masterChanges);

		Status status = new Git(repo).status().call();
		assertEquals(0, status.getChanged().size());
		assertEquals(0, status.getModified().size());
	}

	@Test
	public void mergeModelWithDeletedFileNoConflict() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		String initialContent1 = "some content for the first file";
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);
		String repoRelativePath1 = testRepo.getRepoRelativePath(iFile1
				.getLocation().toPortableString());
		String repoRelativePath2 = testRepo.getRepoRelativePath(iFile2
				.getLocation().toPortableString());

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		setContentsAndCommit(iFile1, branchChanges + initialContent1,
				"branch commit");
		iFile2.delete(true, new NullProgressMonitor());
		testRepo.addAndCommit(iProject, file2, "branch commit - deleted file2."
				+ SAMPLE_FILE_EXTENSION);

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		setContentsAndCommit(iFile1, initialContent1 + masterChanges,
				"master commit");
		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		IMergeContext mergeContext = prepareModelContext(iFile1, MASTER, BRANCH);
		IDiff node = mergeContext.getDiffTree().getDiff(iFile1);
		assertNotNull(node);
		node = mergeContext.getDiffTree().getDiff(iFile2);
		assertNotNull(node);

		IResourceMappingMerger merger = new SampleModelMerger(
				SAMPLE_PROVIDER_ID);
		IStatus mergeStatus = merger.merge(mergeContext,
				new NullProgressMonitor());
		assertEquals(IStatus.OK, mergeStatus.getSeverity());
		assertContentEquals(iFile1, branchChanges + initialContent1
				+ masterChanges);
		assertFalse(iFile2.exists());

		Status status = new Git(repo).status().call();
		assertEquals(1, status.getChanged().size());
		assertEquals(1, status.getRemoved().size());
		assertEquals(0, status.getModified().size());
		assertTrue(status.getChanged().contains(repoRelativePath1));
		assertTrue(status.getRemoved().contains(repoRelativePath2));
	}

	private RevCommit setContentsAndCommit(IFile targetFile,
			String newContents, String commitMessage) throws Exception {
		targetFile.setContents(
				new ByteArrayInputStream(newContents.getBytes("UTF-8")),
				IResource.FORCE, new NullProgressMonitor());
		testRepo.addToIndex(targetFile);
		return testRepo.commit(commitMessage);
	}

	private void assertContentEquals(IFile file, String expectedContents)
			throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				file.getContents(), "UTF-8"));
		StringBuilder contentsBuilder = new StringBuilder();
		String line = reader.readLine();
		while (line != null) {
			contentsBuilder.append(line);
			contentsBuilder.append('\n');
			line = reader.readLine();
		}
		reader.close();
		assertEquals(expectedContents, contentsBuilder.toString());
	}

	private IMergeContext prepareContext(IFile workspaceFile, String srcRev,
			String dstRev) throws Exception {
		GitSynchronizeData gsd = new GitSynchronizeData(repo, srcRev, dstRev,
				true, Collections.<IResource> singleton(workspaceFile));
		GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(gsd);
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsds);
		subscriber.init(new NullProgressMonitor());

		ResourceMapping mapping = AdapterUtils.adapt(workspaceFile,
				ResourceMapping.class);
		SubscriberScopeManager manager = new SubscriberScopeManager(
				subscriber.getName(), new ResourceMapping[] { mapping, },
				subscriber, true);
		manager.initialize(new NullProgressMonitor());

		GitSubscriberMergeContext mergeContext = new GitSubscriberMergeContext(
				subscriber, manager, gsds);
		// Wait for asynchronous update of the diff tree to end
		Job.getJobManager().join(mergeContext, new NullProgressMonitor());
		return mergeContext;
	}

	private IMergeContext prepareModelContext(IFile workspaceFile,
			String srcRev,
			String dstRev) throws Exception {
		Set<IResource> includedResources = new HashSet<IResource>(
				Arrays.asList(workspaceFile));
		Set<IResource> newResources = new HashSet<IResource>(includedResources);
		Set<ResourceMapping> allMappings = new HashSet<ResourceMapping>();
		ResourceMappingContext mappingContext = ResourceMappingContext.LOCAL_CONTEXT;
		ModelProvider provider = new SampleProvider();
		do {
			Set<IResource> copy = newResources;
			newResources = new HashSet<IResource>();
			for (IResource resource : copy) {
				ResourceMapping[] mappings = provider.getMappings(resource,
						mappingContext, new NullProgressMonitor());
				allMappings.addAll(Arrays.asList(mappings));

				newResources.addAll(collectResources(mappings, mappingContext));
			}
		} while (includedResources.addAll(newResources));
		ResourceMapping[] mappings = allMappings
				.toArray(new ResourceMapping[allMappings.size()]);

		GitSynchronizeData gsd = new GitSynchronizeData(repo, srcRev, dstRev,
				true, includedResources);
		GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(gsd);
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsds);
		subscriber.init(new NullProgressMonitor());

		GitSubscriberResourceMappingContext resourceMappingContext = new GitSubscriberResourceMappingContext(
				subscriber, gsds);
		SubscriberScopeManager manager = new SubscriberScopeManager(
				subscriber.getName(), mappings, subscriber,
				resourceMappingContext, true);
		manager.initialize(new NullProgressMonitor());

		GitSubscriberMergeContext mergeContext = new GitSubscriberMergeContext(
				subscriber, manager, gsds);
		// Wait for asynchronous update of the diff tree to end
		Job.getJobManager().join(mergeContext, new NullProgressMonitor());
		return mergeContext;
	}

	private static Set<IResource> collectResources(ResourceMapping[] mappings,
			ResourceMappingContext mappingContext) throws Exception {
		final Set<IResource> resources = new HashSet<IResource>();
		for (ResourceMapping mapping : mappings) {
			ResourceTraversal[] traversals = mapping.getTraversals(
					mappingContext, new NullProgressMonitor());
			for (ResourceTraversal traversal : traversals)
				resources.addAll(Arrays.asList(traversal.getResources()));
		}
		return resources;
	}

	/**
	 * This model provider can be used to make all files of a given extension to
	 * be part of the same model. In this test, this will be used on files with
	 * extension {@link #SAMPLE_FILE_EXTENSION}.
	 */
	private static class SampleProvider extends ModelProvider {
		@Override
		public ResourceMapping[] getMappings(IResource resource,
				ResourceMappingContext context, IProgressMonitor monitor)
				throws CoreException {
			if (resource instanceof IFile
					&& SAMPLE_FILE_EXTENSION
							.equals(resource.getFileExtension())) {
				return new ResourceMapping[] { new SampleResourceMapping(
						(IFile) resource, SAMPLE_PROVIDER_ID), };
			}
			return super.getMappings(resource, context, monitor);
		}
	}

	/**
	 * This resource mapping will consider all files of extension
	 * {@link #SAMPLE_FILE_EXTENSION} in a given folder to be part of the same
	 * model.
	 */
	private static class SampleResourceMapping extends ResourceMapping {
		private final IFile file;

		private final String providerId;

		public SampleResourceMapping(IFile file, String providerId) {
			this.file = file;
			this.providerId = providerId;
		}

		@Override
		public Object getModelObject() {
			return file;
		}

		@Override
		public String getModelProviderId() {
			return providerId;
		}

		@Override
		public ResourceTraversal[] getTraversals(
				ResourceMappingContext context, IProgressMonitor monitor)
				throws CoreException {
			Set<IFile> sampleSiblings = new LinkedHashSet<IFile>();
			for (IResource res : file.getParent().members()) {
				if (res instanceof IFile
						&& SAMPLE_FILE_EXTENSION.equals(res.getFileExtension())) {
					sampleSiblings.add((IFile) res);
				}
			}
			final IResource[] resourceArray = sampleSiblings
					.toArray(new IResource[sampleSiblings.size()]);
			return new ResourceTraversal[] { new ResourceTraversal(
					resourceArray, IResource.DEPTH_ONE, IResource.NONE), };
		}

		@Override
		public IProject[] getProjects() {
			return new IProject[] { file.getProject(), };
		}
	}

	/**
	 * Since we do not register our model provider the plugin way, we won't have
	 * a descriptor for it, and the default implementation would fail. This will
	 * avoid all the useless calls to provider.getDescriptor().
	 */
	private static class SampleModelMerger extends ResourceMappingMerger {
		private final String providerID;

		public SampleModelMerger(String providerID) {
			this.providerID = providerID;
		}

		@Override
		protected ModelProvider getModelProvider() {
			return null;
		}

		@Override
		public IStatus merge(IMergeContext mergeContext,
				IProgressMonitor monitor) throws CoreException {
			IDiff[] deltas = getSetToMerge(mergeContext);
			IStatus status = mergeContext.merge(deltas, false /* don't force */,
					monitor);
			if (status.getCode() == IMergeStatus.CONFLICTS) {
				return new MergeStatus(status.getPlugin(), status.getMessage(),
						mergeContext.getScope().getMappings(providerID));
			}
			return status;
		}

		private IDiff[] getSetToMerge(IMergeContext mergeContext) {
			ResourceMapping[] mappings = mergeContext.getScope().getMappings(
					providerID);
			Set<IDiff> deltas = new HashSet<IDiff>();
			for (int i = 0; i < mappings.length; i++) {
				ResourceTraversal[] traversals = mergeContext.getScope()
						.getTraversals(mappings[i]);
				deltas.addAll(Arrays.asList(mergeContext.getDiffTree()
						.getDiffs(traversals)));
			}
			return deltas.toArray(new IDiff[deltas.size()]);
		}
	}
}
