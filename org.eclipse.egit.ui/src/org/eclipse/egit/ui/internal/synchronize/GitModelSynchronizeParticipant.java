/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;

/**
 * Git model synchronization participant
 */
public class GitModelSynchronizeParticipant extends ModelSynchronizeParticipant {

	private static final String WORKSPACE_MODEL_PROVIDER = "org.eclipse.core.resources.modelProvider"; //$NON-NLS-1$

	/**
	 * Id of model compare participant
	 */
	public static final String ID = "org.eclipse.egit.ui.modelCompareParticipant"; //$NON-NLS-1$

	/**
	 * Id of model synchronization participant
	 */
	public static final String VIEWER_ID = "org.eclipse.egit.ui.compareSynchronization"; //$NON-NLS-1$

	/**
	 * Creates {@link GitModelSynchronizeParticipant} for given context
	 *
	 * @param context
	 */
	public GitModelSynchronizeParticipant(SynchronizationContext context) {
		super(context);
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
		boolean addGitProvider = true;
		List<ModelProvider> providers = new ArrayList<ModelProvider>();
		boolean includeResourceModel = includeResourceModelProvider();
		ModelProvider[] enabledProviders = super.getEnabledModelProviders();

		for (ModelProvider provider : enabledProviders) {
			String providerId = provider.getId();
			if (!providerId.equals(WORKSPACE_MODEL_PROVIDER)
					|| includeResourceModel)
				providers.add(provider);

			if (addGitProvider
					&& providerId.equals(GitChangeSetModelProvider.ID))
				addGitProvider = false;
		}

		if (addGitProvider)
			providers.add(GitChangeSetModelProvider.getProvider());

		return providers.toArray(new ModelProvider[providers.size()]);
	}

	private boolean includeResourceModelProvider() {
		GitSubscriberMergeContext context = (GitSubscriberMergeContext) getContext();
		for (GitSynchronizeData gsd : context.getSyncData())
			if (!gsd.shouldIncludeLocal())
				return false;

		return true;
	}

}
