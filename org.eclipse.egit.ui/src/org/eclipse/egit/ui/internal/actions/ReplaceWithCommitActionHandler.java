/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Benjamin Muskalla (Tasktop Technologies Inc.) - support for model scoping
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.egit.ui.internal.history.CommitSelectionDialog;
import org.eclipse.jface.window.Window;

/**
 * Replace with commit action handler
 */
public class ReplaceWithCommitActionHandler extends DiscardChangesActionHandler {

	@Override
	protected String gatherRevision(ExecutionEvent event) throws ExecutionException {
		CommitSelectionDialog dlg = new CommitSelectionDialog(getShell(event),
				getRepository(true, event));
		if (dlg.open() == Window.OK)
			return dlg.getCommitId().name();
		else
			throw new OperationCanceledException();
	}
}
