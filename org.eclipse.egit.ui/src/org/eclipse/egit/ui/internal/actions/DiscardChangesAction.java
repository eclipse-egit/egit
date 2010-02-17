/*******************************************************************************
 * Copyright (C) 2010, Roland Grunberg <rgrunber@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * Checkout all selected resources.
 */
public class DiscardChangesAction extends RepositoryAction{

	@Override
	public void run(IAction action) {

		boolean performAction = MessageDialog.openConfirm(getShell(), "Discard Local Changes", "This will discard all local changes for the selected resources. Are you sure you want to do this ?");
		if (performAction){
			performDiscardChanges();
		}
	}

	private void performDiscardChanges() {
		for (final IResource res : getSelectedResources()) {
			final Repository repository = getRepository(true);
			// path to file relative to the repository
			String workingDirPath = repository.getWorkDir().getAbsolutePath().toString();
			String resPath = res.getLocation().toOSString();
			resPath = resPath.replaceFirst(workingDirPath, ""); //$NON-NLS-1$
			resPath = resPath.substring(1);

			try {
				Entry e = repository.getIndex().getEntry(resPath);
				repository.getIndex().checkoutEntry(repository.getWorkDir(), e);
				res.refreshLocal(0, new NullProgressMonitor());
			} catch (IOException e1) {
				ErrorDialog
						.openError(
								getShell(),
								"Error during checkout",
								"Some of the selected resources could not be checked out",
								new Status(IStatus.ERROR, TeamUIPlugin.ID, 1,
										TeamUIMessages.TeamAction_internal, e1));
			} catch (CoreException e) {
				ErrorDialog
						.openError(
								getShell(),
								"Error during refresh",
								"Some of the selected resources could not be refreshed",
								new Status(IStatus.ERROR, TeamUIPlugin.ID, 1,
										TeamUIMessages.TeamAction_internal, e));
			}

		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
