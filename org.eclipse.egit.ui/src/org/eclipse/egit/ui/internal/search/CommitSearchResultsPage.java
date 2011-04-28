/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.search;

import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * Commit search results page class.
 */
public class CommitSearchResultsPage extends AbstractTextSearchViewPage {

	private class CommitSorter extends ViewerSorter {

		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof RepositoryCommit
					&& e2 instanceof RepositoryCommit) {
				PersonIdent person1 = ((RepositoryCommit) e1).getRevCommit()
						.getAuthorIdent();
				PersonIdent person2 = ((RepositoryCommit) e2).getRevCommit()
						.getAuthorIdent();
				if (person1 != null && person2 != null)
					return person2.getWhen().compareTo(person1.getWhen());
			}
			return super.compare(viewer, e1, e2);
		}

	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#elementsChanged(java.lang.Object[])
	 */
	protected void elementsChanged(Object[] objects) {
		getViewer().refresh();
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#clear()
	 */
	protected void clear() {
		getViewer().refresh();
	}

	private void configureViewer(StructuredViewer viewer) {
		viewer.setSorter(new CommitSorter());
		viewer.setContentProvider(new WorkbenchContentProvider());
		viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
				new CommitResultLabelProvider()));
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTreeViewer(org.eclipse.jface.viewers.TreeViewer)
	 */
	protected void configureTreeViewer(TreeViewer viewer) {
		configureViewer(viewer);
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTableViewer(org.eclipse.jface.viewers.TableViewer)
	 */
	protected void configureTableViewer(TableViewer viewer) {
		configureViewer(viewer);
	}

}
