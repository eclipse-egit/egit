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
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Asks whether the working directory of a (non-bare) Repository should also be
 * deleted
 */
public class DeleteRepositoryConfirmDialog extends TitleAreaDialog {
	private final Repository repository;

	private boolean shouldDeleteGitDir = false;
	private boolean shouldDeleteWorkingDir = false;
	private boolean shouldRemoveProjects = false;

	private final Collection<IProject> projectsToDelete;

	private Button deleteGitDir;
	private Button deleteWorkDir;
	private Button removeProjects;

	private Text deleteWorkDirLabel;

	/**
	 * @param parentShell
	 * @param repository
	 *                             non-bare repository
	 * @param projectsToDelete
	 *                             list of projects to delete
	 */
	public DeleteRepositoryConfirmDialog(Shell parentShell,
			Repository repository, Collection<IProject> projectsToDelete) {
		super(parentShell);
		setHelpAvailable(false);
		if (repository.isBare())
			throw new IllegalArgumentException(
					"DeleteRepositoryConfirmDialog can only be used for non-bare repository."); //$NON-NLS-1$
		this.repository = repository;
		this.projectsToDelete = projectsToDelete;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout(1, false));

		deleteGitDir = new Button(main, SWT.CHECK);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(deleteGitDir);
		deleteGitDir
				.setText(UIText.DeleteRepositoryConfirmDialog_DeleteGitDirCheckbox);
		createIndentedLabel(main, repository.getDirectory().getPath());

		deleteWorkDir = new Button(main, SWT.CHECK);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(deleteWorkDir);
		deleteWorkDir
				.setText(UIText.DeleteRepositoryConfirmDialog_DeleteWorkingDirectoryCheckbox);
		deleteWorkDirLabel = createIndentedLabel(main, repository.getWorkTree()
				.getPath());

		removeProjects = new Button(main, SWT.CHECK);

		deleteGitDir.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shouldDeleteGitDir = deleteGitDir.getSelection();
				updateUI();
			}
		});

		deleteWorkDir.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shouldDeleteWorkingDir = deleteWorkDir.getSelection();
				updateUI();
			}
		});

		if (!projectsToDelete.isEmpty()) {
			removeProjects.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					shouldRemoveProjects = removeProjects.getSelection();
					updateUI();
				}
			});
			GridDataFactory.fillDefaults().grab(true, false).applyTo(removeProjects);
			removeProjects
					.setText(MessageFormat
							.format(UIText.DeleteRepositoryConfirmDialog_DeleteProjectsCheckbox,
									Integer.valueOf(projectsToDelete.size())));
			TableViewer projectsViewer = new TableViewer(main,
					SWT.BORDER | SWT.V_SCROLL);
			// same indentation as the indented labels
			GridDataFactory.fillDefaults().grab(true, true).indent(20, 0)
					.hint(SWT.DEFAULT, 100)
					.applyTo(projectsViewer.getControl());

			projectsViewer
					.setLabelProvider(new DecoratingStyledCellLabelProvider(
							new WorkbenchLabelProvider(),
							PlatformUI.getWorkbench().getDecoratorManager()
									.getLabelDecorator(),
							null));
			projectsViewer.setContentProvider(ArrayContentProvider.getInstance());
			projectsViewer.setInput(projectsToDelete);
		} else
			removeProjects.setVisible(false);
		deleteGitDir.setFocus();
		main.setTabList(
				new Control[] { deleteGitDir, deleteWorkDir, removeProjects });
		return main;
	}

	@Override
	public void create() {
		super.create();
		setTitle(NLS.bind(
				UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryTitle,
				Activator.getDefault().getRepositoryUtil()
						.getRepositoryName(repository)));
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell
				.setText(UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryWindowTitle);
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control result = super.createButtonBar(parent);
		updateUI();
		return result;
	}

	/**
	 * @return if the working directory should be deleted
	 */
	public boolean shouldDeleteWorkingDir() {
		return shouldDeleteWorkingDir;
	}

	/**
	 * @return if the working directory should be deleted
	 */
	public boolean shouldRemoveProjects() {
		return shouldRemoveProjects;
	}

	private static Text createIndentedLabel(Composite main, String text) {
		Text widget = UIUtils.createSelectableLabel(main, 0);
		widget.setText(text);
		// Eclipse 4.3: Use LayoutConstants.getIndent once we depend on 4.3
		int indent = 20;
		GridDataFactory.fillDefaults().grab(true, false).indent(indent, 0)
				.applyTo(widget);
		return widget;
	}

	private void updateUI() {
		// The user has to select the delete checkbox before OK can be clicked
		getButton(IDialogConstants.OK_ID).setEnabled(shouldDeleteGitDir);
		deleteWorkDir.setEnabled(shouldDeleteGitDir);
		deleteWorkDirLabel.setEnabled(shouldDeleteGitDir);
		removeProjects
				.setEnabled(shouldDeleteGitDir && !shouldDeleteWorkingDir);
		if (shouldDeleteWorkingDir && !projectsToDelete.isEmpty()) {
			removeProjects.setSelection(true);
			shouldRemoveProjects = true;
		}
		if (shouldDeleteGitDir)
			setMessage(
					UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryNoUndoWarning,
					IMessageProvider.WARNING);
		else
			setMessage(UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryConfirmMessage);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryConfirmButton,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
}
