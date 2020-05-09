/*******************************************************************************
 * Copyright (c) 2013, 2020 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveHandler;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;


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
		return super.execute(event);
	}

	@Override
	protected RebaseOperation createRebaseOperation(Repository repository,
			Ref ref) {
		return new RebaseOperation(repository, ref,
				RebaseInteractiveHandler.INSTANCE);
	}
}
