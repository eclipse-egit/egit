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
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.rebase.RebaseInteracitveHandler;
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
		return new RebaseOperation(repository, Operation.PROCESS_STEPS,
				RebaseInteracitveHandler.INSTANCE);
	}
}
