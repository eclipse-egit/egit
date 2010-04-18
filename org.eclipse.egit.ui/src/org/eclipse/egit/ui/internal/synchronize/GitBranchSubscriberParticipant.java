/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

class GitBranchSubscriberParticipant extends SubscriberParticipant {

	GitBranchSubscriberParticipant(Map<Repository, String> branches,
			IResource[] roots) {
		setSubscriber(new GitBranchResourceVariantTreeSubscriber(branches,
				roots));
		setName("Git");
	}

	void reset(Map<Repository, String> branches, IResource[] roots) {
		GitBranchResourceVariantTreeSubscriber subscriber = (GitBranchResourceVariantTreeSubscriber) getSubscriber();
		subscriber.reset(branches, roots);
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
		return "org.eclipse.egit.ui.synchronizeParticipant"; //$NON-NLS-1$
	}

	@Override
	public String getSecondaryId() {
		// need to figure out what this is for, null is supposed to be
		// acceptable but Team throws an NPE, see bug 256961
		return "secondaryId";
	}

}
