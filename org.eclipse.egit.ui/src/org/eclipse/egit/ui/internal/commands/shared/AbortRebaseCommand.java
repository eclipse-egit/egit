/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.api.RebaseCommand.Operation;

/**
 * Implements "Abort Rebase"
 */
public class AbortRebaseCommand extends AbstractRebaseCommandHandler {
	/**
	 * Default constructor
	 */
	public AbortRebaseCommand() {
		super(UIText.AbortRebaseCommand_CancelDialogMessage,
				UIText.RebaseCommandHandler_CancelDialogTitle);
	}

	@Override
	public RebaseOperation getRebaseOperation(ExecutionEvent event)
			throws ExecutionException {
		return new RebaseOperation(getRepository(event), Operation.ABORT);
	}

	@Override
	public String getJobName(RebaseOperation operation)
			throws ExecutionException {
		return UIText.AbortRebaseCommand_JobName;
	}
}
