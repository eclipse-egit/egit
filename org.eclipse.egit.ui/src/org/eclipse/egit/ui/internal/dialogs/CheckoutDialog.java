/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
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

import java.text.MessageFormat;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for checking out a branch, tag, or Reference.
 *
 */
public class CheckoutDialog extends BranchSelectionAndEditDialog {

	/**
	 * @param parentShell
	 * @param repo
	 */
	public CheckoutDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo,
				SELECT_CURRENT_REF | SHOW_LOCAL_BRANCHES | SHOW_REMOTE_BRANCHES
				| SHOW_TAGS | SHOW_REFERENCES | EXPAND_LOCAL_BRANCHES_NODE
				| ALLOW_MULTISELECTION);
	}

	@Override
	protected String getTitle() {
		return MessageFormat.format(UIText.CheckoutDialog_Title, Activator
				.getDefault().getRepositoryUtil().getRepositoryName(repo));
	}

	@Override
	protected void refNameSelected(String refName) {
		super.refNameSelected(refName);

		if (BranchOperationUI.checkoutWillShowQuestionDialog(refName))
			getButton(Window.OK).setText(
					UIText.CheckoutDialog_OkCheckoutWithQuestion);
		else
			getButton(Window.OK).setText(UIText.CheckoutDialog_OkCheckout);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		getButton(Window.OK).setText(UIText.CheckoutDialog_OkCheckout);
	}

}
