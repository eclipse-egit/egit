/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
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

	private boolean showLocalBranches = true;

	private boolean showRemoteBranches = true;

	private boolean showTags = true;

	private boolean showReferences = true;

	/**
	 * Construct a dialog to select a branch.
	 * <p>
	 * The currently checked out {@link Ref} is marked if possible
	 *
	 * @param parentShell
	 * @param repository
	 *            the {@link Repository}
	 */
	public AbstractBranchSelectionDialog(Shell parentShell,
			Repository repository) {
		this(parentShell, repository, null);
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
	 */
	public AbstractBranchSelectionDialog(Shell parentShell,
			Repository repository, String refToMark) {
		super(parentShell);
		this.repo = repository;
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

		FilteredTree tree = new FilteredTree(parent, SWT.SINGLE | SWT.BORDER,
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
					okPressed();

			}
		});

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

		List<RepositoryTreeNode> roots = new ArrayList<RepositoryTreeNode>();
		if (showLocalBranches)
			roots.add(localBranches);
		if (showRemoteBranches)
			roots.add(remoteBranches);
		if (showTags)
			roots.add(tags);
		if (showReferences)
			roots.add(references);

		branchTree.setInput(roots);

		try {
			if (refToMark != null) {
				if (!markRef(refToMark))
					// if we can't determine a branch, we just expand local
					// branches
					branchTree.expandToLevel(localBranches, 1);
			} else {
				// initially, we mark the current head if it can be determined
				String fullBranch = repo.getFullBranch();
				if (!markRef(fullBranch))
					// if we can't determine a branch, we just expand local
					// branches
					branchTree.expandToLevel(localBranches, 1);
			}
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

	/**
	 * @param showLocalBranches show/hide the local branches root
	 * @param showRemoteBranches show/hide the remote branches root
	 * @param showTags show/hide the tag root
	 * @param showReferences show/hide the references root
	 */
	protected void setRootsToShow(boolean showLocalBranches,
			boolean showRemoteBranches, boolean showTags, boolean showReferences) {
		this.showLocalBranches = showLocalBranches;
		this.showRemoteBranches = showRemoteBranches;
		this.showTags = showTags;
		this.showReferences = showReferences;
	}
}
