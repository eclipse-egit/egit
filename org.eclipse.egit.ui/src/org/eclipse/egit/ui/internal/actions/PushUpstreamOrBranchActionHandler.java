/*******************************************************************************
 * Copyright (C) 2010, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.push.MultiPushOperationUI;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Action for "Push to Upstream" or "Push Branch..." if not configured
 */
public class PushUpstreamOrBranchActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository[] repositories = getRepositories(event);

		if (repositories == null) {
			return null;
		}

		MultiPushOperationUI multiPushOperationUI = new MultiPushOperationUI();
		multiPushOperationUI.configureAndPushRepositories(repositories);

		return null;
	}

	@Override
	public boolean isEnabled() {
		final Repository[] repositories = getRepositories();
		for (final Repository repository : repositories) {
			if (repository == null) {
				return false;
			}

			final Ref head = MultiPushOperationUI.getHeadIfSymbolic(repository);
			if (head == null) {
				return false;
			}
		}
		return true;
	}
}
