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
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Executes the Rebase
 */
public class RebaseCurrentHandler extends AbstractRebaseHistoryCommandHandler {

	@Override
	protected RebaseOperation createRebaseOperation(Repository repository,
			Ref ref) {
		if (LaunchFinder.shouldCancelBecauseOfRunningLaunches(repository,
				null)) {
			return null;
		}
		return new RebaseOperation(repository, ref);
	}

}
