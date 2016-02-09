/*******************************************************************************
 * Copyright (C) 2016, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.dialogs;

import static org.eclipse.egit.ui.internal.CommonUtils.STRING_ASCENDING_COMPARATOR;

import java.util.List;

import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * Widget for viewing a filtered list of Gitflow branches.
 */
public class FilteredBranchesWidget {
	private TreeViewer branchesViewer;

	private final List<Ref> refs;

	private String prefix;

	FilteredBranchesWidget(List<Ref> refs, String prefix) {
		this.refs = refs;
		this.prefix = prefix;
	}

	Control create(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(area);
		area.setLayout(new GridLayout(1, false));

		PatternFilter filter = new PatternFilter() {
			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				TreeViewer treeViewer = (TreeViewer) viewer;
				int numberOfColumns = treeViewer.getTree().getColumnCount();
				boolean isMatch = false;
				for (int columnIndex = 0; columnIndex < numberOfColumns; columnIndex++) {
					ColumnLabelProvider labelProvider = (ColumnLabelProvider) treeViewer
							.getLabelProvider(columnIndex);
					String labelText = labelProvider.getText(element);
					isMatch |= wordMatches(labelText);
				}
				return isMatch;
			}
		};
		filter.setIncludeLeadingWildcard(true);

		final FilteredTree tree = new FilteredTree(area, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION, filter,
				true);
		tree.setQuickSelectionMode(true);
		branchesViewer = tree.getViewer();

		TreeColumn nameColumn = new TreeColumn(branchesViewer.getTree(), SWT.LEFT);
		branchesViewer.getTree().setLinesVisible(false);
		nameColumn.setAlignment(SWT.LEFT);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(branchesViewer.getControl());

		branchesViewer.setContentProvider(new BranchListContentProvider());
		branchesViewer.setLabelProvider(createLabelProvider());
		branchesViewer.setComparator(new ViewerComparator(STRING_ASCENDING_COMPARATOR));
		branchesViewer.setInput(refs);

		nameColumn.pack();

		branchesViewer.addFilter(createFilter());
		return area;
	}

	private ViewerFilter createFilter() {
		return new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return true;
			}
		};
	}

	private ColumnLabelProvider createLabelProvider() {
		return new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof Ref) {
					String name = ((Ref) element).getName();
					return name.substring(prefix.length());
				}
				return super.getText(element);
			}

			@Override
			public Image getImage(Object element) {
				return RepositoryTreeNodeType.REF.getIcon();
			}

		};
	}

	@SuppressWarnings("unchecked") // conversion to conform to List<Ref>
	List<Ref> getSelection() {
		return branchesViewer.getStructuredSelection().toList();
	}

	TreeViewer getBranchesList() {
		return branchesViewer;
	}
}
