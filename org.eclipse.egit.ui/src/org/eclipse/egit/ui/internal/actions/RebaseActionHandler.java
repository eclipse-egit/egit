/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.ui.internal.commands.shared.RebaseCurrentRefCommand;

/**
 * An action to rebase the current branch on top of given branch.
 *
 * @see RebaseOperation
 */
public class RebaseActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		return new RebaseCurrentRefCommand().execute(event);
	}

	@Override
	public boolean isEnabled() {
		return getRepository() != null && containsHead();
	}
}
