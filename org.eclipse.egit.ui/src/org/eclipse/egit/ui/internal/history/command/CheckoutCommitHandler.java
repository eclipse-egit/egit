/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Check out of a commit.
 */
public class CheckoutCommitHandler extends AbstractHistoryCommanndHandler {
	private final class BranchMessageDialog extends MessageDialog {
		private final List<RefNode> nodes;

		TableViewer branchesList;

		RefNode selected;

		private BranchMessageDialog(Shell parentShell, List<RefNode> nodes) {
			super(parentShell, UIText.CheckoutHandler_SelectBranchTitle, null,
					UIText.CheckoutHandler_SelectBranchMessage,
					MessageDialog.QUESTION, new String[] {
							IDialogConstants.OK_LABEL,
							IDialogConstants.CANCEL_LABEL }, 0);
			this.nodes = nodes;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Composite area = new Composite(parent, SWT.NONE);
			area.setLayoutData(new GridData(GridData.FILL_BOTH));
			area.setLayout(new FillLayout());

			branchesList = new TableViewer(area, SWT.SINGLE | SWT.H_SCROLL
					| SWT.V_SCROLL | SWT.BORDER);
			branchesList.setContentProvider(ArrayContentProvider.getInstance());
			branchesList.setLabelProvider(new BranchLabelProvider());
			branchesList.setInput(nodes);
			branchesList
					.addSelectionChangedListener(new ISelectionChangedListener() {

						public void selectionChanged(SelectionChangedEvent event) {
							getButton(OK).setEnabled(
									!event.getSelection().isEmpty());
						}
					});
			return area;
		}

		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId == OK)
				selected = (RefNode) ((IStructuredSelection) branchesList
						.getSelection()).getFirstElement();
			super.buttonPressed(buttonId);
		}

		@Override
		public void create() {
			super.create();
			getButton(OK).setEnabled(false);
		}

		public RefNode getSelectedNode() {
			return selected;
		}
	}

	private final class BranchLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			RefNode refNode = (RefNode) element;
			return refNode.getObject().getName();
		}

		@Override
		public Image getImage(Object element) {
			return RepositoryTreeNodeType.REF.getIcon();
		}
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		PlotCommit commit = (PlotCommit) getSelection(event).getFirstElement();
		Repository repo = getRepository(event);
		List<Ref> availableBranches = new ArrayList<Ref>();

		final BranchOperation op;

		try {
			Map<String, Ref> localBranches = repo.getRefDatabase().getRefs(
					Constants.R_HEADS);
			for (Ref branch : localBranches.values()) {
				if (branch.getLeaf().getObjectId().equals(commit.getId())) {
					availableBranches.add(branch);
				}
			}
		} catch (IOException e) {
			// ignore here
		}

		if (availableBranches.isEmpty())
			op = new BranchOperation(repo, commit.getId());
		else if (availableBranches.size() == 1)
			op = new BranchOperation(repo, availableBranches.get(0).getName());
		else {
			List<RefNode> nodes = new ArrayList<RefNode>();
			RepositoryNode repoNode = new RepositoryNode(null, repo);
			for (Ref ref : availableBranches) {
				nodes.add(new RefNode(repoNode, repo, ref));
			}
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
