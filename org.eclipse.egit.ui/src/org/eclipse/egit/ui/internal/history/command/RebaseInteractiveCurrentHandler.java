/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveHandler;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveView;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;


/**
 * Executes the Rebase (interactively)
 */
public class RebaseInteractiveCurrentHandler extends AbstractRebaseHistoryCommandHandler {

	@Override
	public boolean isEnabled() {
		final Repository repository = getRepository(getPage());
		if (repository == null)
			return false;
		return repository.getRepositoryState().equals(RepositoryState.SAFE);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		PlotCommit commit = (PlotCommit) getSelection(event).getFirstElement();
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
				return new RebaseOperation(repository2, ref,
						RebaseInteractiveHandler.INSTANCE);
			}
		};
		rebaseCurrentRef.execute(repository);
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

	@Override
	protected RebaseOperation createRebaseOperation(Repository repository,
			Ref ref) {
		return new RebaseOperation(repository, ref,
				RebaseInteractiveHandler.INSTANCE);
	}
}
