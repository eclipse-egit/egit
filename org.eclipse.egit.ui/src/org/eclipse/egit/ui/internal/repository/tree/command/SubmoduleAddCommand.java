/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.SubmoduleAddOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.submodule.AddSubmoduleWizard;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;

/**
 * Command to add a new submodule to a repository
 */
public class SubmoduleAddCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode<?>> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode<?>> nodes = getSelectedNodes(event);
		if (nodes.isEmpty())
			return null;

		final Repository repo = nodes.get(0).getRepository();
		if (repo == null)
			return null;

		AddSubmoduleWizard wizard = new AddSubmoduleWizard(repo);
		WizardDialog dialog = new WizardDialog(getShell(event), wizard);
		if (dialog.open() == Window.OK) {
			final String path = wizard.getPath();
			final String uri = wizard.getUri().toPrivateASCIIString();
			final SubmoduleAddOperation op = new SubmoduleAddOperation(repo,
					path, uri);
			Job job = new WorkspaceJob(MessageFormat.format(
					UIText.SubmoduleAddCommand_JobTitle, path, uri)) {

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					monitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
					try {
						op.execute(monitor);
					} catch (CoreException e) {
						Activator.logError(UIText.SubmoduleAddCommand_AddError,
								e);
					}
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					if (JobFamilies.SUBMODULE_ADD.equals(family))
						return true;
					return super.belongsTo(family);
				}
			};
			job.setUser(true);
			job.setRule(op.getSchedulingRule());
			job.schedule();
		}
		return null;
	}
}
