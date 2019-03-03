/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
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

import org.eclipse.egit.ui.internal.UIText;
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
	private final String resourceName;

	/**
	 * @param parentShell
	 * @param repo
	 * @param resourceName
	 */
	public CompareTargetSelectionDialog(Shell parentShell, Repository repo,
			String resourceName) {
		super(parentShell, repo, SHOW_LOCAL_BRANCHES | SHOW_REMOTE_BRANCHES
				| SHOW_TAGS | SHOW_REFERENCES | EXPAND_LOCAL_BRANCHES_NODE
				| SELECT_CURRENT_REF);
		this.resourceName = resourceName;
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
		if (resourceName != null && resourceName.length() > 0)
			return NLS.bind(UIText.CompareTargetSelectionDialog_CompareTitle,
					resourceName);
		else
			return UIText.CompareTargetSelectionDialog_CompareTitleEmptyPath;
	}

	@Override
	protected String getWindowTitle() {
		return UIText.CompareTargetSelectionDialog_WindowTitle;
	}

	@Override
	protected void refNameSelected(String refName) {
		setOkButtonEnabled(refName != null);
	}
}
