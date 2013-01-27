/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Asks whether the working directory of a (non-bare) Repository should also be
 * deleted
 */
public class DeleteRepositoryConfirmDialog extends TitleAreaDialog {
	private final Repository repository;

	private boolean shouldDelete = false;
	private boolean shouldRemoveProjects = false;
	private int numberOfProjects = 0;

	/**
	 * @param parentShell
	 * @param repository
	 * @param numberOfProjects
	 */
	public DeleteRepositoryConfirmDialog(Shell parentShell,
			Repository repository, int numberOfProjects) {
		super(parentShell);
		setHelpAvailable(false);
		this.repository = repository;
		this.numberOfProjects = numberOfProjects;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout(1, false));
		// if the repository is bare, we don't have a working directory to
		// delete; we should not use this dialog in this case, though
		// as it would be rendered ugly
		if (repository.isBare())
			return main;
		final Button deleteWorkDir = new Button(main, SWT.CHECK);
		final Button removeProjects = new Button(main, SWT.CHECK);
		deleteWorkDir.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shouldDelete = deleteWorkDir.getSelection();
				removeProjects.setEnabled(!shouldDelete);
				if (shouldDelete && numberOfProjects > 0) {
					removeProjects.setSelection(true);
					shouldRemoveProjects = true;
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(deleteWorkDir);
		deleteWorkDir
				.setText(NLS
						.bind(
								UIText.DeleteRepositoryConfirmDialog_DeleteWorkingDirectoryCheckbox,
								repository.getWorkTree().getPath()));
		if (numberOfProjects > 0) {
			removeProjects.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					shouldRemoveProjects = removeProjects.getSelection();
				}
			});
			GridDataFactory.fillDefaults().grab(true, false).applyTo(removeProjects);
			removeProjects
				.setText(NLS
						.bind(UIText.DeleteRepositoryConfirmDialog_DeleteProjectsCheckbox,
								Integer.valueOf(numberOfProjects)));
		} else
			removeProjects.setVisible(false);
		return main;
	}

	@Override
	public void create() {
		super.create();
		setTitle(NLS.bind(
				UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryTitle,
				repository.getDirectory().getPath()));
		setMessage(NLS.bind(
				UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryMessage,
				repository.getDirectory().getPath()));
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell
				.setText(UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryWindowTitle);
	}

	/**
	 * @return if the working directory should be deleted
	 */
	public boolean shouldDeleteWorkingDir() {
		return shouldDelete;
	}

	/**
	 * @return if the working directory should be deleted
	 */
	public boolean shouldRemoveProjects() {
		return shouldRemoveProjects;
	}
}
