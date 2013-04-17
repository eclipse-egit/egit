/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christian Halstrick (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Robin Rosenberg - Adoption for the history menu
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.rebase.RebaseHelper;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.osgi.util.NLS;

/**
 * Executes the Rebase
 */
public class RebaseCurrentHandler extends AbstractHistoryCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		PlotCommit commit = (PlotCommit) getSelection(getPage()).getFirstElement();
		final Repository repository = getRepository(event);
		if (repository == null)
			return null;

		String currentBranch = getCurrentBranch(repository);
		Ref ref = getRef(commit, repository, currentBranch);

		String jobname = NLS.bind(
				UIText.RebaseCurrentRefCommand_RebasingCurrentJobName,
				currentBranch, ref.getName());
		RebaseHelper.runRebaseJob(repository, jobname, ref);
		return null;
	}

	private Ref getRef(PlotCommit commit, Repository repository, String currentBranch) {
		int count = commit.getRefCount();
		if (count == 0)
			return new ObjectIdRef.Unpeeled(Storage.LOOSE, commit.getName(), commit);
		else if (count == 1)
			return commit.getRef(0);
		else {
			BranchConfig branchConfig = new BranchConfig(repository.getConfig(), currentBranch);
			String trackingBranch = branchConfig.getTrackingBranch();
			Ref remoteRef = null;

			for (int i = 0; i < count; i++) {
				Ref ref = commit.getRef(i);
				if (trackingBranch != null && trackingBranch.equals(ref.getName()))
					return ref;
				if (ref.getName().startsWith(Constants.R_REMOTES))
					remoteRef = ref;
			}

			if (remoteRef != null)
				return remoteRef;
			else
				// We tried to pick a nice ref, just pick the first then
				return commit.getRef(0);
		}
	}

	private String getCurrentBranch(Repository repository) throws ExecutionException {
		try {
			return repository.getBranch();
		} catch (IOException e) {
			throw new ExecutionException(UIText.RebaseCurrentRefCommand_ErrorGettingCurrentBranchMessage, e);
		}
	}
}
