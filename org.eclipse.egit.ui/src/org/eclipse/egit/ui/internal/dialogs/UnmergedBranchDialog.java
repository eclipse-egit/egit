/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - use the abstract super class
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
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
 * Used to display unmerged branches for confirmation during delete
 *
 * @param <T>
 *            either {@link Ref} or {@link IAdaptable} to {@link Ref}
 */
public class UnmergedBranchDialog<T> extends MessageDialog {
	private final List<T> nodes;

	/**
	 * @param parentShell
	 * @param nodes
	 */
	public UnmergedBranchDialog(Shell parentShell, List<T> nodes) {
		super(parentShell, UIText.UnmergedBranchDialog_Title, null,
				UIText.UnmergedBranchDialog_Message, MessageDialog.QUESTION,
				new String[] { UIText.UnmergedBranchDialog_deleteButtonLabel,
						IDialogConstants.CANCEL_LABEL }, 0);
		this.nodes = nodes;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayoutData(new GridData(GridData.FILL_BOTH));
		area.setLayout(new FillLayout());

		TableViewer branchesList = new TableViewer(area);
		branchesList.setContentProvider(ArrayContentProvider.getInstance());
		branchesList.setLabelProvider(new GitLabelProvider());
		branchesList.setInput(nodes);
		return area;
	}

}
