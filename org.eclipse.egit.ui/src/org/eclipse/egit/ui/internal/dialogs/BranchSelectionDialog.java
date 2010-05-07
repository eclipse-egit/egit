/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com.dewire.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.op.ResetOperation.ResetType;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNode.RepositoryTreeNodeType;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * The branch and reset selection dialog
 *
 */
public class BranchSelectionDialog extends Dialog {

	private final Repository repo;

	private final boolean showResetType;

	private TreeViewer branchTree;

	private Button confirmationBtn;

	private Button renameButton;

	private Button newButton;

	private String selectedBranch;

	private ResetType resetType = ResetType.MIXED;

	private final RepositoryTreeNode<Repository> localBranches;

	private final RepositoryTreeNode<Repository> remoteBranches;

	private final RepositoryTreeNode<Repository> tags;

	/**
	 * Construct a dialog to select a branch to reset to or check out
	 *
	 * @param parentShell
	 * @param repo
	 * @param showReset
	 *            true if the "reset" part should be shown
	 */
	public BranchSelectionDialog(Shell parentShell, Repository repo,
			boolean showReset) {
		super(parentShell);
		this.repo = repo;
		localBranches = new RepositoryTreeNode<Repository>(null,
				RepositoryTreeNodeType.LOCALBRANCHES, this.repo, this.repo);
		remoteBranches = new RepositoryTreeNode<Repository>(null,
				RepositoryTreeNodeType.REMOTEBRANCHES, this.repo, this.repo);
		tags = new RepositoryTreeNode<Repository>(null,
				RepositoryTreeNodeType.TAGS, this.repo, this.repo);
		this.showResetType = showReset;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Composite createDialogArea(Composite base) {
		Composite parent = (Composite) super.createDialogArea(base);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		GridLayoutFactory.swtDefaults().applyTo(parent);

		new Label(parent, SWT.NONE).setText(UIText.BranchSelectionDialog_Refs);
		branchTree = new TreeViewer(parent, SWT.SINGLE | SWT.BORDER);
		new RepositoriesViewLabelProvider(branchTree);
		branchTree.setContentProvider(new RepositoriesViewContentProvider());

		GridDataFactory.fillDefaults().grab(true, true).hint(500, 300).applyTo(
				branchTree.getTree());

		branchTree.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				// enable the buttons depending on the selection

				boolean branchSelected = isBranchSelected();

				// TODO add support for checkout of tags
				confirmationBtn.setEnabled(branchSelected);

				if (!showResetType) {
					// we don't support rename on tags
					renameButton.setEnabled(branchSelected);

					// new branch can not be based on a tag
					newButton.setEnabled(branchSelected);
				}
			}
		});

		// double-click support
		branchTree.addOpenListener(new IOpenListener() {

			public void open(OpenEvent event) {
				RepositoryTreeNode node = (RepositoryTreeNode) ((IStructuredSelection) branchTree
						.getSelection()).getFirstElement();
				if (node.getType() != RepositoryTreeNodeType.REF)
					branchTree.setExpandedState(node, !branchTree
							.getExpandedState(node));
				else if (isBranchSelected())
					okPressed();

			}
		});

		if (showResetType) {
			buildResetGroup(parent);
		}

		String rawTitle = showResetType ? UIText.BranchSelectionDialog_TitleReset
				: UIText.BranchSelectionDialog_TitleCheckout;
		getShell().setText(
				NLS.bind(rawTitle, new Object[] { repo.getDirectory() }));

		return parent;
	}

	@Override
	public void create() {
		super.create();

		List<RepositoryTreeNode> roots = new ArrayList<RepositoryTreeNode>();
		roots.add(localBranches);
		roots.add(remoteBranches);
		roots.add(tags);

		branchTree.setInput(roots);

		try {
			// initially, we mark the current head if it can be determined
			String fullBranch = repo.getFullBranch();
			if (!markRef(fullBranch))
				// if we can't determine a branch, we just expand local branches
				branchTree.expandToLevel(localBranches, 1);
		} catch (IOException e) {
			// ignore
		}
	}

	private boolean markRef(String refName) {
		// selects the entry specified by the name
		if (refName == null)
			return false;
		Ref actRef;
		try {
			actRef = repo.getRef(refName);
		} catch (IOException e) {
			// ignore
			return false;
		}

		if (actRef == null)
			return false;

		RepositoryTreeNode<Repository> parentNode;
		if (refName.startsWith(Constants.R_HEADS)) {
			parentNode = localBranches;
		} else if (refName.startsWith(Constants.R_REMOTES)) {
			parentNode = remoteBranches;
		} else if (refName.startsWith(Constants.R_TAGS)) {
			parentNode = tags;
		} else {
			return false;
		}

		RepositoryTreeNode<Ref> actNode = new RepositoryTreeNode<Ref>(
				parentNode, RepositoryTreeNodeType.REF, repo, actRef);
		branchTree.setSelection(new StructuredSelection(actNode), true);
		return true;
	}

	private void buildResetGroup(Composite parent) {
		Group g = new Group(parent, SWT.NONE);
		g.setText(UIText.BranchSelectionDialog_ResetType);
		g.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL)
				.create());
		g.setLayout(new RowLayout(SWT.VERTICAL));

		Button soft = new Button(g, SWT.RADIO);
		soft.setText(UIText.BranchSelectionDialog_ResetTypeSoft);
		soft.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				resetType = ResetType.SOFT;
			}
		});

		Button medium = new Button(g, SWT.RADIO);
		medium.setSelection(true);
		medium.setText(UIText.BranchSelectionDialog_ResetTypeMixed);
		medium.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				resetType = ResetType.MIXED;
			}
		});

		Button hard = new Button(g, SWT.RADIO);
		hard.setText(UIText.BranchSelectionDialog_ResetTypeHard);
		hard.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				resetType = ResetType.HARD;
			}
		});
	}

	/**
	 * @return the selected refName
	 */
	public String getRefName() {
		return this.selectedBranch;
	}

	/**
	 * @return Type of Reset
	 */
	public ResetType getResetType() {
		return resetType;
	}

	@Override
	protected void okPressed() {
		if (showResetType) {
			if (resetType == ResetType.HARD) {
				if (!MessageDialog.openQuestion(getShell(),
						UIText.BranchSelectionDialog_ReallyResetTitle,
						UIText.BranchSelectionDialog_ReallyResetMessage)) {
					return;
				}
			}
		}
		this.selectedBranch = refNameFromDialog();
		super.okPressed();
	}

	private String refNameFromDialog() {
		IStructuredSelection sel = (IStructuredSelection) branchTree
				.getSelection();
		if (sel.size() != 1)
			return null;
		RepositoryTreeNode node = (RepositoryTreeNode) sel.getFirstElement();
		if (node.getType() == RepositoryTreeNodeType.REF) {
			return ((Ref) node.getObject()).getName();
		}
		return null;
	}

	private InputDialog getRefNameInputDialog(String prompt,
			final String refPrefix) {
		InputDialog labelDialog = new InputDialog(getShell(),
				UIText.BranchSelectionDialog_QuestionNewBranchTitle, prompt,
				null, new IInputValidator() {
					public String isValid(String newText) {
						if (newText.length() == 0) {
							// nothing entered, just don't let the user proceed,
							// no need to prompt them with an error message
							return ""; //$NON-NLS-1$
						}

						String testFor = refPrefix + newText;
						try {
							if (repo.resolve(testFor) != null)
								return UIText.BranchSelectionDialog_ErrorAlreadyExists;
						} catch (IOException e1) {
							Activator
									.logError(
											NLS
													.bind(
															UIText.BranchSelectionDialog_ErrorCouldNotResolve,
															testFor), e1);
							return e1.getMessage();
						}
						if (!Repository.isValidRefName(testFor))
							return UIText.BranchSelectionDialog_ErrorInvalidRefName;
						return null;
					}
				});
		labelDialog.setBlockOnOpen(true);
		return labelDialog;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (!showResetType) {
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

			renameButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {

					String refName = refNameFromDialog();
					String refPrefix;

					// the button should be disabled anyway, but we check again
					if (refName.equals(Constants.HEAD))
						return;

					if (refName.startsWith(Constants.R_HEADS))
						refPrefix = Constants.R_HEADS;
					else if (refName.startsWith(Constants.R_REMOTES))
						refPrefix = Constants.R_REMOTES;
					else if (refName.startsWith(Constants.R_TAGS))
						refPrefix = Constants.R_TAGS;
					else {
						// the button should be disabled anyway, but we check
						// again
						return;
					}

					String branchName = refName.substring(refPrefix.length());

					InputDialog labelDialog = getRefNameInputDialog(
							NLS
									.bind(
											UIText.BranchSelectionDialog_QuestionNewBranchNameMessage,
											branchName, refPrefix), refPrefix);
					if (labelDialog.open() == Window.OK) {
						String newRefName = refPrefix + labelDialog.getValue();
						try {
							RefRename renameRef = repo.renameRef(refName,
									newRefName);
							if (renameRef.rename() != Result.RENAMED) {
								reportError(
										null,
										UIText.BranchSelectionDialog_ErrorCouldNotRenameRef,
										refName, newRefName, renameRef
												.getResult());
								return;
							}
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
					// check what ref name the user selected, if any.
					String refName = refNameFromDialog();

					String refPrefix;

					if (refName.startsWith(Constants.R_HEADS))
						refPrefix = Constants.R_HEADS;
					else if (refName.startsWith(Constants.R_REMOTES))
						refPrefix = Constants.R_REMOTES;
					else {
						// the button should be disabled anyway, but we check
						// again
						return;
					}

					InputDialog labelDialog = getRefNameInputDialog(
							NLS
									.bind(
											UIText.BranchSelectionDialog_QuestionNewBranchMessage,
											refName, refPrefix), refPrefix);

					if (labelDialog.open() == Window.OK) {
						String newRefName = refPrefix + labelDialog.getValue();
						RefUpdate updateRef;
						try {
							updateRef = repo.updateRef(newRefName);
							Ref startRef = repo.getRef(refName);
							ObjectId startAt = repo.resolve(refName);
							String startBranch;
							if (startRef != null)
								startBranch = refName;
							else
								startBranch = startAt.name();
							startBranch = repo.shortenRefName(startBranch);
							updateRef.setNewObjectId(startAt);
							updateRef
									.setRefLogMessage(
											"branch: Created from " + startBranch, false); //$NON-NLS-1$
							updateRef.update();
							branchTree.refresh();
							markRef(newRefName);
						} catch (Throwable e1) {
							reportError(
									e1,
									UIText.BranchSelectionDialog_ErrorCouldNotCreateNewRef,
									newRefName);
						}
					}
				}
			});
		}
		confirmationBtn = createButton(parent, IDialogConstants.OK_ID,
				showResetType ? UIText.BranchSelectionDialog_OkReset
						: UIText.BranchSelectionDialog_OkCheckout, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);

		// can't advance without a selection
		confirmationBtn.setEnabled(refNameFromDialog() != null);
	}

	private void reportError(Throwable e, String message, Object... args) {
		String msg = NLS.bind(message, args);
		Activator.handleError(msg, e, true);
	}

	private boolean isBranchSelected() {
		String refName = refNameFromDialog();

		// boolean tagSelected = refName != null
		// && refName.startsWith(Constants.R_TAGS);

		// TODO at the moment we don't support checking out
		// of remote branches; HEAD should point to the underlying
		// commit rather than to the branch
		boolean branchSelected = refName != null
				&& (refName.startsWith(Constants.R_HEADS));
		return branchSelected;
	}
}
