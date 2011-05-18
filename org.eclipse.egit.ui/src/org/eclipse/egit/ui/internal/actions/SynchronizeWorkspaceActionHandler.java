/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.synchronize.GitModelSynchronize;
import org.eclipse.jgit.lib.Repository;

/**
 * An action that launch synchronization with selected repository
 */
public class SynchronizeWorkspaceActionHandler extends RepositoryActionHandler {

	@Override
	public boolean isEnabled() {
		return true;
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository[] repos = getRepositories(event);

		if (repos.length == 0)
			return null;

		GitSynchronizeDataSet gsdSet = new GitSynchronizeDataSet();
		for (Repository repo : repos)
			try {
				gsdSet.add(new GitSynchronizeData(repo, HEAD, HEAD, true));
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}

		GitModelSynchronize.launch(gsdSet, getSelectedResources(event));

		return null;
	}

}
