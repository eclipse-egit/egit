/*******************************************************************************
 * Copyright (C) 2016, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.dialogs;

import static org.eclipse.jface.dialogs.IDialogConstants.CANCEL_LABEL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Select Git Flow feature branches.
 */
public class FeatureBranchSelectionDialog extends MessageDialog {
	private List<Ref> selected = new ArrayList<>();
	private FilteredBranchesWidget filteredFeatures;
	private GitFlowRepository gfRepo;

	/**
	 * @param parentShell
	 * @param refs
	 * @param title
	 * @param message
	 * @param featurePrefix
	 * @param okButtonLabel
	 * @param gfRepo
	 */
	public FeatureBranchSelectionDialog(Shell parentShell,
			List<Ref> refs, String okButtonLabel, String title, String message,
			String featurePrefix, GitFlowRepository gfRepo) {
		super(parentShell, title, null, message, MessageDialog.QUESTION,
				new String[] { okButtonLabel, CANCEL_LABEL }, 0);
		this.gfRepo = gfRepo;
		filteredFeatures = new FilteredBranchesWidget(refs, featurePrefix, gfRepo);
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
				if (getButton(OK).isEnabled()) {
					buttonPressed(OK);
				}
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
		List<Ref> selection = filteredFeatures.getSelection();
		if (selection.isEmpty() || selection.get(0) == null) {
			getButton(OK).setEnabled(false);
			return;
		}
		Repository repository = gfRepo.getRepository();
		try {
			Ref currentBranch = repository.exactRef(repository.getFullBranch());
			getButton(OK).setEnabled(!selection.get(0).equals(currentBranch));
		} catch (IOException e) {
			Activator.logError("Unable to find current branch", e); //$NON-NLS-1$
		}
	}
}
