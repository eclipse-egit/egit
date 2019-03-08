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

import java.util.List;

import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.TreeColumnPatternFilter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.GitDateFormatter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.FilteredTree;

/**
 * Widget for viewing a filtered list of Gitflow branches.
 */
public class FilteredBranchesWidget {
	private TreeViewer branchesViewer;

	private final List<Ref> refs;

	private String prefix;

	private GitFlowRepository gfRepo;

	private BranchComparator comparator;

	private GitDateFormatter dateFormatter;

	FilteredBranchesWidget(List<Ref> refs, String prefix, GitFlowRepository gfRepo) {
		this.refs = refs;
		this.prefix = prefix;
		this.gfRepo = gfRepo;
	}

	Control create(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(area);
		area.setLayout(new GridLayout(1, false));

		final FilteredTree tree = new FilteredTree(area, SWT.MULTI | SWT.H_SCROLL
						| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION,
				new TreeColumnPatternFilter(),
				true);
		tree.setQuickSelectionMode(true);
		branchesViewer = tree.getViewer();
		branchesViewer.getTree().setLinesVisible(false);
		branchesViewer.getTree().setHeaderVisible(true);

		comparator = new BranchComparator();
		branchesViewer.setComparator(comparator);

		DecoratedBranchLabelProvider nameLabelProvider = new DecoratedBranchLabelProvider(gfRepo.getRepository(), prefix);
		TreeColumn nameColumn = createColumn(UIText.BranchSelectionTree_NameColumn, branchesViewer, nameLabelProvider);

		TreeColumn idColumn = createColumn(UIText.BranchSelectionTree_IdColumn, branchesViewer, new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof Ref) {
					ObjectId objectId = ((Ref) element).getObjectId();
					if (objectId == null) {
						return ""; //$NON-NLS-1$
					}
					return Utils.getShortObjectId(objectId);
				}
				return super.getText(element);
			}});
		ColumnLabelProvider dateLabelProvider = new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof Ref) {
					String name = ((Ref) element).getName().substring(Constants.R_HEADS.length());
					RevCommit revCommit = gfRepo.findHead(name);
					if (revCommit == null) {
						return ""; //$NON-NLS-1$
					}
					return getDateFormatter().formatDate(revCommit.getCommitterIdent());
				}
				return super.getText(element);
			}};
		TreeColumn dateColumn = createColumn(UIText.FilteredBranchesWidget_lastModified, branchesViewer, dateLabelProvider);
		setSortedColumn(dateColumn, dateLabelProvider);

		TreeColumn msgColumn = createColumn(UIText.BranchSelectionTree_MessageColumn, branchesViewer, new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof Ref) {
					String name = ((Ref) element).getName().substring(Constants.R_HEADS.length());
					RevCommit revCommit = gfRepo.findHead(name);
					if (revCommit == null) {
						return ""; //$NON-NLS-1$
					}
					return revCommit.getShortMessage();
				}
				return super.getText(element);
			}});


		GridDataFactory.fillDefaults().grab(true, true).applyTo(branchesViewer.getControl());

		branchesViewer.setContentProvider(new BranchListContentProvider());
		branchesViewer.setInput(refs);

		// Layout tree for maximum width of message column
		TreeColumnLayout layout = new TreeColumnLayout();
		nameColumn.pack();
		layout.setColumnData(nameColumn, new ColumnWeightData(0, nameColumn.getWidth()));
		idColumn.pack();
		layout.setColumnData(idColumn, new ColumnWeightData(0, idColumn.getWidth()));
		dateColumn.pack();
		layout.setColumnData(dateColumn, new ColumnWeightData(0, dateColumn.getWidth()));
		layout.setColumnData(msgColumn, new ColumnWeightData(100));
		branchesViewer.getTree().getParent().setLayout(layout);

		branchesViewer.addFilter(createFilter());
		return area;
	}

	private GitDateFormatter getDateFormatter() {
		if (dateFormatter == null) {
			dateFormatter = PreferenceBasedDateFormatter.create();
		}
		return dateFormatter;
	}

	private TreeColumn createColumn(final String name, TreeViewer treeViewer, final ColumnLabelProvider labelProvider) {
		final TreeColumn column = new TreeColumn(treeViewer.getTree(), SWT.LEFT);
		column.setAlignment(SWT.LEFT);
		column.setText(name);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSortedColumn(column, labelProvider);
			}
		});

		TreeViewerColumn treeViewerNameColumn = new TreeViewerColumn(treeViewer, column);
		treeViewerNameColumn.setLabelProvider(labelProvider);
		return column;
	}

	private void setSortedColumn(final TreeColumn column, ColumnLabelProvider labelProvider) {
		comparator.setColumn(column, labelProvider);
		int dir = comparator.getDirection();
		branchesViewer.getTree().setSortDirection(dir);
		branchesViewer.getTree().setSortColumn(column);
		branchesViewer.refresh();
	}

	private ViewerFilter createFilter() {
		return new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return true;
			}
		};
	}

	@SuppressWarnings("unchecked") // conversion to conform to List<Ref>
	List<Ref> getSelection() {
		return ((IStructuredSelection) branchesViewer.getSelection()).toList();
	}

	TreeViewer getBranchesList() {
		return branchesViewer;
	}
}
