/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.search;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * Commit search results page class.
 */
public class CommitSearchResultsPage extends AbstractTextSearchViewPage {

	private static class CommitSorter extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof RepositoryCommit
					&& e2 instanceof RepositoryCommit) {
				PersonIdent person1 = ((RepositoryCommit) e1).getRevCommit()
						.getAuthorIdent();
				PersonIdent person2 = ((RepositoryCommit) e2).getRevCommit()
						.getAuthorIdent();
				if (person1 != null && person2 != null)
					return person2.getWhen().compareTo(person1.getWhen());
			} else if (e1 instanceof RepositoryMatch
					&& e2 instanceof RepositoryMatch)
				return ((RepositoryMatch) e1).getLabel(e1).compareToIgnoreCase(
						((RepositoryMatch) e2).getLabel(e2));
			return super.compare(viewer, e1, e2);
		}
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#elementsChanged(java.lang.Object[])
	 */
	@Override
	protected void elementsChanged(Object[] objects) {
		getViewer().refresh();
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#clear()
	 */
	@Override
	protected void clear() {
		getViewer().refresh();
	}

	private void configureViewer(StructuredViewer viewer) {
		viewer.setComparator(new CommitSorter());
		viewer.setContentProvider(new WorkbenchContentProvider() {

			@Override
			public Object[] getElements(Object element) {
				if (getLayout() == FLAG_LAYOUT_TREE) {
					Map<Repository, RepositoryMatch> repos = new HashMap<>();
					for (Object inputElement : getInput().getElements()) {
						RepositoryCommit commit = (RepositoryCommit) inputElement;
						repos.computeIfAbsent(commit.getRepository(),
								r -> new RepositoryMatch(r)).addCommit(commit);
					}
					return repos.values().toArray();
				}
				return super.getElements(element);
			}

		});
		viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
				new CommitResultLabelProvider(getLayout())));
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTreeViewer(org.eclipse.jface.viewers.TreeViewer)
	 */
	@Override
	protected void configureTreeViewer(TreeViewer viewer) {
		configureViewer(viewer);
	}

	/**
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTableViewer(org.eclipse.jface.viewers.TableViewer)
	 */
	@Override
	protected void configureTableViewer(TableViewer viewer) {
		configureViewer(viewer);
	}

	@Override
	protected void showMatch(Match match, int currentOffset, int currentLength,
			boolean activate) throws PartInitException {
		if (match instanceof CommitMatch)
			CommitEditor.open(((CommitMatch) match).getCommit());
	}
}
