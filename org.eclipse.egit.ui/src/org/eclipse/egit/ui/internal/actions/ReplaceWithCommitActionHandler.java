/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.ui.internal.history.CommitSelectionDialog;
import org.eclipse.jface.window.Window;

/**
 * Replace with commit action handler
 */
public class ReplaceWithCommitActionHandler extends DiscardChangesActionHandler {

	/**
	 * @see org.eclipse.egit.ui.internal.actions.DiscardChangesActionHandler#createOperation(org.eclipse.core.commands.ExecutionEvent)
	 */
	protected DiscardChangesOperation createOperation(ExecutionEvent event)
			throws ExecutionException {
		CommitSelectionDialog dlg = new CommitSelectionDialog(getShell(event),
				getRepository(true, event));
		if (dlg.open() != Window.OK)
			return null;

		return new DiscardChangesOperation(getSelectedResources(event), dlg
				.getCommitId().name());
	}

}
