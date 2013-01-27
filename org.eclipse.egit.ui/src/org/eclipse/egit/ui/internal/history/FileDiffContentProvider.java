/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;

/**
 * Content provider for {@link FileDiff} objects
 */
public class FileDiffContentProvider implements IStructuredContentProvider {
	private TreeWalk walk;

	private RevCommit commit;

	private FileDiff[] diff;

	public void inputChanged(final Viewer newViewer, final Object oldInput,
			final Object newInput) {
		if (newInput != null) {
			walk = ((CommitFileDiffViewer) newViewer).getTreeWalk();
			commit = (RevCommit) newInput;
		} else {
			walk = null;
			commit = null;
		}
		diff = null;
	}

	public Object[] getElements(final Object inputElement) {
		if (diff == null && walk != null && commit != null)
			try {
				diff = FileDiff.compute(walk, commit);
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
