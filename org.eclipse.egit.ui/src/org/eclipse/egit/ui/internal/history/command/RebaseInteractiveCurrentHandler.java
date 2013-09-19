/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler;
import org.eclipse.egit.ui.internal.rebase.RebaseInteracitveHandler;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveView;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Executes the Rebase (interactively) TODO: This is a copy of
 * {@link RebaseCurrentHandler}. Refactoring (extract common code to new
 * superclass)
 */
public class RebaseInteractiveCurrentHandler extends AbstractHistoryCommandHandler {

	@Override
	public boolean isEnabled() {
		final Repository repository = getRepository(getPage());
		if (repository == null)
			return false;
		return repository.getRepositoryState().equals(RepositoryState.SAFE);
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {

		PlotCommit commit = (PlotCommit) getSelection(getPage()).getFirstElement();
		final Repository repository = getRepository(event);
		if (repository == null)
			return null;

		String currentBranch = getCurrentBranch(repository);
		final Ref ref = getRef(commit, repository, currentBranch);

		String jobname = NLS.bind(
				UIText.RebaseCurrentRefCommand_RebasingCurrentJobName,
				currentBranch, ref.getName());
		AbstractRebaseCommandHandler rebaseCurrentRef = new AbstractRebaseCommandHandler(
				jobname, UIText.RebaseCurrentRefCommand_RebaseCanceledMessage) {
			@Override
			public RebaseOperation createRebaseOperation(ExecutionEvent event2)
					throws ExecutionException {
				return new RebaseOperation(repository, ref,
						RebaseInteracitveHandler.INSTANCE);
			}
		};
		rebaseCurrentRef.execute(event);
		try {
			RebaseInteractiveView rebaseInteractiveView = (RebaseInteractiveView) HandlerUtil
					.getActiveWorkbenchWindowChecked(event).getActivePage()
					.showView(RebaseInteractiveView.VIEW_ID);
			rebaseInteractiveView.setInput(repository);
		} catch (PartInitException e) {
			Activator.showError(e.getMessage(), e);
		}
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
