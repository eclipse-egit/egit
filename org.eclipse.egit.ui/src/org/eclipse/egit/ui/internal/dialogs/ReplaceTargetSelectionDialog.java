/*******************************************************************************
 * Copyright (c) 2011, 2012 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Benjamin Muskalla (Tasktop Technologies) - initial implementation
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
public class ReplaceTargetSelectionDialog extends AbstractBranchSelectionDialog {
	private final String resourceName;

	/**
	 * @param parentShell
	 * @param repo
	 * @param resourceName
	 */
	public ReplaceTargetSelectionDialog(Shell parentShell, Repository repo,
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
				UIText.ReplaceTargetSelectionDialog_ReplaceButton);
	}

	@Override
	protected String getMessageText() {
		return UIText.ReplaceTargetSelectionDialog_ReplaceMessage;
	}

	@Override
	protected String getTitle() {
		if (resourceName != null && resourceName.length() > 0)
			return NLS.bind(UIText.ReplaceTargetSelectionDialog_ReplaceTitle,
					resourceName);
		else
			return UIText.ReplaceTargetSelectionDialog_ReplaceTitleEmptyPath;
	}

	@Override
	protected String getWindowTitle() {
		return UIText.ReplaceTargetSelectionDialog_ReplaceWindowTitle;
	}

	@Override
	protected void refNameSelected(String refName) {
		setOkButtonEnabled(refName != null);
	}
}
