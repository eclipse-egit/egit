/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2014, Gregor Dschung <gregor.dschung@andrena.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.Set;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.osgi.util.NLS;

/**
 * Content provider for {@link FileDiff} objects
 */
public class FileDiffContentProvider implements IStructuredContentProvider {

	static final int INTERESTING_MARK_TREE_FILTER_INDEX = 0;

	private TreeWalk walk;

	private RevCommit commit;

	private FileDiff[] diff;

	private TreeFilter markTreeFilter = TreeFilter.ALL;

	private Repository repo;

	private final int maxNumberOfElements;

	/**
	 * A {@link FileDiffContentProvider} which isn't limited in the number of
	 * elements it's providing.
	 */
	public FileDiffContentProvider() {
		this.maxNumberOfElements = 0;
	}

	/**
	 * A {@link FileDiffContentProvider} which provides a limited number of
	 * elements.
	 *
	 * @param maxNumberOfElements
	 *            the number of elements {@link #getElements(Object)} is limited
	 *            to return
	 */
	public FileDiffContentProvider(final int maxNumberOfElements) {
		this.maxNumberOfElements = maxNumberOfElements;
	}

	public void inputChanged(final Viewer newViewer, final Object oldInput,
			final Object newInput) {
		if (newInput != null) {
			repo = ((CommitFileDiffViewer) newViewer).getRepository();
			walk = ((CommitFileDiffViewer) newViewer).getTreeWalk();
			commit = (RevCommit) newInput;
		} else {
			repo = null;
			walk = null;
			commit = null;
		}
		diff = null;
	}

	/**
	 * Set the paths which are interesting and should be highlighted in the view.
	 * @param interestingPaths
	 */
	void setInterestingPaths(Set<String> interestingPaths) {
		if (interestingPaths != null)
			this.markTreeFilter = PathFilterGroup.createFromStrings(interestingPaths);
		else
			this.markTreeFilter = TreeFilter.ALL;
		// FileDiffs need to be updated
		this.diff = null;
	}

	public Object[] getElements(final Object inputElement) {
		if (diff == null && walk != null && commit != null)
			try {
				diff = FileDiff.compute(repo, walk, commit,
						maxNumberOfElements, markTreeFilter);
			} catch (IOException err) {
				Activator.handleError(NLS.bind(UIText.FileDiffContentProvider_errorGettingDifference,
						commit.getId()), err, false);
			}
		return diff != null ? diff : new Object[0];
	}

	public void dispose() {
		// Nothing.
	}
}
