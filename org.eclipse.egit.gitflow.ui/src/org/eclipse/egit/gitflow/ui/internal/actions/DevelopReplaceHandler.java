/*******************************************************************************
 * Copyright (C) 2016, Max Hohenegger <eclipse@hohenegger.eu> and others
 *
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
package org.eclipse.egit.gitflow.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.actions.DiscardChangesActionHandler;

/**
 * Replace content of selected resources with that on develop branch.
 */
public class DevelopReplaceHandler extends DiscardChangesActionHandler {

	@Override
	protected String gatherRevision(ExecutionEvent event)
			throws ExecutionException {
		try {
			return GitFlowHandlerUtil.gatherRevision(event);
		} catch (IOException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
	}
}
