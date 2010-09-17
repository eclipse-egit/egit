/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.synchronize.compare.ComparisonDataSource;
import org.eclipse.egit.ui.internal.synchronize.compare.GitCompareInput;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;

/**
 * Git model synchronization participant
 */
public class GitModelSynchronizeParticipant extends ModelSynchronizeParticipant {

	/**
	 * Id of model compare participant
	 */
	public static final String ID = "org.eclipse.egit.ui.modelCompareParticipant"; //$NON-NLS-1$

	/**
	 * Id of model synchronization participant
	 */
	public static final String VIEWER_ID = "org.eclipse.egit.ui.compareSynchronization"; //$NON-NLS-1$

	private static final String WORKSPACE_MODEL_PROVIDER_ID = "org.eclipse.core.resources.modelProvider"; //$NON-NLS-1$

	private final GitSynchronizeDataSet gsds;

	/**
	 * Creates {@link GitModelSynchronizeParticipant} for given context
	 *
	 * @param context
	 */
	public GitModelSynchronizeParticipant(GitSubscriberMergeContext context) {
		super(context);
		gsds = context.getSyncData();

		try {
			setInitializationData(TeamUI.getSynchronizeManager()
					.getParticipantDescriptor(ID));
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
		}

		setSecondaryId(Long.toString(System.currentTimeMillis()));
	}

	protected void initializeConfiguration(
			ISynchronizePageConfiguration configuration) {
		configuration.setProperty(ISynchronizePageConfiguration.P_VIEWER_ID,
				VIEWER_ID);
		configuration.setProperty(
				ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER,
				GitChangeSetModelProvider.ID);
		super.initializeConfiguration(configuration);
	}

	@Override
	public ModelProvider[] getEnabledModelProviders() {
		List<ModelProvider> providers = new ArrayList<ModelProvider>();
		ModelProvider[] avaliableProviders = super.getEnabledModelProviders();
		if (!includeResourceModelProvider()) {
			for (ModelProvider provider : avaliableProviders)
				if (provider.getId().equals(WORKSPACE_MODEL_PROVIDER_ID)) {
					providers.add(provider);
					break;
				}

			providers.add(GitChangeSetModelProvider.getProvider());
		} else {
			boolean addGitProvider = true;

			for (ModelProvider provider : avaliableProviders) {
				String providerId = provider.getId();
				providers.add(provider);

				if (addGitProvider
						&& providerId.equals(GitChangeSetModelProvider.ID))
					addGitProvider = false;
			}

			if (addGitProvider)
				providers.add(GitChangeSetModelProvider.getProvider());
		}

		return providers.toArray(new ModelProvider[providers.size()]);
	}

	@Override
	public ICompareInput asCompareInput(Object object) {
		// handle file comparison in Workspace model
		if (object instanceof IFile) {
			IFile file = (IFile) object;
			GitSynchronizeData gsd = gsds.getData(file.getProject());
			if (!gsd.shouldIncludeLocal())
				return getFileFromGit(gsd, file.getLocation());
		}

		return super.asCompareInput(object);
	}

	private boolean includeResourceModelProvider() {
		GitSubscriberMergeContext context = (GitSubscriberMergeContext) getContext();
		for (GitSynchronizeData gsd : context.getSyncData())
			if (!gsd.shouldIncludeLocal())
				return false;

		return true;
	}

	private ICompareInput getFileFromGit(GitSynchronizeData gsd, IPath location) {
		Repository repo = gsd.getRepository();
		File workTree = repo.getWorkTree();
		String repoRelativeLocation = Repository.stripWorkDir(workTree,
				location.toFile());

		TreeWalk tw = new TreeWalk(repo);
		tw.setRecursive(true);
		tw.setFilter(PathFilter.create(repoRelativeLocation.toString()));
		RevCommit baseCommit = gsd.getSrcRevCommit();
		RevCommit remoteCommit = gsd.getDstRevCommit();

		try {
			int baseNth = tw.addTree(baseCommit.getTree());
			int remoteNth = tw.addTree(remoteCommit.getTree());

			if (tw.next()) {
				ComparisonDataSource baseData = new ComparisonDataSource(
						baseCommit, tw.getObjectId(baseNth));
				ComparisonDataSource remoteData = new ComparisonDataSource(
						remoteCommit, tw.getObjectId(remoteNth));
				return new GitCompareInput(repo, baseData, baseData,
						remoteData, repoRelativeLocation);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return null;
	}

}
