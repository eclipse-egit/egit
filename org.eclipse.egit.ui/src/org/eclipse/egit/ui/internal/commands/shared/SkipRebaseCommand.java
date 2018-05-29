/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveHandler;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements "Skip Rebase"
 */
public class SkipRebaseCommand extends AbstractRebaseCommandHandler {
	/**
	 * Default constructor
	 */
	public SkipRebaseCommand() {
		super(UIText.SkipRebaseCommand_JobName,
				UIText.SkipRebaseCommand_CancelDialogMessage);
	}

	@Override
	public RebaseOperation createRebaseOperation(Repository repository) {
		return new RebaseOperation(repository, Operation.SKIP,
				RebaseInteractiveHandler.INSTANCE);
	}
}
