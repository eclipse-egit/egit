/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.dialogs.DeleteBranchDialog;
import org.eclipse.jgit.lib.Repository;

/**
 * Action for deleting a branch
 */
public class DeleteBranchActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null)
			return null;
		DeleteBranchDialog dialog = new DeleteBranchDialog(getShell(event),
				repository);
		dialog.open();
		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repo = getRepository();
		return repo != null && containsHead(repo);
	}
}
