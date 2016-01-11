/*******************************************************************************
 * Copyright (c) 2013, 2015 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.PushBranchWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * "Push Branch..." action for repository
 */
public class PushBranchActionHandler extends RepositoryActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(true, event);

		try {
			PushBranchWizard wizard = null;
			Ref ref = getBranchRef(repository);
			if (ref != null) {
				wizard = new PushBranchWizard(repository, ref);
			} else {
				ObjectId id = repository.resolve(repository.getFullBranch());
				if (id == null) {
					showNoHeadWarning(event);
					return null;
				}
				wizard = new PushBranchWizard(repository, id);
			}
			WizardDialog dlg = new WizardDialog(getShell(event), wizard);
			dlg.open();
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
		Ref head = getBranchRef(repository);
		if (head == null) {
			return false;
		}
		return true;
	}

	private Ref getBranchRef(Repository repository) {
		try {
			String fullBranch = repository.getFullBranch();
			if (fullBranch != null && fullBranch.startsWith(Constants.R_HEADS))
				return repository.exactRef(fullBranch);
		} catch (IOException e) {
			Activator.handleError(e.getLocalizedMessage(), e, false);
		}
		return null;
	}

	private void showNoHeadWarning(final ExecutionEvent event) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				MessageDialog.openWarning(HandlerUtil.getActiveShell(event),
						UIText.PushBranchActionHandler_EmptyRepository,
						UIText.PushBranchActionHandler_NoHead);
			}
		});
	}
}
