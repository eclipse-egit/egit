/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Check out of a commit.
 */
public class CheckoutCommitHandler extends AbstractHistoryCommanndHandler {
	private static final class BranchMessageDialog extends AmbiguosBranchDialog {

		public BranchMessageDialog(Shell parentShell, List<RefNode> nodes) {
			super(parentShell, nodes, UIText.CheckoutHandler_SelectBranchTitle, UIText.CheckoutHandler_SelectBranchMessage);
		}

	}
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = (RevCommit) getSelection(getPage()).getFirstElement();
		Repository repo = getRepository(event);

		final BranchOperation op;

		List<RefNode> nodes = getRefNodes(commit, repo, Constants.R_HEADS);

		if (nodes.isEmpty())
			op = new BranchOperation(repo, commit.getId());
		else if (nodes.size() == 1)
			op = new BranchOperation(repo, nodes.get(0).getObject().getName());
		else {
			BranchMessageDialog dlg = new BranchMessageDialog(HandlerUtil
					.getActiveShellChecked(event), nodes);
			if (dlg.open() == Window.OK) {
				op = new BranchOperation(repo, dlg.getSelectedNode()
						.getObject().getName());
			} else {
				op = null;
			}
		}

		if (op == null)
			return null;

		// for the sake of UI responsiveness, let's start a job
		Job job = new Job(NLS.bind(UIText.RepositoriesView_CheckingOutMessage,
				commit.getId().name())) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				IWorkspaceRunnable wsr = new IWorkspaceRunnable() {

					public void run(IProgressMonitor myMonitor)
							throws CoreException {
						op.execute(myMonitor);
					}
				};

				try {
					ResourcesPlugin.getWorkspace().run(wsr,
							ResourcesPlugin.getWorkspace().getRoot(),
							IWorkspace.AVOID_UPDATE, monitor);
				} catch (CoreException e1) {
					return Activator.createErrorStatus(e1.getMessage(), e1);
				}

				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.CHECKOUT))
					return true;
				return super.belongsTo(family);
			}
		};

		job.setUser(true);
		job.schedule();
		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		IStructuredSelection sel = getSelection(page);
		return sel.size() == 1 && sel.getFirstElement() instanceof RevCommit;
	}
}
