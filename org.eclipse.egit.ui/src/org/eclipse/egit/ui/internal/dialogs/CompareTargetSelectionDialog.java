/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for selecting a compare target.
 *
 */
public class CompareTargetSelectionDialog extends AbstractBranchSelectionDialog {
	private final String pathString;

	/**
	 * @param parentShell
	 * @param repo
	 * @param pathString
	 */
	public CompareTargetSelectionDialog(Shell parentShell, Repository repo,
			String pathString) {
		super(parentShell, repo);
		this.pathString = pathString;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText(
				UIText.CompareTargetSelectionDialog_CompareButton);
	}

	@Override
	protected String getMessageText() {
		return UIText.CompareTargetSelectionDialog_CompareMessage;
	}

	@Override
	protected String getTitle() {
		return NLS.bind(UIText.CompareTargetSelectionDialog_CompareTitle,
				pathString);
	}

	@Override
	protected String getWindowTitle() {
		return UIText.CompareTargetSelectionDialog_WindowTitle;
	}

	@Override
	protected void refNameSelected(String refName) {
		getButton(Window.OK).setEnabled(refName != null);
	}
}
