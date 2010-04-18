/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org> - changed class access to public,
 *     				exported participant name to public field, improved
 *     				reset(Map, IResource[]) method implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.subscribers.Subscriber;
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
public class GitBranchSubscriberParticipant extends SubscriberParticipant {

	/**
	 * Name of Git synchronization participant
	 */
	public static final String PARTICIPANT_NAME = "org.eclipse.egit.ui.synchronizeParticipant"; //$NON-NLS-1$

	/**
	 * Construct GitBranchSubscriberParticipant.
	 *
	 * @param branches
	 * @param roots
	 */
	public GitBranchSubscriberParticipant(Map<Repository, String> branches,
			IResource[] roots) {
		setSubscriber(new GitBranchResourceVariantTreeSubscriber(branches,
				roots));
		setName(UIText.GitBranchSubscriberParticipant_git);
	}

	/**
	 *
	 * @param branches
	 * @param roots
	 */
	public void reset(Map<Repository, String> branches, IResource[] roots) {
		GitBranchResourceVariantTreeSubscriber subscriber = (GitBranchResourceVariantTreeSubscriber) getSubscriber();
		subscriber.reset(branches, roots);
		reset();
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

}
