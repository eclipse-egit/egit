/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * "Removes" one or several nodes
 */
public class RemoveCommand extends
		RepositoriesViewCommandHandler<RepositoryNode> implements IHandler {
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) getView(
				event).getSite()
				.getService(IWorkbenchSiteProgressService.class);

		Job job = new Job("Remove Repositories Job") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<IProject> projectsToDelete = new ArrayList<IProject>();

				monitor
						.setTaskName(UIText.RepositoriesView_DeleteRepoDeterminProjectsMessage);

				for (RepositoryNode node : getSelectedNodes(event)) {
					if (node.getRepository().isBare())
						continue;
					File workDir = node.getRepository().getWorkDir();
					final IPath wdPath = new Path(workDir.getAbsolutePath());
					for (IProject prj : ResourcesPlugin.getWorkspace()
							.getRoot().getProjects()) {
						if (monitor.isCanceled())
							return Status.OK_STATUS;
						if (wdPath.isPrefixOf(prj.getLocation())) {
							projectsToDelete.add(prj);
						}
					}
				}

				final boolean[] confirmedCanceled = new boolean[] { false,
						false };

				if (!projectsToDelete.isEmpty()) {
					Display.getDefault().syncExec(new Runnable() {

						public void run() {
							try {
								confirmedCanceled[0] = confirmProjectDeletion(
										projectsToDelete, event);
							} catch (OperationCanceledException e) {
								confirmedCanceled[1] = true;
							}
						}
					});
				}
				if (confirmedCanceled[1]) {
					// canceled: return
					return Status.OK_STATUS;
				}
				if (confirmedCanceled[0]) {
					// confirmed deletion
					IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

						public void run(IProgressMonitor actMonitor)
								throws CoreException {

							for (IProject prj : projectsToDelete)
								prj.delete(false, false, actMonitor);
						}
					};

					try {
						ResourcesPlugin.getWorkspace().run(wsr,
								ResourcesPlugin.getWorkspace().getRoot(),
								IWorkspace.AVOID_UPDATE, monitor);
					} catch (CoreException e1) {
						Activator.logError(e1.getMessage(), e1);
					}
				}
				for (RepositoryNode node : getSelectedNodes(event)) {
					util.removeDir(node.getRepository().getDirectory());
				}
				return Status.OK_STATUS;
			}
		};

		service.schedule(job);

		return null;
	}

	@SuppressWarnings("boxing")
	private boolean confirmProjectDeletion(List<IProject> projectsToDelete,
			ExecutionEvent event) throws OperationCanceledException {

		String message = NLS.bind(
				UIText.RepositoriesView_ConfirmProjectDeletion_Question,
				projectsToDelete.size());
		MessageDialog dlg = new MessageDialog(getView(event).getSite()
				.getShell(),
				UIText.RepositoriesView_ConfirmProjectDeletion_WindowTitle,
				null, message, MessageDialog.INFORMATION, new String[] {
						IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
						IDialogConstants.CANCEL_LABEL }, 0);
		int index = dlg.open();
		if (index == 2)
			throw new OperationCanceledException();
		return index == 0;
	}
}
