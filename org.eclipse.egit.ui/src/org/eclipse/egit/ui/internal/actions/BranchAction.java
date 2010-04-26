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
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;

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

		BranchSelectionDialog dialog = new BranchSelectionDialog(getShell(), repository, false);
		if (dialog.open() != Window.OK) {
			return;
		}

		final String refName = dialog.getRefName();
		try {
			getTargetPart().getSite().getWorkbenchWindow().run(true, false,
					new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor)
				throws InvocationTargetException {
					try {
						new BranchOperation(repository, refName).run(monitor);
						GitLightweightDecorator.refresh();
					} catch (final CoreException e) {
						if (GitTraceLocation.UI.isActive())
							GitTraceLocation.getTrace().trace(GitTraceLocation.UI.getLocation(), e.getMessage(), e);
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								handle(
										new TeamException(e.getStatus()),
										UIText.BranchAction_errorSwitchingBranches,
										UIText.BranchAction_unableToSwitchBranches);
							}
						});
					}
				}
			});
		} catch (InvocationTargetException e) {
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation.getTrace().trace(GitTraceLocation.UI.getLocation(), e.getMessage(), e);
			throw e;
		} catch (InterruptedException e) {
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation.getTrace().trace(GitTraceLocation.UI.getLocation(), e.getMessage(), e);
			throw new InvocationTargetException(e);
		}
	}

	@Override
	public boolean isEnabled() {
		return getRepository(false) != null;
	}
}
