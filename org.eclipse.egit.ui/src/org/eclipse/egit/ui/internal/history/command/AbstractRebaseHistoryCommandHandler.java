/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christian Halstrick (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Robin Rosenberg - Adoption for the history menu
 *    Tobias Pfeifer - Extracted from RebaseCurrentHandler
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;

/**
 * Common class for rebase commands in the HistoryView
 */
public abstract class AbstractRebaseHistoryCommandHandler extends
		AbstractHistoryCommandHandler {

	@Override
	public boolean isEnabled() {
		final Repository repository = getRepository(getPage());
		if (repository == null)
			return false;
		return repository.getRepositoryState().equals(RepositoryState.SAFE);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit revCommit = AdapterUtils
				.adapt(getSelection(event).getFirstElement(), RevCommit.class);
		if (!(revCommit instanceof PlotCommit)) {
			return null;
		}
		PlotCommit commit = (PlotCommit) revCommit;
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
			protected RebaseOperation createRebaseOperation(
					Repository repository2) throws ExecutionException {
				return AbstractRebaseHistoryCommandHandler.this
						.createRebaseOperation(
						repository2, ref);
			}
		};
		rebaseCurrentRef.execute(repository);
		return null;
	}

	/**
	 * Factory method delegating creation of RebaseOperation to concrete
	 * subclasses.
	 *
	 * @param repository
	 * @param ref
	 * @return the {@link RebaseOperation} to be executed
	 */
	protected abstract RebaseOperation createRebaseOperation(
			Repository repository, Ref ref);

	/**
	 * @param commit
	 * @param repository
	 * @param currentBranch
	 * @return ref pointing to the given commit, prefers tracking branch if
	 *         multiple refs are available
	 */
	protected Ref getRef(PlotCommit commit, Repository repository,
			String currentBranch) {
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

	/**
	 * @param repository
	 * @return the short name of the current branch that HEAD points to.
	 * @throws ExecutionException
	 */
	protected String getCurrentBranch(Repository repository)
			throws ExecutionException {
		try {
			return repository.getBranch();
		} catch (IOException e) {
			throw new ExecutionException(UIText.RebaseCurrentRefCommand_ErrorGettingCurrentBranchMessage, e);
		}
	}

}
