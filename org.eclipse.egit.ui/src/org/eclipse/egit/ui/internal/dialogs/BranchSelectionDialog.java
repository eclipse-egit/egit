/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
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
package org.eclipse.egit.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.components.CachedCheckboxTreeViewer;
import org.eclipse.egit.ui.internal.components.FilteredCheckboxTree;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Allows to display some Branches for selection
 * <p>
 * In case of multi-selection, a checkbox tree is shown including a pattern
 * filter, while in case of single selection, a simple table is used supporting
 * double-click
 *
 * @param <T>
 *            the type of node; either {@link Ref} or and {@link IAdaptable} for
 *            {@link Ref}
 */
public class BranchSelectionDialog<T> extends MessageDialog {

	private final List<T> nodes;

	private TableViewer branchesList;

	private FilteredCheckboxTree fTree;

	private List<T> selected = new ArrayList<>();

	private final int style;

	private final boolean multiMode;

	private boolean preselectedBranch;

	/**
	 * @param parentShell
	 * @param nodes
	 * @param title
	 * @param message
	 * @param buttonLabel
	 *                        label of the okay button, should be the verb used
	 *                        in the title
	 * @param style
	 *                        only {@link SWT#SINGLE} and {@link SWT#MULTI} are
	 *                        supported
	 */
	public BranchSelectionDialog(Shell parentShell, List<T> nodes, String title,
			String message, String buttonLabel, int style) {
		super(parentShell, title, null, message, MessageDialog.QUESTION,
				new String[] { buttonLabel,
						IDialogConstants.CANCEL_LABEL }, 0);
		this.nodes = nodes;
		this.style = style;
		this.multiMode = (this.style & SWT.MULTI) > 0;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1)
				.applyTo(area);
		area.setLayout(new GridLayout(1, false));
		if (multiMode) {
			fTree = new FilteredCheckboxTree(area, null, SWT.NONE,
					new PatternFilter()) {
				/*
				 * Overridden to check page when refreshing is done.
				 */
				@Override
				protected WorkbenchJob doCreateRefreshJob() {
					WorkbenchJob refreshJob = super.doCreateRefreshJob();
					refreshJob.addJobChangeListener(new JobChangeAdapter() {
						@Override
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								getDisplay().asyncExec(new Runnable() {
									@Override
									public void run() {
										checkPage();
									}
								});
							}
						}
					});
					return refreshJob;
				}
			};

			CachedCheckboxTreeViewer viewer = fTree.getCheckboxTreeViewer();
			GridDataFactory.fillDefaults().grab(true, true).applyTo(fTree);
			viewer.setContentProvider(new ITreeContentProvider() {
				@Override
				public void inputChanged(Viewer actViewer, Object oldInput,
						Object newInput) {
					// nothing
				}

				@Override
				public void dispose() {
					// nothing
				}

				@Override
				public boolean hasChildren(Object element) {
					return false;
				}

				@Override
				public Object getParent(Object element) {
					return null;
				}

				@Override
				public Object[] getElements(Object inputElement) {
					return ((List) inputElement).toArray();
				}

				@Override
				public Object[] getChildren(Object parentElement) {
					return null;
				}
			});

			viewer.addCheckStateListener(new ICheckStateListener() {
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
					checkPage();
				}
			});

			viewer.setLabelProvider(new GitLabelProvider());
			viewer.setComparator(new ViewerComparator(
					CommonUtils.STRING_ASCENDING_COMPARATOR));
			viewer.setInput(nodes);

			preselectBranchMultiMode(nodes, fTree);
		} else {
			branchesList = new TableViewer(area, this.style | SWT.H_SCROLL
					| SWT.V_SCROLL | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, true)
					.applyTo(branchesList.getControl());
			branchesList.setContentProvider(ArrayContentProvider.getInstance());
			branchesList.setLabelProvider(new GitLabelProvider());
			branchesList.setComparator(new ViewerComparator(
					CommonUtils.STRING_ASCENDING_COMPARATOR));
			branchesList.setInput(nodes);

			preselectBranchSingleMode(nodes, branchesList);

			branchesList
					.addSelectionChangedListener(new ISelectionChangedListener() {
						@Override
						public void selectionChanged(SelectionChangedEvent event) {
							checkPage();
						}
					});
			branchesList.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					buttonPressed(OK);
				}
			});
		}
		return area;
	}

	private Set<Ref> getLocalBranches(List<T> list) {
		Set<Ref> branches = new HashSet<>();
		for (Object o : list) {
			if (o instanceof Ref) {
				Ref r = (Ref) o;
				String name = r.getName();
				if (name.startsWith(Constants.R_HEADS)) {
					branches.add(r);
				}
			}
		}
		return branches;
	}

	private void preselectBranchMultiMode(List<T> list,
			FilteredCheckboxTree tree) {
		Set<Ref> branches = getLocalBranches(list);
		if (branches.size() == 1) {
			Ref b = branches.iterator().next();
			tree.getCheckboxTreeViewer().setChecked(b, true);
			preselectedBranch = true;
		}
	}

	private void preselectBranchSingleMode(List<T> list, TableViewer table) {
		Set<Ref> branches = getLocalBranches(list);
		if (branches.size() == 1) {
			Ref b = branches.iterator().next();
			table.setSelection(new StructuredSelection(b), true);
			preselectedBranch = true;
		}

	}

	private void checkPage() {
		Button ok = getButton(OK);
		if (ok == null || ok.isDisposed()) {
			return;
		}

		if (multiMode) {
			if (fTree == null || fTree.isDisposed()) {
				return;
			}
			ok.setEnabled(
					fTree.getCheckboxTreeViewer().getCheckedLeafCount() > 0);
		} else {
			ok.setEnabled(!branchesList.getSelection().isEmpty());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			if (multiMode) {
				selected.clear();
				Object[] checked = fTree.getCheckboxTreeViewer()
						.getCheckedElements();
				for (Object o : checked) {
					selected.add((T) o);
				}
			} else {
				selected = ((IStructuredSelection) branchesList.getSelection())
						.toList();
			}
		}
		super.buttonPressed(buttonId);
	}

	@Override
	public void create() {
		super.create();
		getButton(OK).setEnabled(preselectedBranch);
	}

	/**
	 * @return the selected entry (single mode)
	 */
	public T getSelectedNode() {
		if (selected.isEmpty())
			return null;
		return selected.get(0);
	}

	/**
	 * @return the selected entries (multi mode)
	 */
	public List<T> getSelectedNodes() {
		return selected;
	}
}
