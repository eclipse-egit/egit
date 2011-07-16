/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robin Rosenberg - Refactoring from CheckoutCommand
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.util.List;

import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

abstract class AmbiguousBranchDialog extends MessageDialog {

	private final List<RefNode> nodes;

	private TableViewer branchesList;

	private RefNode selected;

	AmbiguousBranchDialog(Shell parentShell, List<RefNode> nodes, String title, String message) {
		super(parentShell, title, null,
				message,
				MessageDialog.QUESTION, new String[] {
						IDialogConstants.OK_LABEL,
						IDialogConstants.CANCEL_LABEL }, 0);
		this.nodes = nodes;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayoutData(new GridData(GridData.FILL_BOTH));
		area.setLayout(new FillLayout());

		branchesList = new TableViewer(area, SWT.SINGLE | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		branchesList.setContentProvider(ArrayContentProvider.getInstance());
		branchesList.setLabelProvider(new GitLabelProvider());
		branchesList.setInput(nodes);
		branchesList
				.addSelectionChangedListener(new ISelectionChangedListener() {

					public void selectionChanged(SelectionChangedEvent event) {
						getButton(OK).setEnabled(
								!event.getSelection().isEmpty());
					}
				});
		return area;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK)
			selected = (RefNode) ((IStructuredSelection) branchesList
					.getSelection()).getFirstElement();
		super.buttonPressed(buttonId);
	}

	@Override
	public void create() {
		super.create();
		getButton(OK).setEnabled(false);
	}

	public RefNode getSelectedNode() {
		return selected;
	}
}