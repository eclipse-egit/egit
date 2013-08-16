/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchCommandConstants;

/**
 * Dialog for selecting and editing a branch.
 */
public class BranchSelectionAndEditDialog extends
		AbstractBranchSelectionDialog {

	private String currentBranch;

	private Button deleteButton;

	private Button renameButton;

	private Button newButton;

	/**
	 * @param parentShell
	 * @param repo
	 * @param refToMark
	 * @param settings
	 */
	public BranchSelectionAndEditDialog(Shell parentShell, Repository repo,
			String refToMark,
			int settings) {
		super(parentShell, repo, refToMark, settings);
		try {
			currentBranch = repo.getFullBranch();
		} catch (IOException e) {
			currentBranch = null;
		}
	}

	BranchSelectionAndEditDialog(Shell parentShell, Repository repo,
			int settings) {
		// no initial selection
		this(parentShell, repo, null, settings);
	}

	@Override
	protected String getMessageText() {
		return UIText.BranchSelectionAndEditDialog_Message;
	}

	@Override
	protected String getTitle() {
		return UIText.BranchSelectionAndEditDialog_Title;
	}

	@Override
	protected String getWindowTitle() {
		return UIText.BranchSelectionAndEditDialog_WindowTitle;
	}

	@Override
	protected void refNameSelected(String refName) {
		boolean tagSelected = refName != null
				&& refName.startsWith(Constants.R_TAGS);

		boolean branchSelected = refName != null
				&& (refName.startsWith(Constants.R_HEADS) || refName
						.startsWith(Constants.R_REMOTES));

		// handle multiple selection
		if (((TreeSelection) branchTree.getSelection()).size() > 1) {
			TreeSelection selection = (TreeSelection) branchTree
					.getSelection();
			boolean onlyBranchesAreSelected = onlyBranchesAreSelected(selection);

			// enable/disable buttons
			deleteButton.setEnabled(onlyBranchesAreSelected);
			renameButton.setEnabled(false);
			newButton.setEnabled(false);
		} else {
			getButton(Window.OK).setEnabled(branchSelected || tagSelected);

			// we don't support rename on tags
			renameButton.setEnabled(branchSelected && !tagSelected);
			deleteButton.setEnabled(branchSelected && !tagSelected);

			// new button should be always enabled
			newButton.setEnabled(true);
		}

		getButton(Window.OK).setEnabled(
				refName != null && !refName.equals(currentBranch));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		newButton = new Button(parent, SWT.PUSH);
		newButton.setFont(JFaceResources.getDialogFont());
		newButton.setText(UIText.BranchSelectionAndEditDialog_NewBranch);
		setButtonLayoutData(newButton);
		((GridLayout) parent.getLayout()).numColumns++;

		renameButton = new Button(parent, SWT.PUSH);
		renameButton.setFont(JFaceResources.getDialogFont());
		renameButton.setText(UIText.BranchSelectionAndEditDialog_Rename);
		setButtonLayoutData(renameButton);
		((GridLayout) parent.getLayout()).numColumns++;

		deleteButton = new Button(parent, SWT.PUSH);
		deleteButton.setFont(JFaceResources.getDialogFont());
		deleteButton.setText(UIText.BranchSelectionAndEditDialog_Delete);
		setButtonLayoutData(deleteButton);
		((GridLayout) parent.getLayout()).numColumns++;

		renameButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				String refName = refNameFromDialog();
				String refPrefix;

				if (refName.startsWith(Constants.R_HEADS))
					refPrefix = Constants.R_HEADS;
				else if (refName.startsWith(Constants.R_REMOTES))
					refPrefix = Constants.R_REMOTES;
				else if (refName.startsWith(Constants.R_TAGS))
					refPrefix = Constants.R_TAGS;
				else {
					// the button should be disabled anyway, but we check again
					return;
				}

				String branchName = refName.substring(refPrefix.length());

				InputDialog labelDialog = getRefNameInputDialog(NLS.bind(
								UIText.BranchSelectionAndEditDialog_QuestionNewBranchNameMessage,
						branchName, refPrefix), refPrefix, branchName);
				if (labelDialog.open() == Window.OK) {
					String newRefName = refPrefix + labelDialog.getValue();
					try {
						new Git(repo).branchRename().setOldName(refName)
								.setNewName(labelDialog.getValue()).call();
						branchTree.refresh();
						markRef(newRefName);
					} catch (Throwable e1) {
						reportError(e1,
								UIText.BranchSelectionAndEditDialog_ErrorCouldNotRenameRef,
								refName, newRefName, e1.getMessage());
					}
				}
			}
		});
		newButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				CreateBranchWizard wiz = new CreateBranchWizard(repo,
						refNameFromDialog());
				if (new WizardDialog(getShell(), wiz).open() == Window.OK) {
					String newRefName = wiz.getNewBranchName();
					try {
						branchTree.refresh();
						markRef(Constants.R_HEADS + newRefName);
						if (repo.getBranch().equals(newRefName))
							// close branch selection dialog when new branch was
							// already checked out from new branch wizard
							BranchSelectionAndEditDialog.this
									.okPressed();
					} catch (Throwable e1) {
						reportError(
								e1,
								UIText.BranchSelectionAndEditDialog_ErrorCouldNotCreateNewRef,
								newRefName);
					}
				}
			}
		});

		deleteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				try {
					CommonUtils.runCommand(
							IWorkbenchCommandConstants.EDIT_DELETE,
							(IStructuredSelection) branchTree.getSelection());
					branchTree.refresh();
				} catch (Throwable e) {
					reportError(e,
							UIText.BranchSelectionAndEditDialog_ErrorCouldNotDeleteRef,
							refNameFromDialog());
				}
			}
		});

		super.createButtonsForButtonBar(parent);

		// can't advance without a selection
		getButton(Window.OK).setEnabled(!branchTree.getSelection().isEmpty());
	}

	private InputDialog getRefNameInputDialog(String prompt,
			final String refPrefix, String initialValue) {
		InputDialog labelDialog = new InputDialog(getShell(),
				UIText.BranchSelectionAndEditDialog_QuestionNewBranchTitle,
				prompt,
				initialValue, ValidationUtils.getRefNameInputValidator(repo,
						refPrefix, true));
		labelDialog.setBlockOnOpen(true);
		return labelDialog;
	}

	private void reportError(Throwable e, String message, Object... args) {
		String msg = NLS.bind(message, args);
		Activator.handleError(msg, e, true);
	}

	private boolean onlyBranchesAreSelected(TreeSelection selection) {
		Iterator selIterator = selection.iterator();
		while (selIterator.hasNext()) {
			Object sel = selIterator.next();
			if (sel instanceof RefNode) {
				RefNode node = (RefNode) sel;
				String refName = node.getObject().getName();
				if (!refName.startsWith(R_HEADS)
						&& !refName.startsWith(R_REMOTES))
					return false;
			} else
				return false;
		}
		return true;
	}

}
