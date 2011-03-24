/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * The branch and reset selection dialog
 */
public class RenameBranchDialog extends AbstractBranchSelectionDialog {
	/**
	 * Construct a dialog to select a branch to reset to or check out
	 *
	 * @param parentShell
	 * @param repo
	 */
	public RenameBranchDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo);
		setRootsToShow(true, true, false, false);
	}

	private InputDialog getRefNameInputDialog(String prompt,
			final String refPrefix, String initialValue) {
		InputDialog labelDialog = new InputDialog(getShell(),
				"New branch", prompt, //$NON-NLS-1$
				initialValue, ValidationUtils.getRefNameInputValidator(repo,
						refPrefix, true));
		labelDialog.setBlockOnOpen(true);
		return labelDialog;
	}

	@Override
	protected void okPressed() {
		// TODO Auto-generated method stub
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

		InputDialog labelDialog = getRefNameInputDialog(
				NLS
						.bind(
								"Enter new name of the {0} branch. {1} will be prepended to the name you type", //$NON-NLS-1$
								branchName, refPrefix), refPrefix,
				branchName);
		if (labelDialog.open() == Window.OK) {
			String newRefName = refPrefix + labelDialog.getValue();
			try {
				new Git(repo).branchRename().setOldName(refName)
						.setNewName(labelDialog.getValue()).call();
				branchTree.refresh();
				markRef(newRefName);
			} catch (Throwable e1) {
				reportError(
						e1,
						"Failed to rename branch {0} -> {1}, status={2}", //$NON-NLS-1$
						refName, newRefName, e1.getMessage());
			}
		}
		super.okPressed();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText("&Rename"); //$NON-NLS-1$

		// can't advance without a selection
		getButton(Window.OK).setEnabled(!branchTree.getSelection().isEmpty());
	}

	/**
	 * @return the message shown above the refs tree
	 */
	protected String getMessageText() {
		return "Select a branch to rename"; //$NON-NLS-1$
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
		return "Rename a Branch"; //$NON-NLS-1$
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
	}
}
