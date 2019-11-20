/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
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

	/**
	 * Id of the New branch button.
	 */
	public static final int NEW = 100;

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
			String refToMark, int settings) {
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
		return MessageFormat.format(UIText.BranchSelectionAndEditDialog_Title,
				Activator.getDefault().getRepositoryUtil()
						.getRepositoryName(repo));
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
			deleteButton.setEnabled(onlyBranchesExcludingCurrentAreSelected(selection));
			renameButton.setEnabled(false);
			if (newButton != null)
				newButton.setEnabled(false);
			setOkButtonEnabled(false);
		} else {
			// we don't support rename on tags
			renameButton.setEnabled(branchSelected && !tagSelected);
			deleteButton.setEnabled(branchSelected && !tagSelected
					&& !isCurrentBranch(refName));
			// new button should be always enabled
			if (newButton != null)
				newButton.setEnabled(true);
			setOkButtonEnabled((branchSelected || tagSelected)
					&& !isCurrentBranch(refName));
		}

		Button okButton = getButton(Window.OK);
		if (okButton != null) {
			if (BranchOperationUI.checkoutWillShowQuestionDialog(refName))
				okButton.setText(UIText.CheckoutDialog_OkCheckoutWithQuestion);
			else
				okButton.setText(UIText.CheckoutDialog_OkCheckout);
		}
	}

	private boolean isCurrentBranch(String refName) {
		if (refName != null)
			return refName.equals(currentBranch);
		return false;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createNewButton(parent);
		createRenameButton(parent);
		createDeleteButton(parent);

		super.createButtonsForButtonBar(parent);

		// can't advance without a selection
		setOkButtonEnabled(!branchTree.getSelection().isEmpty());
	}

	/**
	 * Creates button for creating a new branch.
	 *
	 * @param parent
	 * @return button
	 */
	protected Button createNewButton(Composite parent) {
		newButton = createButton(parent, NEW,
				UIText.BranchSelectionAndEditDialog_NewBranch, false);
		setButtonLayoutData(newButton);
		newButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// try to read default source ref from git config
				// in the case that no base is selected in dialog
				String base = refNameFromDialog();
				if (base == null) {
					String sourceRef = repo.getConfig().getString(
							ConfigConstants.CONFIG_WORKFLOW_SECTION, null,
							ConfigConstants.CONFIG_KEY_DEFBRANCHSTARTPOINT);
					try {
						Ref ref = repo.findRef(sourceRef);
						if (ref != null) {
							base = ref.getName();
						}
					} catch (IOException e1) {
						// base = null;
					}
				}
				CreateBranchWizard wiz = new CreateBranchWizard(repo, base);
				if (new WizardDialog(getShell(), wiz).open() == Window.OK) {
					String newRefName = wiz.getNewBranchName();
					try {
						branchTree.refresh();
						markRef(Constants.R_HEADS + newRefName);
						if (newRefName.equals(repo.getBranch()))
							// close branch selection dialog when new branch was
							// already checked out from new branch wizard
							BranchSelectionAndEditDialog.this.okPressed();
					} catch (Throwable e1) {
						reportError(
								e1,
								UIText.BranchSelectionAndEditDialog_ErrorCouldNotCreateNewRef,
								newRefName);
					}
				}
			}
		});
		return newButton;
	}

	/**
	 * Creates button for renaming a branch.
	 *
	 * @param parent
	 * @return button
	 */
	protected Button createRenameButton(Composite parent) {
		((GridLayout) parent.getLayout()).numColumns++;
		renameButton = new Button(parent, SWT.PUSH);
		renameButton.setFont(JFaceResources.getDialogFont());
		renameButton.setText(UIText.BranchSelectionAndEditDialog_Rename);
		setButtonLayoutData(renameButton);
		renameButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Ref selectedRef = refFromDialog();

				String namePrefix = selectedRef.getName()
						.startsWith(Constants.R_REMOTES) ? Constants.R_REMOTES
								: Constants.R_HEADS;

				BranchRenameDialog dialog = new BranchRenameDialog(getShell(),
						repo, selectedRef);
				if (dialog.open() == Window.OK) {
					branchTree.refresh();
					markRef(namePrefix + dialog.getNewName());
				}
			}
		});
		renameButton.setEnabled(false);
		return renameButton;
	}

	/**
	 * Creates button for deleting a branch.
	 *
	 * @param parent
	 * @return button
	 */
	protected Button createDeleteButton(Composite parent) {
		((GridLayout) parent.getLayout()).numColumns++;
		deleteButton = new Button(parent, SWT.PUSH);
		deleteButton.setFont(JFaceResources.getDialogFont());
		deleteButton.setText(UIText.BranchSelectionAndEditDialog_Delete);
		setButtonLayoutData(deleteButton);

		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent selectionEvent) {
				try {
					CommonUtils.runCommand(
							IWorkbenchCommandConstants.EDIT_DELETE,
							(IStructuredSelection) branchTree.getSelection());
					branchTree.refresh();
				} catch (Throwable e) {
					reportError(
							e,
							UIText.BranchSelectionAndEditDialog_ErrorCouldNotDeleteRef,
							refNameFromDialog());
				}
			}
		});
		deleteButton.setEnabled(false);
		return deleteButton;
	}

	private void reportError(Throwable e, String message, Object... args) {
		String msg = NLS.bind(message, args);
		Activator.handleError(msg, e, true);
	}

	private boolean onlyBranchesExcludingCurrentAreSelected(
			TreeSelection selection) {
		Iterator selIterator = selection.iterator();
		while (selIterator.hasNext()) {
			Object sel = selIterator.next();
			if (sel instanceof RefNode) {
				RefNode node = (RefNode) sel;
				String refName = node.getObject().getName();
				if (!refName.startsWith(R_HEADS)
						&& !refName.startsWith(R_REMOTES))
					return false;
				if (isCurrentBranch(refName))
					return false;
			} else
				return false;
		}
		return true;
	}

}
