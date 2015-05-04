/*******************************************************************************
 * Copyright (c) 2012, 2014 Markus Duft <markus.duft@salomon.at> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.clean.CleanWizardDialog;
import org.eclipse.jgit.lib.Repository;

/**
 * Clean untracked/ignored files.
 */
public class CleanActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository();

		CleanWizardDialog dlg = new CleanWizardDialog(getShell(event),
				repository);
		dlg.setBlockOnOpen(true);
		dlg.open();

		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repository = getRepository();
		return repository != null && !repository.isBare();
	}
}
