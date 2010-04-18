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

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;

/**
 *
 */
public class GitSynchronizeWizard extends Wizard {

	private GitBranchSynchronizeWizardPage page;

	/**
	 * Instantiates a new wizard for synchronizing resources that are being
	 * managed by EGit.
	 */
	public GitSynchronizeWizard() {
		setWindowTitle("Synchronize");
	}

	@Override
	public void addPages() {
		page = new GitBranchSynchronizeWizardPage();
		addPage(page);
	}

	private SubscriberParticipant getParticipant() {
		Set<IProject> projects = page.getSelectedProjects();
		ISynchronizeManager synchronizeManager = TeamUI.getSynchronizeManager();
		ISynchronizeParticipantReference[] participants = synchronizeManager
				.get("org.eclipse.egit.ui.synchronizeParticipant");
		if (participants.length == 0) {
			GitBranchSubscriberParticipant participant = new GitBranchSubscriberParticipant(
					page.getSelectedBranches(), projects
							.toArray(new IResource[projects.size()]));
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(
					new ISynchronizeParticipant[] { participant });
			return participant;
		}

		try {
			GitBranchSubscriberParticipant participant = (GitBranchSubscriberParticipant) participants[0]
					.getParticipant();
			participant.reset(page.getSelectedBranches(), projects
					.toArray(new IResource[projects.size()]));
			return participant;
		} catch (TeamException e) {
			GitBranchSubscriberParticipant participant = new GitBranchSubscriberParticipant(
					page.getSelectedBranches(), projects
							.toArray(new IResource[projects.size()]));
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(
					new ISynchronizeParticipant[] { participant });
			return participant;
		}
	}

	@Override
	public boolean performFinish() {
		Set<IProject> projects = page.getSelectedProjects();
		SubscriberParticipant participant = getParticipant();
		participant.refresh(projects.toArray(new IResource[projects.size()]),
				"Git Resource Synchronization", null, null);
		return true;
	}

}
