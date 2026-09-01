/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.List;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Lists the untracked files of a submodule that a submodule update would
 * overwrite and asks whether to overwrite them.
 */
public class SubmoduleUpdateConflictDialog extends MessageDialog {

	private static final int OVERWRITE = IDialogConstants.CLIENT_ID + 1;

	private final Repository submodule;

	private final List<String> files;

	/**
	 * @param shell
	 *            parent shell
	 * @param submodule
	 *            the submodule repository that could not be updated
	 * @param message
	 *            text shown above the file list
	 * @param files
	 *            untracked files in the submodule that would be overwritten
	 */
	public SubmoduleUpdateConflictDialog(Shell shell, Repository submodule,
			String message, List<String> files) {
		super(shell, UIText.SubmoduleUpdateConflictDialog_Title, null, message,
				MessageDialog.WARNING, 1,
				UIText.SubmoduleUpdateConflictDialog_Overwrite,
				IDialogConstants.CANCEL_LABEL);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.submodule = submodule;
		this.files = files;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		new NonDeletedFilesTree(main, submodule, files);
		applyDialogFont(main);
		return main;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		setReturnCode(buttonId == 0 ? OVERWRITE : IDialogConstants.CANCEL_ID);
		close();
	}

	/**
	 * @return whether the user chose to overwrite the files
	 */
	public boolean shouldOverwrite() {
		return getReturnCode() == OVERWRITE;
	}
}
