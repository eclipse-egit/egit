/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;

/**
 * ContentProvider for staged and unstaged table nodes
 */
public class GitXStagingViewContentProvider implements
		IStructuredContentProvider {
	private GitXStagedNode[] content;
	private boolean isWorkspace;

	GitXStagingViewContentProvider(boolean workspace) {
		this.isWorkspace = workspace;
	}

	public Object[] getElements(Object inputElement) {
		return content;
	}

	public void inputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		if (newInput != null) {

			Repository repository = (Repository)((Object[])newInput)[0];
			IndexDiff indexDiff = (IndexDiff)((Object[])newInput)[1];

			if (isWorkspace) {
				content = new GitXStagedNode[indexDiff.getMissing().size() + indexDiff.getModified().size() + indexDiff.getUntracked().size() + indexDiff.getConflicting().size()];
				int i = 0;
				for (String file : indexDiff.getMissing())
					content[i++] = new GitXStagedNode(repository, GitXStagedNode.State.MISSING, file);
				for (String file : indexDiff.getModified())
					content[i++] = new GitXStagedNode(repository, GitXStagedNode.State.MODIFIED, file);
				for (String file : indexDiff.getUntracked())
					content[i++] = new GitXStagedNode(repository, GitXStagedNode.State.UNTRACKED, file);
				for (String file : indexDiff.getConflicting())
					content[i++] = new GitXStagedNode(repository, GitXStagedNode.State.CONFLICTING, file);
			} else {
				content = new GitXStagedNode[indexDiff.getAdded().size() + indexDiff.getChanged().size() + indexDiff.getRemoved().size()];
				int i = 0;
				for (String file : indexDiff.getAdded())
					content[i++] = new GitXStagedNode(repository, GitXStagedNode.State.ADDED, file);
				for (String file : indexDiff.getChanged())
					content[i++] = new GitXStagedNode(repository, GitXStagedNode.State.CHANGED, file);
				for (String file : indexDiff.getRemoved())
					content[i++] = new GitXStagedNode(repository, GitXStagedNode.State.REMOVED, file);

			}
		}
	}

	public void dispose() {
		// nothing to dispose
	}
}
