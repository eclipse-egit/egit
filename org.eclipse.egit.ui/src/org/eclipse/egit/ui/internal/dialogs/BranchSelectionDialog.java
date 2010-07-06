/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * The branch and reset selection dialog
 */
public class BranchSelectionDialog extends AbstractBranchSelectionDialog {

	private Button renameButton;

	private Button newButton;

	/**
	 * Construct a dialog to select a branch to reset to or check out
	 *
	 * @param parentShell
	 * @param repo
	 */
	public BranchSelectionDialog(Shell parentShell, FileRepository repo) {
		super(parentShell, repo);
	}

	private InputDialog getRefNameInputDialog(String prompt,
			final String refPrefix) {
		InputDialog labelDialog = new InputDialog(getShell(),
				UIText.BranchSelectionDialog_QuestionNewBranchTitle, prompt,
				null, ValidationUtils.getRefNameInputValidator(repo, refPrefix));
		labelDialog.setBlockOnOpen(true);
		return labelDialog;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		newButton = new Button(parent, SWT.PUSH);
		newButton.setFont(JFaceResources.getDialogFont());
		newButton.setText(UIText.BranchSelectionDialog_NewBranch);
		setButtonLayoutData(newButton);
		((GridLayout) parent.getLayout()).numColumns++;

		renameButton = new Button(parent, SWT.PUSH);
		renameButton.setFont(JFaceResources.getDialogFont());
		renameButton.setText(UIText.BranchSelectionDialog_Rename);
		setButtonLayoutData(renameButton);
		((GridLayout) parent.getLayout()).numColumns++;

		renameButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				String refName = refNameFromDialog();
				String refPrefix;

				// the button should be disabled anyway, but we check again
				if (refName.equals(Constants.HEAD))
					return;

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

				InputDialog labelDialog = getRefNameInputDialog(
						NLS
								.bind(
										UIText.BranchSelectionDialog_QuestionNewBranchNameMessage,
										branchName, refPrefix), refPrefix);
				if (labelDialog.open() == Window.OK) {
					String newRefName = refPrefix + labelDialog.getValue();
					try {
						RefRename renameRef = repo.renameRef(refName,
								newRefName);
						if (renameRef.rename() != Result.RENAMED) {
							reportError(
									null,
									UIText.BranchSelectionDialog_ErrorCouldNotRenameRef,
									refName, newRefName, renameRef.getResult());
						}
						branchTree.refresh();
						markRef(newRefName);
					} catch (Throwable e1) {
						reportError(
								e1,
								UIText.BranchSelectionDialog_ErrorCouldNotRenameRef,
								refName, newRefName, e1.getMessage());
					}
				}
			}
		});
		newButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				// check what ref name the user selected, if any.
				String refName = refNameFromDialog();

				// the button should be disabled anyway, but we check again
				if (refName.equals(Constants.HEAD))
					return;
				if (refName.startsWith(Constants.R_TAGS))
					// the button should be disabled anyway, but we check again
					return;

				InputDialog labelDialog = getRefNameInputDialog(NLS.bind(
						UIText.BranchSelectionDialog_QuestionNewBranchMessage,
						refName, Constants.R_HEADS), Constants.R_HEADS);

				if (labelDialog.open() == Window.OK) {
					String newRefName = Constants.R_HEADS
							+ labelDialog.getValue();
					RefUpdate updateRef;
					try {
						updateRef = repo.updateRef(newRefName);
						Ref startRef = repo.getRef(refName);
						ObjectId startAt = repo.resolve(refName);
						String startBranch;
						if (startRef != null)
							startBranch = refName;
						else
							startBranch = startAt.name();
						startBranch = repo.shortenRefName(startBranch);
						updateRef.setNewObjectId(startAt);
						updateRef.setRefLogMessage(
								"branch: Created from " + startBranch, false); //$NON-NLS-1$
						updateRef.update();
						branchTree.refresh();
						markRef(newRefName);
					} catch (Throwable e1) {
						reportError(
								e1,
								UIText.BranchSelectionDialog_ErrorCouldNotCreateNewRef,
								newRefName);
					}
				}
			}
		});

		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText(UIText.BranchSelectionDialog_OkCheckout);
		// createButton(parent, IDialogConstants.OK_ID,
		// UIText.BranchSelectionDialog_OkCheckout, true);
		// createButton(parent, IDialogConstants.CANCEL_ID,
		// IDialogConstants.CANCEL_LABEL, false);

		// can't advance without a selection
		getButton(Window.OK).setEnabled(!branchTree.getSelection().isEmpty());
	}

	/**
	 * @return the message shown above the refs tree
	 */
	protected String getMessageText() {
		return UIText.BranchSelectionDialog_Refs;
	}

	/**
	 * Subclasses may add UI elements
	 *
	 * @param parent
	 */
	protected void createCustomArea(Composite parent) {
		// do nothing
	}

	/**
	 * Subclasses may change the title of the dialog
	 *
	 * @return the title of the dialog
	 */
	protected String getTitle() {
		return NLS.bind(UIText.BranchSelectionDialog_TitleCheckout,
				new Object[] { repo.getDirectory() });
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	private void reportError(Throwable e, String message, Object... args) {
		String msg = NLS.bind(message, args);
		Activator.handleError(msg, e, true);
	}

	@Override
	protected void refNameSelected(String refName) {
		boolean tagSelected = refName != null
				&& refName.startsWith(Constants.R_TAGS);

		boolean branchSelected = refName != null
				&& (refName.startsWith(Constants.R_HEADS) || refName
						.startsWith(Constants.R_REMOTES));

		getButton(Window.OK).setEnabled(branchSelected || tagSelected);

		// we don't support rename on tags
		if (renameButton != null) {
			renameButton.setEnabled(branchSelected && !tagSelected);
		}

		// new branch can not be based on a tag
		if (newButton != null) {
			newButton.setEnabled(branchSelected && !tagSelected);
		}
	}
}
