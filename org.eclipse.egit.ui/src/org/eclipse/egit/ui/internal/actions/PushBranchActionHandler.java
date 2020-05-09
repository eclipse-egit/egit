/*******************************************************************************
 * Copyright (c) 2013, 2020 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.push.PushMode;
import org.eclipse.egit.ui.internal.push.PushWizardDialog;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;

/**
 * "Push Branch..." action for repository
 */
public class PushBranchActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(true, event);
		if (repository == null) {
			return null;
		}
		try {
			Wizard wizard = PushMode.UPSTREAM.getWizard(repository, null);
			if (wizard != null) {
				PushWizardDialog dlg = new PushWizardDialog(getShell(event),
						wizard);
				dlg.open();
			}
		} catch (IOException ex) {
			Activator.handleError(ex.getLocalizedMessage(), ex, false);
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repository = getRepository();
		if (repository == null) {
			return false;
		}
		return SelectionRepositoryStateCache.INSTANCE.getHead(repository) != null;
	}
}
