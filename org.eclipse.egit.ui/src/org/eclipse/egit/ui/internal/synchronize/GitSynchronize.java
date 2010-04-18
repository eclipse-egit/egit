/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference;

/**
 * Perform synchronize action with selected branches on selected resources.
 */
public class GitSynchronize {

	/**
	 * Constructs GitSynchronize
	 * @param data
	 */
	public GitSynchronize(GitSynchronizeData data) {
		this(new GitSynchronizeDataSet(data));
	}

	/**
	 * Constructs GitSynchronize
	 * @param data
	 */
	public GitSynchronize(GitSynchronizeDataSet data) {
		GitSubscriberParticipant participant = getParticipant(data);
		participant.refresh(data);
	}

	private GitSubscriberParticipant getParticipant(GitSynchronizeDataSet data) {
		ISynchronizeManager synchronizeManager = TeamUI.getSynchronizeManager();
		ISynchronizeParticipantReference[] participants = synchronizeManager
				.get(GitSubscriberParticipant.PARTICIPANT_NAME);

		GitSubscriberParticipant participant;

		if (participants.length == 0) {
			participant = createDefaultParticipant(data);
		} else {
			try {
				participant = (GitSubscriberParticipant) participants[0].getParticipant();
				participant.reset(data);
			} catch (TeamException e) {
				participant = createDefaultParticipant(data);
			}
		}
		return participant;
	}

	private GitSubscriberParticipant createDefaultParticipant(GitSynchronizeDataSet data) {
		GitSubscriberParticipant participant = new GitSubscriberParticipant(data);
		TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] { participant });

		return participant;
	}

}
