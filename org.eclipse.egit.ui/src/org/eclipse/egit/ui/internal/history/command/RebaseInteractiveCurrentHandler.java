/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveHandler;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;


/**
 * Executes the Rebase (interactively)
 */
public class RebaseInteractiveCurrentHandler extends AbstractRebaseHistoryCommandHandler {

	@Override
	protected RebaseOperation createRebaseOperation(Repository repository,
			Ref ref) {
		return new RebaseOperation(repository, ref,
				RebaseInteractiveHandler.INSTANCE);
	}
}
