/*******************************************************************************
 * Copyright (C) 2016, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.dialogs;

import static org.eclipse.jface.dialogs.IDialogConstants.CANCEL_LABEL;
import static org.eclipse.jface.dialogs.IDialogConstants.OK_LABEL;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Select Git Flow feature branches.
 */
public class FeatureBranchSelectionDialog extends MessageDialog {
	private List<Ref> selected = new ArrayList<>();
	private FilteredBranchesWidget filteredFeatures;

	/**
	 * @param parentShell
	 * @param nodes
	 * @param title
	 * @param message
	 * @param featurePrefix
	 */
	public FeatureBranchSelectionDialog(Shell parentShell,
			List<Ref> nodes, String title, String message,
			String featurePrefix) {
		super(parentShell, title, null, message, MessageDialog.QUESTION,
				new String[] { OK_LABEL, CANCEL_LABEL }, 0);
		filteredFeatures = new FilteredBranchesWidget(nodes, featurePrefix);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Control result = filteredFeatures.create(parent);

		TreeViewer viewer = filteredFeatures.getBranchesList();
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				checkPage();
			}
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				buttonPressed(OK);
			}
		});
		return result;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			selected = filteredFeatures.getSelection();
		}
		super.buttonPressed(buttonId);
	}

	@Override
	public void create() {
		super.create();
		getButton(OK).setEnabled(false);
	}

	/**
	 * @return the selected entry (single mode)
	 */
	public Ref getSelectedNode() {
		if (selected.isEmpty()) {
			return null;
		}
		return selected.get(0);
	}

	private void checkPage() {
		getButton(OK).setEnabled(!filteredFeatures.getSelection().isEmpty());
	}
}
