/*******************************************************************************
 * Copyright (c) 2013, 2014 Robin Stocker <robin@nibor.org> and others.
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
import org.eclipse.egit.ui.internal.push.PushBranchWizard;
import org.eclipse.egit.ui.internal.push.PushWizardDialog;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;

/**
 * "Push Branch..." action for repository
 */
public class PushBranchActionHandler extends RepositoryActionHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(true, event);
		Shell shell = getShell(event);

		openPushHEADWizard(repository, shell);

		return null;
	}

	/**
	 * Opens a {@link PushBranchWizard}, for the current branch or, if detached
	 * HEAD, from HEAD commit.
	 *
	 * @param repository
	 * @param shell
	 */
	public static void openPushHEADWizard(Repository repository, Shell shell) {
		try {
			PushBranchWizard wizard = null;
			Ref ref = getBranchRef(repository);
			if (ref != null) {
				wizard = new PushBranchWizard(repository, ref);
			} else {
				ObjectId id = repository.resolve(repository.getFullBranch());
				wizard = new PushBranchWizard(repository, id);
			}
			PushWizardDialog dlg = new PushWizardDialog(shell,
					wizard);
			dlg.open();
		} catch (IOException ex) {
			Activator.handleError(ex.getLocalizedMessage(), ex, false);
		}
	}

	@Override
	public boolean isEnabled() {
		Repository repository = getRepository();
		if (repository == null) {
			return false;
		}
		return SelectionRepositoryStateCache.INSTANCE.getHead(repository) != null;
	}

	private static Ref getBranchRef(Repository repository) {
		try {
			String fullBranch = repository.getFullBranch();
			if (fullBranch != null && fullBranch.startsWith(Constants.R_HEADS))
				return repository.exactRef(fullBranch);
		} catch (IOException e) {
			Activator.handleError(e.getLocalizedMessage(), e, false);
		}
		return null;
	}
}
