/******************************************************************************
 *  Copyright (c) 2012, 2013 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Stefan Lay (SAP AG)
 *****************************************************************************/
package org.eclipse.egit.ui.internal.stash;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.StashCreateOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
* The UI wrapper for {@link StashCreateOperation} */
public class StashCreateUI {

	private Repository repo;

	/**
	 * @param repo
	 */
	public StashCreateUI(Repository repo) {
		this.repo = repo;
	}

	/**
	 * @param shell
	 *            the shell to use for showing the message input dialog
	 * @return true if a stash create operation was triggered
	 */
	public boolean createStash(Shell shell) {
		if (!UIUtils.saveAllEditors(repo))
			return false;
		StashCreateDialog commitMessageDialog = new StashCreateDialog(shell);
		if (commitMessageDialog.open() != Window.OK)
			return false;
		String message = commitMessageDialog.getValue();
		if (message.length() == 0)
			message = null;

		boolean includeUntracked = commitMessageDialog.getIncludeUntracked();

		final StashCreateOperation op = new StashCreateOperation(repo, message,
				includeUntracked);
		Job job = new WorkspaceJob(UIText.StashCreateCommand_jobTitle) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
					RevCommit commit = op.getCommit();
					if (commit == null) {
						showNoChangesToStash();
					}
				} catch (CoreException e) {
					Activator
							.logError(UIText.StashCreateCommand_stashFailed, e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.STASH.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return true;

	}

	private static void showNoChangesToStash() {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				MessageDialog.openInformation(shell,
						UIText.StashCreateCommand_titleNoChanges,
						UIText.StashCreateCommand_messageNoChanges);
			}
		});
	}

	private static class StashCreateDialog extends Dialog {

		/**
		 * Commit message widget.
		 */
		private Text text;

		/**
		 * Include untracked checkbox.
		 */
		private Button untrackedButton;

		/**
		 * The input value; the empty string by default.
		 */
		private String commitMessage = ""; //$NON-NLS-1$

		private boolean includeUntracked;

		public StashCreateDialog(Shell shell) {
			super(shell);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);

			getShell().setText(
					UIText.StashCreateCommand_titleEnterCommitMessage);

			Label label = new Label(composite, SWT.WRAP);
			label.setText(UIText.StashCreateCommand_messageEnterCommitMessage);
			GridData data = new GridData(GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_CENTER);
			data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
			label.setLayoutData(data);
			label.setFont(parent.getFont());

			text = new Text(composite, SWT.SINGLE | SWT.BORDER);
			text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
					| GridData.HORIZONTAL_ALIGN_FILL));

			untrackedButton = new Button(composite, SWT.CHECK);
			untrackedButton
					.setText(UIText.StashCreateCommand_includeUntrackedLabel);

			text.setFocus();
			return composite;
		}

		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId == IDialogConstants.OK_ID) {
				commitMessage = text.getText();
				includeUntracked = untrackedButton.getSelection();
			} else {
				commitMessage = null;
				includeUntracked = false;
			}
			super.buttonPressed(buttonId);
		}

		public String getValue() {
			return commitMessage;
		}

		public boolean getIncludeUntracked() {
			return includeUntracked;
		}


	}

}
