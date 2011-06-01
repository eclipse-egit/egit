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

import static org.eclipse.egit.ui.internal.CommonUtils.STRING_ASCENDING_COMPARATOR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefsNode;
import org.eclipse.egit.ui.internal.repository.tree.LocalNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteTrackingNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * The abstract base class to display a branch/tag selection dialog.
 * <p>
 * This will construct a tree similar to the Git Repositories View from which
 * the user can select an item. Concrete subclasses are responsible to check
 * whether the selection is valid and may add extra UI elements by overriding
 * {@link #createCustomArea(Composite)}.
 */
public abstract class AbstractBranchSelectionDialog extends TitleAreaDialog {

	/** The {@link Repository} used in the constructor */
	protected final Repository repo;

	/** The tree */
	protected TreeViewer branchTree;

	private String selectedBranch;

	private final String refToMark;

	private final RepositoryTreeNode<Repository> localBranches;

	private final RepositoryTreeNode<Repository> remoteBranches;

	private final RepositoryTreeNode<Repository> tags;

	private final RepositoryTreeNode<Repository> references;

	/** Determinate does local branches should be show or not */
	protected static final int SHOW_LOCAL_BRANCHES = 1 << 1;

	/** Determinate does remote branches should be show or not */
	protected static final int SHOW_REMOTE_BRANCHES = 1 << 2;

	/** Determinate does tags should be show or not */
	protected static final int SHOW_TAGS = 1 << 3;

	/** Determinate does references shout be show or not */
	protected static final int SHOW_REFERENCES = 1 << 4;

	/** Determinate does current should be selected or not */
	protected static final int SELECT_CURRENT_REF = 1 << 5;

	/** Determinate does local branches should be expanded or not */
	protected static final int EXPAND_LOCAL_BRANCHES_NODE = 1 << 6;

	/** Determinate does remote branches should be expanded or not */
	protected static final int EXPAND_REMOTE_BRANCHES_NODE = 1 << 7;

	/**
	 * Will allow select multiple branches. The implementer must override
	 * {@link AbstractBranchSelectionDialog#refNameFromDialog()} to be able to
	 * obtain list of selected branches
	 */
	protected static final int ALLOW_MULTISELECTION = 1 << 8;

	private final int settings;

	/**
	 * Construct a dialog to select a branch.
	 * <p>
	 * The currently checked out {@link Ref} is marked if possible
	 *
	 * @param parentShell
	 * @param repository
	 *            the {@link Repository}
	 * @param settings
	 *            configuration options of this dialog like
	 *            {@link AbstractBranchSelectionDialog#SHOW_LOCAL_BRANCHES},
	 *            {@link AbstractBranchSelectionDialog#SHOW_REMOTE_BRANCHES},
	 *            {@link AbstractBranchSelectionDialog#SHOW_TAGS},
	 *            {@link AbstractBranchSelectionDialog#SHOW_REFERENCES},
	 *            {@link AbstractBranchSelectionDialog#SELECT_CURRENT_REF},
	 *            {@link AbstractBranchSelectionDialog#EXPAND_LOCAL_BRANCHES_NODE},
	 *            {@link AbstractBranchSelectionDialog#EXPAND_REMOTE_BRANCHES_NODE}
	 */
	public AbstractBranchSelectionDialog(Shell parentShell,
			Repository repository, int settings) {
		this(parentShell, repository, null, settings);
		setHelpAvailable(false);
	}

	/**
	 * Construct a dialog to select a branch and specify a {@link Ref} to mark
	 *
	 * @param parentShell
	 * @param repository
	 *            the {@link Repository}
	 * @param refToMark
	 *            the name of the {@link Ref} to mark initially
	 * @param settings
	 *            configuration options of this dialog like
	 *            {@link AbstractBranchSelectionDialog#SHOW_LOCAL_BRANCHES},
	 *            {@link AbstractBranchSelectionDialog#SHOW_REMOTE_BRANCHES},
	 *            {@link AbstractBranchSelectionDialog#SHOW_TAGS},
	 *            {@link AbstractBranchSelectionDialog#SHOW_REFERENCES},
	 *            {@link AbstractBranchSelectionDialog#SELECT_CURRENT_REF},
	 *            {@link AbstractBranchSelectionDialog#EXPAND_LOCAL_BRANCHES_NODE},
	 *            {@link AbstractBranchSelectionDialog#EXPAND_REMOTE_BRANCHES_NODE}
	 */
	public AbstractBranchSelectionDialog(Shell parentShell,
			Repository repository, String refToMark, int settings) {
		super(parentShell);
		this.repo = repository;
		this.settings = settings;
		localBranches = new LocalNode(null, this.repo);
		remoteBranches = new RemoteTrackingNode(null, this.repo);
		tags = new TagsNode(null, this.repo);
		references = new AdditionalRefsNode(null, this.repo);
		this.refToMark = refToMark;
		setHelpAvailable(false);
	}

	/**
	 * Concrete subclasses should implement their check logic around this
	 *
	 * @param refName
	 *            the name of the currently selected {@link Ref}, may be null
	 */
	protected abstract void refNameSelected(String refName);

	/**
	 * Subclasses must provide the title of the dialog
	 *
	 * @return the title of the dialog
	 */
	protected abstract String getTitle();

	/**
	 * @return the message shown above the refs tree
	 */
	protected abstract String getMessageText();

	/**
	 * Subclasses should provide the title of the dialog window
	 * <p>
	 * Defaults to {@link #getTitle()}
	 *
	 * @return the title of the dialog window
	 */
	protected String getWindowTitle() {
		return getTitle();
	}

	@Override
	protected final Composite createDialogArea(Composite base) {
		Composite parent = (Composite) super.createDialogArea(base);
		parent.setLayout(GridLayoutFactory.fillDefaults().create());

		int selectionModel = -1;
		if ((settings & ALLOW_MULTISELECTION) != 0)
			selectionModel = SWT.MULTI;
		else
			selectionModel = SWT.SINGLE;
		FilteredTree tree = new FilteredTree(parent, selectionModel | SWT.BORDER,
				new PatternFilter(), true);
		branchTree = tree.getViewer();
		branchTree.setLabelProvider(new RepositoriesViewLabelProvider());
		branchTree.setContentProvider(new RepositoriesViewContentProvider());

		GridDataFactory.fillDefaults().grab(true, true).hint(500, 300).applyTo(
				tree);
		branchTree.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				String refName = refNameFromDialog();
				refNameSelected(refName);
			}
		});

		// double-click support
		branchTree.addOpenListener(new IOpenListener() {

			public void open(OpenEvent event) {
				RepositoryTreeNode node = (RepositoryTreeNode) ((IStructuredSelection) branchTree
						.getSelection()).getFirstElement();
				if (node == null)
					return;
				if (node.getType() != RepositoryTreeNodeType.REF
						&& node.getType() != RepositoryTreeNodeType.TAG)
					branchTree.setExpandedState(node, !branchTree
							.getExpandedState(node));
				else if (getButton(Window.OK).isEnabled())
					buttonPressed(OK);

			}
		});

		branchTree.setComparator(new ViewerComparator(
				STRING_ASCENDING_COMPARATOR));

		createCustomArea(parent);

		setTitle(getTitle());
		setMessage(getMessageText());
		getShell().setText(getWindowTitle());

		applyDialogFont(parent);

		return parent;
	}

	/**
	 * Concrete subclasses must issue a call to super.create() when overriding
	 * this
	 */
	@Override
	public void create() {
		super.create();

		// Initially disable OK button, as the required user inputs may not be
		// complete after the dialog is first shown. If automatic selections
		// happen after this (making the user inputs complete), the button will
		// be enabled.
		getButton(Window.OK).setEnabled(false);

		List<RepositoryTreeNode> roots = new ArrayList<RepositoryTreeNode>();
		if ((settings & SHOW_LOCAL_BRANCHES) != 0)
			roots.add(localBranches);
		if ((settings & SHOW_REMOTE_BRANCHES) != 0)
			roots.add(remoteBranches);
		if ((settings & SHOW_TAGS) != 0)
			roots.add(tags);
		if ((settings & SHOW_REFERENCES) != 0)
			roots.add(references);

		branchTree.setInput(roots);

		try {
			if ((settings & SELECT_CURRENT_REF) != 0)
				if (refToMark != null)
					markRef(refToMark);
				else {
					// initially, we mark the current head if it can be determined
					String fullBranch = repo.getFullBranch();
					markRef(fullBranch);
				}
			if ((settings & EXPAND_LOCAL_BRANCHES_NODE) != 0)
				// if we can't determine a branch, we just expand local
				// branches
				branchTree.expandToLevel(localBranches, 1);
			if ((settings & EXPAND_REMOTE_BRANCHES_NODE) != 0)
				// minor UX improvement to always expand remote branches node
				branchTree.expandToLevel(remoteBranches, 1);
		} catch (IOException e) {
			// ignore
		}
	}

	/**
	 * Set the selection to a {@link Ref} if possible
	 *
	 * @param refName
	 *            the name of the {@link Ref}
	 * @return <code>true</code> if the {@link Ref} with the given name was
	 *         found
	 */
	protected boolean markRef(String refName) {
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
	 * Will only work after the dialog was closed with the OK button
	 *
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

	/**
	 * @return the selected ref name from the tree, may be null
	 */
	protected String refNameFromDialog() {
		IStructuredSelection sel = (IStructuredSelection) branchTree
				.getSelection();
		if (sel.size() != 1)
			return null;
		RepositoryTreeNode node = (RepositoryTreeNode) sel.getFirstElement();
		if (node.getType() == RepositoryTreeNodeType.REF
				|| node.getType() == RepositoryTreeNodeType.TAG
				|| node.getType() == RepositoryTreeNodeType.ADDITIONALREF)
			return ((Ref) node.getObject()).getName();
		return null;
	}

	/**
	 * @return the selected {@link Ref} from the tree, may be null
	 */
	protected Ref refFromDialog() {
		IStructuredSelection sel = (IStructuredSelection) branchTree
				.getSelection();
		if (sel.size() != 1)
			return null;
		RepositoryTreeNode node = (RepositoryTreeNode) sel.getFirstElement();
		if (node.getType() == RepositoryTreeNodeType.REF
				|| node.getType() == RepositoryTreeNodeType.TAG) {
			return ((Ref) node.getObject());
		}
		return null;
	}

	/**
	 * Subclasses may add UI elements
	 *
	 * @param parent
	 */
	protected void createCustomArea(Composite parent) {
		// do nothing
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

}
