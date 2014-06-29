/*******************************************************************************
 * Copyright (C) 2014 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.stash.StashCreateUI;
import org.eclipse.jgit.lib.Repository;

/**
 * Handler for "Stash Changes...".
 */
public class StashCreateHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository();
		if (repository == null)
			return null;

		StashCreateUI stashCreateUI = new StashCreateUI(repository);
		stashCreateUI.createStash(getShell(event));

		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repository = getRepository();
		if (repository == null)
			return false;

		return repository.getRepositoryState().canCommit();
	}
}
