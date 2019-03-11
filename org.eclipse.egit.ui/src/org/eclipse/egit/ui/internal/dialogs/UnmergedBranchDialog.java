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
 *    Mathias Kinzler (SAP AG) - use the abstract super class
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Used to display unmerged branches for confirmation during delete.
 */
public class UnmergedBranchDialog extends MessageDialog {
	private final List<Ref> refs;

	/**
	 * @param parentShell
	 * @param refs
	 */
	public UnmergedBranchDialog(Shell parentShell, List<Ref> refs) {
		super(parentShell, UIText.UnmergedBranchDialog_Title, null,
				UIText.UnmergedBranchDialog_Message, MessageDialog.QUESTION,
				new String[] { UIText.UnmergedBranchDialog_deleteButtonLabel,
						IDialogConstants.CANCEL_LABEL }, 0);
		this.refs = new ArrayList<>(refs);
		Collections.sort(this.refs, CommonUtils.REF_ASCENDING_COMPARATOR);
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new FillLayout());

		TableViewer branchesList = new TableViewer(area);
		branchesList.setContentProvider(ArrayContentProvider.getInstance());
		branchesList.setLabelProvider(new GitLabelProvider());
		branchesList.setInput(refs);

		// restrict height to 20 items
		GridData layoutData = new GridData(GridData.FILL_BOTH);
		layoutData.heightHint = Math.min(20, refs.size() + 1)
				* branchesList.getTable().getItemHeight();
		area.setLayoutData(layoutData);

		return area;
	}

}
