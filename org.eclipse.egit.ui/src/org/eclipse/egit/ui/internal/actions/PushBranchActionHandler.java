/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.push.PushBranchWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * "Push Branch..." action for repository
 */
public class PushBranchActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(true, event);

		Ref branchRef = getBranchRef(repository);
		if (branchRef != null) {
			PushBranchWizard wizard = new PushBranchWizard(repository, branchRef);
			WizardDialog dlg = new WizardDialog(getShell(event), wizard);
			dlg.open();
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		Repository repository = getRepository();
		return getBranchRef(repository) != null;
	}

	private Ref getBranchRef(Repository repository) {
		try {
			String fullBranch = repository.getFullBranch();
			if (fullBranch != null && fullBranch.startsWith(Constants.R_HEADS))
				return repository.getRef(fullBranch);
		} catch (IOException e) {
			// We'll return null then, see below
		}
		return null;
	}
}
