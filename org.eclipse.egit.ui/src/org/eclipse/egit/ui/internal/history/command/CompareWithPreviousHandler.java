/*******************************************************************************
 * Copyright (C) 2024 Thomas Wolf <twolf@apache.org> and others
 *
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
import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.ui.internal.commit.DiffViewer;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Handler for showing a two-way comparison between the current commit and the
 * base commit for a {@link FileDiff}.
 */
public class CompareWithPreviousHandler extends AbstractHistoryCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		FileDiff diff = getDiff(selection);
		if (diff != null) {
			DiffViewer.showTwoWayFileDiff(diff);
		}
		return null;
	}

	private FileDiff getDiff(IStructuredSelection selection) {
		if (selection.size() == 1) {
			return Adapters.adapt(selection.getFirstElement(), FileDiff.class);
		}
		return null;
	}

}
