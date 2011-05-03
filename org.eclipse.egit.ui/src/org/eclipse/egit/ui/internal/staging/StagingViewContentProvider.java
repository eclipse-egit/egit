/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;

/**
 * ContentProvider for staged and unstaged table nodes
 */
public class StagingViewContentProvider implements
		IStructuredContentProvider {
	private StagedNode[] content;
	private boolean isWorkspace;

	StagingViewContentProvider(boolean workspace) {
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

			Set<StagedNode> nodes = new TreeSet<StagedNode>(new Comparator<StagedNode>() {
				public int compare(StagedNode o1, StagedNode o2) {
					return o1.getPath().compareTo(o2.getPath());
				}
			});

			if (isWorkspace) {
				for (String file : indexDiff.getMissing())
					nodes.add(new StagedNode(repository, StagedNode.State.MISSING, file));
				for (String file : indexDiff.getModified())
					nodes.add(new StagedNode(repository, StagedNode.State.MODIFIED, file));
				for (String file : indexDiff.getUntracked())
					nodes.add(new StagedNode(repository, StagedNode.State.UNTRACKED, file));
				for (String file : indexDiff.getConflicting())
					nodes.add(new StagedNode(repository, StagedNode.State.CONFLICTING, file));
			} else {
				for (String file : indexDiff.getAdded())
					nodes.add(new StagedNode(repository, StagedNode.State.ADDED, file));
				for (String file : indexDiff.getChanged())
					nodes.add(new StagedNode(repository, StagedNode.State.CHANGED, file));
				for (String file : indexDiff.getRemoved())
					nodes.add(new StagedNode(repository, StagedNode.State.REMOVED, file));
			}
			content = nodes.toArray(new StagedNode[nodes.size()]);
		}
	}

	public void dispose() {
		// nothing to dispose
	}
}
