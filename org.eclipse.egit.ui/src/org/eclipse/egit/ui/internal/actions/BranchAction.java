/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.storage.file.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * Action for selecting a branch and checking it out.
 *
 * @see BranchOperation
 */
public class BranchAction extends RepositoryAction {
	@Override
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final Repository repository = getRepository(true);
		if (repository == null)
			return;

		if (!repository.getRepositoryState().canCheckout()) {
			MessageDialog.openError(getShell(),
					UIText.BranchAction_cannotCheckout, NLS.bind(
							UIText.BranchAction_repositoryState, repository
									.getRepositoryState().getDescription()));
			return;
		}

		BranchSelectionDialog dialog = new BranchSelectionDialog(getShell(), repository);
		if (dialog.open() != Window.OK) {
			return;
		}

		final String refName = dialog.getRefName();

		String jobname = NLS.bind(UIText.BranchAction_checkingOut, refName);
		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					new BranchOperation(repository, refName).execute(monitor);
				} catch (CoreException e) {
					return Activator.createErrorStatus(
							UIText.BranchAction_branchFailed, e);
				} finally {
					GitLightweightDecorator.refresh();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	@Override
	public boolean isEnabled() {
		return getRepository(false) != null;
	}
}
