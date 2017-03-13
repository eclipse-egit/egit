/*******************************************************************************
 * Copyright (C) 2014 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.models;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.IResourceMappingMerger;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.junit.Before;

/**
 * Provides shared utility methods for unit tests working on logical models. The
 * model provider used for tests, {@link SampleModelProvider}, links all
 * "*.sample" files from a common directory into a single logical model.
 */
public abstract class ModelTestCase extends GitTestCase {
	protected static final String SAMPLE_FILE_EXTENSION = SampleModelProvider.SAMPLE_FILE_EXTENSION;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		IContentType textType = Platform.getContentTypeManager()
				.getContentType("org.eclipse.core.runtime.text");
		textType.addFileSpec(SAMPLE_FILE_EXTENSION,
				IContentType.FILE_EXTENSION_SPEC);
	}

	protected RevCommit setContentsAndCommit(TestRepository testRepository,
			IFile targetFile, String newContents, String commitMessage)
			throws Exception {
		targetFile.setContents(
				new ByteArrayInputStream(newContents.getBytes("UTF-8")),
				IResource.FORCE, new NullProgressMonitor());
		testRepository.addToIndex(targetFile);
		return testRepository.commit(commitMessage);
	}

	protected void assertContentEquals(IFile file, String expectedContents)
			throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				file.getContents(), file.getCharset()));
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

	protected void merge(Repository repository, String refName)
			throws CoreException {
		new MergeOperation(repository, refName).execute(null);
	}

	protected Status status(Repository repository) throws Exception {
		return new Git(repository).status().call();
	}

	protected IResourceMappingMerger createMerger() throws CoreException {
		final ModelProvider provider = ModelProvider
				.getModelProviderDescriptor(
						SampleModelProvider.SAMPLE_PROVIDER_ID)
				.getModelProvider();
		return Utils.getAdapter(provider, IResourceMappingMerger.class);
	}

	protected IMergeContext prepareContext(Repository repository,
			IFile workspaceFile, String srcRev, String dstRev) throws Exception {
		GitSynchronizeData gsd = new GitSynchronizeData(repository, srcRev,
				dstRev, true, Collections.<IResource> singleton(workspaceFile));
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

	protected IMergeContext prepareModelContext(Repository repository,
			IFile workspaceFile, String srcRev, String dstRev) throws Exception {
		Set<IResource> includedResources = new HashSet<IResource>(
				Arrays.asList(workspaceFile));
		Set<IResource> newResources = new HashSet<IResource>(includedResources);
		Set<ResourceMapping> allMappings = new HashSet<ResourceMapping>();
		ResourceMappingContext mappingContext = ResourceMappingContext.LOCAL_CONTEXT;
		ModelProvider provider = ModelProvider.getModelProviderDescriptor(
				SampleModelProvider.SAMPLE_PROVIDER_ID).getModelProvider();
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

		GitSynchronizeData gsd = new GitSynchronizeData(repository, srcRev,
				dstRev, true, includedResources);
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
}
