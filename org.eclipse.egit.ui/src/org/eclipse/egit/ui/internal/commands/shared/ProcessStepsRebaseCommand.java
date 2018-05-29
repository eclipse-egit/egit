/*******************************************************************************
 * Copyright (c) 2013, 2016 SAP AG and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveHandler;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements the RebaseCommand that is used to start processing the steps. This
 * command needs to be called the first time after a {@link RebaseOperation}
 * stopped and returned with status
 * {@link org.eclipse.jgit.api.RebaseResult.Status#INTERACTIVE_PREPARED}
 */
public class ProcessStepsRebaseCommand extends AbstractRebaseCommandHandler {
	/**
	 * Default constructor
	 */
	public ProcessStepsRebaseCommand() {
		super(UIText.ProcessStepsRebaseCommand_JobName,
				UIText.ProcessStepsRebaseCommand_CancelDialogMessage);
	}

	@Override
	protected RebaseOperation createRebaseOperation(Repository repository)
			throws ExecutionException {
		if (LaunchFinder.shouldCancelBecauseOfRunningLaunches(repository,
				null)) {
			return null;
		}
		return new RebaseOperation(repository, Operation.PROCESS_STEPS,
				RebaseInteractiveHandler.INSTANCE);
	}
}
