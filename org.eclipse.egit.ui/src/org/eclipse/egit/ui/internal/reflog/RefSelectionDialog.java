/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.reflog;

import java.text.MessageFormat;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.AbstractBranchSelectionDialog;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Select the ref to show reflog for
 */
public class RefSelectionDialog extends AbstractBranchSelectionDialog {

	/**
	 * @param parentShell
	 * @param repository
	 */
	public RefSelectionDialog(Shell parentShell, Repository repository) {
		super(parentShell, repository, SHOW_LOCAL_BRANCHES
				| SHOW_REMOTE_BRANCHES | EXPAND_LOCAL_BRANCHES_NODE
				| EXPAND_REMOTE_BRANCHES_NODE | SHOW_REFERENCES);
	}

	@Override
	protected void refNameSelected(String refName) {
		getButton(Window.OK).setEnabled(refName != null);
	}

	@Override
	protected String getTitle() {
		String repoName;
		if (!repo.isBare())
			repoName = repo.getDirectory().getParentFile().getName();
		else
			repoName = repo.getDirectory().getName();
		return MessageFormat.format(UIText.RefSelectionDialog_Title, repoName);
	}

	@Override
	protected void createCustomArea(Composite parent) {
		branchTree.addFilter(new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (element instanceof AdditionalRefNode) {
					AdditionalRefNode ref = (AdditionalRefNode) element;
					return Constants.HEAD.equals(ref.getObject().getName());
				}
				return true;
			}
		});
	}

	@Override
	protected String getMessageText() {
		return UIText.RefSelectionDialog_Messsage;
	}
}
