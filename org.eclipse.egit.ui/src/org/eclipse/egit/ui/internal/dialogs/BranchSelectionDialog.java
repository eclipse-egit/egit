/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
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

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.LocalBranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteBranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * The branch and reset selection dialog
 */
public class BranchSelectionDialog extends TitleAreaDialog {

	private final Repository repo;

	private TreeViewer branchTree;

	/**
	 * button which finally triggers the action
	 */
	protected Button confirmationBtn;

	private Button renameButton;

	private Button newButton;

	private String selectedBranch;

	private final RepositoryTreeNode<Repository> localBranches;

	private final RepositoryTreeNode<Repository> remoteBranches;

	private final RepositoryTreeNode<Repository> tags;

	/**
	 * Construct a dialog to select a branch to reset to or check out
	 * @param parentShell
	 * @param repo
	 */
	public BranchSelectionDialog(Shell parentShell, Repository repo) {
		super(parentShell);
		this.repo = repo;
		localBranches = new LocalBranchesNode(null, this.repo);
		remoteBranches = new RemoteBranchesNode(null, this.repo);
		tags = new TagsNode(null, this.repo);
	}

	@Override
	protected Composite createDialogArea(Composite base) {
		Composite parent = (Composite) super.createDialogArea(base);
		parent.setLayout(GridLayoutFactory.fillDefaults().create());

		// TODO deprecated constructor for now
		FilteredTree tree = new FilteredTree(parent, SWT.SINGLE | SWT.BORDER, new PatternFilter());
		branchTree = tree.getViewer();
		new RepositoriesViewLabelProvider(branchTree);
		branchTree.setContentProvider(new RepositoriesViewContentProvider());

		GridDataFactory.fillDefaults().grab(true, true).hint(500, 300).applyTo(
				tree);
		branchTree.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				// enable the buttons depending on the selection

				String refName = refNameFromDialog();

				boolean tagSelected = refName != null
						&& refName.startsWith(Constants.R_TAGS);

				boolean branchSelected = refName != null
						&& (refName.startsWith(Constants.R_HEADS) || refName
								.startsWith(Constants.R_REMOTES));

				// we don't allow reset on tags, but checkout
				if (!canConfirmOnTag())
					confirmationBtn.setEnabled(branchSelected);
				else
					confirmationBtn.setEnabled(branchSelected || tagSelected);

				// we don't support rename on tags
				if (renameButton != null) {
					renameButton.setEnabled(branchSelected && !tagSelected
							&& !tagSelected);
				}

				// new branch can not be based on a tag
				if (newButton != null) {
					newButton.setEnabled(branchSelected && !tagSelected);
				}
			}
		});

		// double-click support
		branchTree.addOpenListener(new IOpenListener() {

			public void open(OpenEvent event) {
				RepositoryTreeNode node = (RepositoryTreeNode) ((IStructuredSelection) branchTree
						.getSelection()).getFirstElement();
				if (node.getType() != RepositoryTreeNodeType.REF
						&& node.getType() != RepositoryTreeNodeType.TAG)
					branchTree.setExpandedState(node, !branchTree
							.getExpandedState(node));
				else if (confirmationBtn.isEnabled())
					okPressed();

			}
		});

		createCustomArea(parent);

		String rawTitle = getTitle();
		String title = NLS.bind(rawTitle, new Object[] { repo.getDirectory() });

		setTitle(title);
		setMessage(getMessageText());
		getShell().setText(title);

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

		RepositoryTreeNode node;
		try {
			if (refName.startsWith(Constants.R_HEADS)) {
				Ref ref = this.repo.getRef(refName);
				node = new RefNode(localBranches, this.repo, ref);
			} else {
				String mappedRef = Activator.getDefault().getRepositoryUtil()
						.mapCommitToRef(this.repo, refName, false);
				if (mappedRef != null
						&& mappedRef.startsWith(Constants.R_REMOTES)) {
					Ref ref = this.repo.getRef(mappedRef);
					node = new RefNode(remoteBranches, this.repo, ref);
				} else if (mappedRef != null
						&& mappedRef.startsWith(Constants.R_TAGS)) {
					Ref ref = this.repo.getRef(mappedRef);
					node = new TagNode(tags, this.repo, ref);
				} else {
					return false;
				}
			}
		} catch (IOException e) {
			return false;
		}

		branchTree.setSelection(new StructuredSelection(node), true);
		return true;
	}

	/**
	 * @return the selected refName
	 */
	public String getRefName() {
		return this.selectedBranch;
	}

	@Override
	protected void okPressed() {
		this.selectedBranch = refNameFromDialog();
		super.okPressed();
	}

	private String refNameFromDialog() {
		IStructuredSelection sel = (IStructuredSelection) branchTree
				.getSelection();
		if (sel.size() != 1)
			return null;
		RepositoryTreeNode node = (RepositoryTreeNode) sel.getFirstElement();
		if (node.getType() == RepositoryTreeNodeType.REF
				|| node.getType() == RepositoryTreeNodeType.TAG) {
			return ((Ref) node.getObject()).getName();
		}
		return null;
	}

	private InputDialog getRefNameInputDialog(String prompt, final String refPrefix) {
		InputDialog labelDialog = new InputDialog(
				getShell(),
				UIText.BranchSelectionDialog_QuestionNewBranchTitle,
				prompt,
				null, ValidationUtils.getRefNameInputValidator(repo, refPrefix));
		labelDialog.setBlockOnOpen(true);
		return labelDialog;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		newButton = new Button(parent, SWT.PUSH);
		newButton.setFont(JFaceResources.getDialogFont());
		newButton.setText(UIText.BranchSelectionDialog_NewBranch);
		setButtonLayoutData(newButton);
		((GridLayout)parent.getLayout()).numColumns++;

		renameButton = new Button(parent, SWT.PUSH);
		renameButton.setFont(JFaceResources.getDialogFont());
		renameButton.setText(UIText.BranchSelectionDialog_Rename);
		setButtonLayoutData(renameButton);
		((GridLayout)parent.getLayout()).numColumns++;

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
					// the button should be disabled anyway, but we check again
					return;
				}

				String branchName = refName.substring(refPrefix.length());

				InputDialog labelDialog = getRefNameInputDialog(NLS
						.bind(
								UIText.BranchSelectionDialog_QuestionNewBranchNameMessage,
								branchName, refPrefix), refPrefix);
				if (labelDialog.open() == Window.OK) {
					String newRefName = refPrefix + labelDialog.getValue();
					try {
						RefRename renameRef = repo.renameRef(refName, newRefName);
						if (renameRef.rename() != Result.RENAMED) {
							reportError(
									null,
									UIText.BranchSelectionDialog_ErrorCouldNotRenameRef,
									refName, newRefName, renameRef
											.getResult());
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

				// the button should be disabled anyway, but we check again
				if (refName.equals(Constants.HEAD))
					return;
				if (refName.startsWith(Constants.R_TAGS))
					// the button should be disabled anyway, but we check again
					return;

				InputDialog labelDialog = getRefNameInputDialog(
						NLS
								.bind(
										UIText.BranchSelectionDialog_QuestionNewBranchMessage,
										refName, Constants.R_HEADS),
						Constants.R_HEADS);

				if (labelDialog.open() == Window.OK) {
					String newRefName = Constants.R_HEADS + labelDialog.getValue();
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
						updateRef.setRefLogMessage("branch: Created from " + startBranch, false); //$NON-NLS-1$
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
		confirmationBtn = createButton(parent, IDialogConstants.OK_ID,
				UIText.BranchSelectionDialog_OkCheckout, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

		// can't advance without a selection
		confirmationBtn.setEnabled(!branchTree.getSelection().isEmpty());
	}

	/**
	 * @return the message shown above the refs tree
	 */
	protected String getMessageText() {
		return UIText.BranchSelectionDialog_Refs;
	}

	/**
	* Subclasses may add UI elements
	* @param parent
	*/
	protected void createCustomArea(Composite parent) {
	// do nothing
	}

	/**
	* Subclasses may change the title of the dialog
	* @return the title of the dialog
	*/
	protected String getTitle() {
		return UIText.BranchSelectionDialog_TitleCheckout;
	}

	/**
	*
	* @return if the confirmation button is enabled when a tag is selected
	*/
	protected boolean canConfirmOnTag() {
		return true;
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	private void reportError(Throwable e, String message, Object... args) {
		String msg = NLS.bind(message, args);
		Activator.handleError(msg, e, true);
	}
}
