/*******************************************************************************
 * Copyright (c) 2012 Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    wim.jongman@remainsoftware.com - branch normalization
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.RenameBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.components.BranchNameNormalizer;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Allows to rename a single branch
 */
public class BranchRenameDialog extends TitleAreaDialog {
	private final Repository repository;

	private final Ref branchToRename;

	private Text name;

	private String newName;

	/**
	 * @param parentShell
	 * @param repository
	 * @param branchToRename
	 *            the branch; name must start with {@link Constants#R_HEADS} or
	 *            {@link Constants#R_REMOTES}
	 */
	public BranchRenameDialog(Shell parentShell, Repository repository,
			Ref branchToRename) {
		super(parentShell);
		this.repository = repository;
		this.branchToRename = branchToRename;
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5)
				.applyTo(main);
		new Label(main, SWT.NONE)
				.setText(UIText.BranchRenameDialog_NewNameLabel);
		name = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(name);
		return main;
	}

	@Override
	public void create() {
		super.create();
		setTitle(UIText.BranchRenameDialog_Title);
		String oldName = branchToRename.getName();
		String prefix;
		if (oldName.startsWith(Constants.R_HEADS))
			prefix = Constants.R_HEADS;
		else if (oldName.startsWith(Constants.R_REMOTES))
			prefix = Constants.R_REMOTES;
		else
			prefix = null;
		String shortName = null;
		if (prefix != null) {
			shortName = Repository.shortenRefName(branchToRename
					.getName());
			setMessage(NLS.bind(UIText.BranchRenameDialog_Message, shortName));
		} else
			setErrorMessage(NLS.bind(
					UIText.BranchRenameDialog_WrongPrefixErrorMessage, oldName));

		if (shortName != null) {
			name.setText(shortName);
			name.setSelection(0, shortName.length());
		}

		final IInputValidator inputValidator = ValidationUtils
				.getRefNameInputValidator(repository, prefix, true);
		name.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String error = inputValidator.isValid(name.getText());
				setErrorMessage(error);
				getButton(OK).setEnabled(error == null);
			}
		});

		BranchNameNormalizer normalizer = new BranchNameNormalizer(name);
		normalizer.setVisible(false);
		getButton(OK).setEnabled(false);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.BranchRenameDialog_WindowTitle);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK)
			try {
				newName = name.getText();
				new RenameBranchOperation(repository, branchToRename, newName)
						.execute(null);
			} catch (CoreException e) {
				Activator.handleError(
						UIText.BranchRenameDialog_RenameExceptionMessage, e,
						true);
				return;
			}
		super.buttonPressed(buttonId);
	}

	/**
	 * @return the name of the branch after the rename operation. This is only
	 *         valid after the dialog has been opened and finished with the OK
	 *         button.
	 */
	public String getNewName() {
		return newName;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.BranchRenameDialog_ButtonOK, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
}
