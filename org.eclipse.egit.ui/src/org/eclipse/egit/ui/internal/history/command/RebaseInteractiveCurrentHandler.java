/*******************************************************************************
 * Copyright (c) 2013, 2017 SAP AG and others.
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
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveHandler;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveView;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;


/**
 * Executes the Rebase (interactively)
 */
public class RebaseInteractiveCurrentHandler extends AbstractRebaseHistoryCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(event);
		if (repository == null) {
			return null;
		}
		if (LaunchFinder.shouldCancelBecauseOfRunningLaunches(repository,
				null)) {
			return null;
		}
		super.execute(event);
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
