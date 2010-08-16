/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.Activator;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
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
		ModelProvider[] enabledProviders = super.getEnabledModelProviders();
		for (int i = 0; i < enabledProviders.length; i++) {
			ModelProvider provider = enabledProviders[i];
			if (provider.getId().equals(GitChangeSetModelProvider.ID))
				return enabledProviders;
		}

		ModelProvider[] extended = new ModelProvider[enabledProviders.length + 1];
		for (int i = 0; i < enabledProviders.length; i++) {
			extended[i] = enabledProviders[i];
		}

		GitChangeSetModelProvider provider = GitChangeSetModelProvider
				.getProvider();

		if (provider == null)
			return enabledProviders;

		extended[extended.length - 1] = provider;
		return extended;
	}

}
