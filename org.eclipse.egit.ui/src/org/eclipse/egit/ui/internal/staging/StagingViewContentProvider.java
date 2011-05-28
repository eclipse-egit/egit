/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.egit.ui.internal.staging.StagingView.StagingViewUpdate;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;

/**
 * ContentProvider for staged and unstaged table nodes
 */
public class StagingViewContentProvider implements
		IStructuredContentProvider {
	private StagingEntry[] content;
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
			StagingViewUpdate update = (StagingViewUpdate)newInput;

			if (update.repository == null || update.indexDiff == null)
				return;

			Set<StagingEntry> nodes = new TreeSet<StagingEntry>(new Comparator<StagingEntry>() {
				public int compare(StagingEntry o1, StagingEntry o2) {
					return o1.getPath().compareTo(o2.getPath());
				}
			});

			if (update.changedResources != null) {
				nodes.addAll(Arrays.asList(content));
				for (String res : update.changedResources)
					for (StagingEntry entry : content)
						if (entry.getPath().equals(res))
							nodes.remove(entry);
			}

			final IndexDiff indexDiff = update.indexDiff;
			final Repository repository = update.repository;
			if (isWorkspace) {
				for (String file : indexDiff.getMissing())
					nodes.add(new StagingEntry(repository, StagingEntry.State.MISSING, file));
				for (String file : indexDiff.getModified()) {
					if (indexDiff.getChanged().contains(file))
						nodes.add(new StagingEntry(repository, StagingEntry.State.PARTIALLY_MODIFIED, file));
					else
						nodes.add(new StagingEntry(repository, StagingEntry.State.MODIFIED, file));
				}
				for (String file : indexDiff.getUntracked())
					nodes.add(new StagingEntry(repository, StagingEntry.State.UNTRACKED, file));
				for (String file : indexDiff.getConflicting())
					nodes.add(new StagingEntry(repository, StagingEntry.State.CONFLICTING, file));
			} else {
				for (String file : indexDiff.getAdded())
					nodes.add(new StagingEntry(repository, StagingEntry.State.ADDED, file));
				for (String file : indexDiff.getChanged())
					nodes.add(new StagingEntry(repository, StagingEntry.State.CHANGED, file));
				for (String file : indexDiff.getRemoved())
					nodes.add(new StagingEntry(repository, StagingEntry.State.REMOVED, file));
			}
			content = nodes.toArray(new StagingEntry[nodes.size()]);
		}
	}

	public void dispose() {
		// nothing to dispose
	}
}
