/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.UIText;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.core.variants.SessionResourceVariantByteStore;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

/**
 * Git synchronize participant that displays synchronization information for
 * local resources that are managed via a {@link Subscriber}. It maintains a
 * dynamic collection of all out-of-sync resources by listening to workspace
 * resource changes and remote changes thus creating a live view of changes in
 * the workspace.
 */
public class GitSubscriberParticipant extends SubscriberParticipant {

	/**
	 * Name of Git synchronization participant
	 */
	public static final String PARTICIPANT_NAME = "org.eclipse.egit.ui.synchronizeParticipant"; //$NON-NLS-1$

	/**
	 * Construct GitBranchSubscriberParticipant.
	 *
	 * @param data
	 */
	public GitSubscriberParticipant(GitSynchronizeDataSet data) {
		ResourceVariantByteStore store = new SessionResourceVariantByteStore();
		setSubscriber(new GitResourceVariantTreeSubscriber(data, store));
		setName(UIText.GitBranchSubscriberParticipant_git);
	}

	@Override
	protected void initializeConfiguration(
			ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);

		configuration.addActionContribution(new SynchronizePageActionGroup() {
			public void initialize(
					ISynchronizePageConfiguration pageConfiguration) {
				super.initialize(pageConfiguration);
				appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
						ISynchronizePageConfiguration.SYNCHRONIZE_GROUP,
						new CommitAction(pageConfiguration));
			}
		});

		configuration
				.setSupportedModes(ISynchronizePageConfiguration.ALL_MODES);
		configuration.setMode(ISynchronizePageConfiguration.BOTH_MODE);
	}

	@Override
	public String getId() {
		// note, this value needs to match the value in the plugin.xml
		return PARTICIPANT_NAME;
	}

	@Override
	public String getSecondaryId() {
		// need to figure out what this is for, null is supposed to be
		// acceptable but Team throws an NPE, see bug 256961
		return "secondaryId"; //$NON-NLS-1$
	}

	/**
	 * @param data
	 *
	 */
	public void refresh(GitSynchronizeDataSet data) {
		refresh(data.getAllResources(),
				UIText.GitSynchronizeWizard_gitResourceSynchronization, null,
				null);
	}

	/**
	 * @param data
	 */
	void reset(GitSynchronizeDataSet data) {
		GitResourceVariantTreeSubscriber subscriber = (GitResourceVariantTreeSubscriber) getSubscriber();
		subscriber.reset(data);
		reset();
	}

}
