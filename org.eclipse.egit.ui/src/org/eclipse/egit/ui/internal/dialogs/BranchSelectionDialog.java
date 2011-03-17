/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import static org.eclipse.ui.ISources.ACTIVE_CURRENT_SELECTION_NAME;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * The branch and reset selection dialog
 */
public class BranchSelectionDialog extends AbstractBranchSelectionDialog {

	private Button deleteteButton;

	private Button renameButton;

	private Button newButton;

	/**
	 * Construct a dialog to select a branch to reset to or check out
	 *
	 * @param parentShell
	 * @param repo
	 */
	public BranchSelectionDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo);
		setRootsToShow(true, true, true, false);
	}

	private InputDialog getRefNameInputDialog(String prompt,
			final String refPrefix, String initialValue) {
		InputDialog labelDialog = new InputDialog(getShell(),
				UIText.BranchSelectionDialog_QuestionNewBranchTitle, prompt,
				initialValue, ValidationUtils.getRefNameInputValidator(repo,
						refPrefix, true));
		labelDialog.setBlockOnOpen(true);
		return labelDialog;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		newButton = new Button(parent, SWT.PUSH);
		newButton.setFont(JFaceResources.getDialogFont());
		newButton.setText(UIText.BranchSelectionDialog_NewBranch);
		setButtonLayoutData(newButton);
		((GridLayout) parent.getLayout()).numColumns++;

		renameButton = new Button(parent, SWT.PUSH);
		renameButton.setFont(JFaceResources.getDialogFont());
		renameButton.setText(UIText.BranchSelectionDialog_Rename);
		setButtonLayoutData(renameButton);
		((GridLayout) parent.getLayout()).numColumns++;

		deleteteButton = new Button(parent, SWT.PUSH);
		deleteteButton.setFont(JFaceResources.getDialogFont());
		deleteteButton.setText(UIText.BranchSelectionDialog_Delete);
		setButtonLayoutData(deleteteButton);
		((GridLayout) parent.getLayout()).numColumns++;

		renameButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				String refName = refNameFromDialog();
				String refPrefix;

				if (refName.startsWith(Constants.R_HEADS))
					refPrefix = Constants.R_HEADS;
				else if (refName.startsWith(Constants.R_REMOTES))
					refPrefix = Constants.R_REMOTES;
				else if (refName.startsWith(Constants.R_TAGS))
					refPrefix = Constants.R_TAGS;
				else {
					// the button should be disabled anyway, but we check again
					return;
				}

				String branchName = refName.substring(refPrefix.length());

				InputDialog labelDialog = getRefNameInputDialog(
						NLS
								.bind(
										UIText.BranchSelectionDialog_QuestionNewBranchNameMessage,
										branchName, refPrefix), refPrefix,
						branchName);
				if (labelDialog.open() == Window.OK) {
					String newRefName = refPrefix + labelDialog.getValue();
					try {
						new Git(repo).branchRename().setOldName(refName)
								.setNewName(labelDialog.getValue()).call();
						branchTree.refresh();
						markRef(newRefName);
					} catch (Throwable e1) {
						reportError(
								e1,
								UIText.BranchSelectionDialog_ErrorCouldNotRenameRef,
								refName, newRefName, e1.getMessage());
					}
				}
			}
		});
		newButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				CreateBranchWizard wiz = new CreateBranchWizard(repo,
						refFromDialog());
				if (new WizardDialog(getShell(), wiz).open() == Window.OK) {
					String newRefName = wiz.getNewBranchName();
					try {
						branchTree.refresh();
						markRef(Constants.R_HEADS + newRefName);
						if (repo.getBranch().equals(newRefName))
							// close branch selection dialog when new branch was
							// already checked out from new branch wizard
							BranchSelectionDialog.this.okPressed();
					} catch (Throwable e1) {
						reportError(
								e1,
								UIText.BranchSelectionDialog_ErrorCouldNotCreateNewRef,
								newRefName);
					}
				}
			}
		});

		deleteteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow activeWorkbenchWindow = workbench
						.getActiveWorkbenchWindow();
				IHandlerService hsr = (IHandlerService) activeWorkbenchWindow
						.getService(IHandlerService.class);
				// set selection in context
				IEvaluationContext ctx = hsr.getCurrentState();
				ctx.addVariable(ACTIVE_CURRENT_SELECTION_NAME,
						branchTree.getSelection());

				ICommandService commandService = (ICommandService) activeWorkbenchWindow
						.getService(ICommandService.class);
				Command deleteCommand = commandService
						.getCommand("org.eclipse.egit.ui.RepositoriesViewDeleteBranch"); //$NON-NLS-1$

				deleteCommand.addExecutionListener(new IExecutionListener() {
					public void preExecute(String commandId,
							ExecutionEvent event) {	/* do nothing */ }

					public void postExecuteSuccess(String commandId,
							Object returnValue) {
						branchTree.refresh();
					}

					public void postExecuteFailure(String commandId,
							ExecutionException exception) { /* do nothing */  }

					public void notHandled(String commandId,
							NotHandledException exception) { /* do nothing */ }
				});

				// launch deleteCommand
				ExecutionEvent executionEvent = hsr.createExecutionEvent(
						deleteCommand, null);
				try {
					deleteCommand.executeWithChecks(executionEvent);
				} catch (Throwable e) {
					reportError(
							e,
							UIText.BranchSelectionDialog_ErrorCouldNotDeleteRef,
							refNameFromDialog());
				}
			}
		});

		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText(UIText.BranchSelectionDialog_OkCheckout);

		// can't advance without a selection
		getButton(Window.OK).setEnabled(!branchTree.getSelection().isEmpty());
	}

	/**
	 * @return the message shown above the refs tree
	 */
	protected String getMessageText() {
		return UIText.BranchSelectionDialog_Refs;
	}

	/**
	 * Subclasses may add UI elements
	 *
	 * @param parent
	 */
	protected void createCustomArea(Composite parent) {
		// do nothing
	}

	/**
	 * Subclasses may change the title of the dialog
	 *
	 * @return the title of the dialog
	 */
	protected String getTitle() {
		return NLS.bind(UIText.BranchSelectionDialog_TitleCheckout,
				new Object[] { repo.getDirectory() });
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	private void reportError(Throwable e, String message, Object... args) {
		String msg = NLS.bind(message, args);
		Activator.handleError(msg, e, true);
	}

	@Override
	protected void refNameSelected(String refName) {
		boolean tagSelected = refName != null
				&& refName.startsWith(Constants.R_TAGS);

		boolean branchSelected = refName != null
				&& (refName.startsWith(Constants.R_HEADS) || refName
						.startsWith(Constants.R_REMOTES));

		getButton(Window.OK).setEnabled(branchSelected || tagSelected);
		newButton.setEnabled(branchSelected || tagSelected);

		// we don't support rename on tags
		renameButton.setEnabled(branchSelected && !tagSelected);
		deleteteButton.setEnabled(branchSelected && !tagSelected);
	}
}
