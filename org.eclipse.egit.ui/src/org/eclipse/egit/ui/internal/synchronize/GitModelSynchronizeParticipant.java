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

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.synchronize.compare.ComparisonDataSource;
import org.eclipse.egit.ui.internal.synchronize.compare.GitCompareInput;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
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
	 * Key value for obtaining {@link GitSynchronizeDataSet} from {@link ISynchronizePageConfiguration}
	 */
	public static final String SYNCHRONIZATION_DATA = "GIT_SYNCHRONIZE_DATA_SET"; //$NON-NLS-1$

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
		String modelProvider;
		if (Activator
				.getDefault()
				.getPreferenceStore()
				.getBoolean(UIPreferences.SYNC_VIEW_ALWAYS_SHOW_CHANGESET_MODEL))
			modelProvider = GitChangeSetModelProvider.ID;
		else
			modelProvider = WORKSPACE_MODEL_PROVIDER_ID;
		configuration.setProperty(
				ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER,
				modelProvider);

		configuration.setProperty(SYNCHRONIZATION_DATA, gsds);

		super.initializeConfiguration(configuration);

		configuration.addActionContribution(new GitActionContributor());
	}

	@Override
	public ModelProvider[] getEnabledModelProviders() {
		ModelProvider[] avaliableProviders = super.getEnabledModelProviders();

		for (ModelProvider provider : avaliableProviders)
			if (provider.getId().equals(GitChangeSetModelProvider.ID))
				return avaliableProviders;

		int capacity = avaliableProviders.length + 1;
		ArrayList<ModelProvider> providers = new ArrayList<ModelProvider>(
				capacity);
		providers.add(GitChangeSetModelProvider.getProvider());

		return providers.toArray(new ModelProvider[providers.size()]);
	}

	@Override
	public boolean hasCompareInputFor(Object object) {
		if (object instanceof GitModelBlob || object instanceof IFile)
			return true;
		// in Java Workspace model Java source files are passed as type
		// CompilationUnit which can be adapted to IResource
		if (object instanceof IAdaptable) {
			IResource res = (IResource) ((IAdaptable) object)
					.getAdapter(IResource.class);
			if (res != null && res.getType() == IResource.FILE)
				return true;
		}
		return super.hasCompareInputFor(object);
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
