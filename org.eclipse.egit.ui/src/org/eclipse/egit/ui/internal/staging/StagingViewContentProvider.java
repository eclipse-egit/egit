/*******************************************************************************
 * Copyright (C) 2011, 2013 Bernard Leach <leachbj@bouncycastle.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.ADDED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.CHANGED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.CONFLICTING;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MISSING;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MISSING_AND_CHANGED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.MODIFIED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.PARTIALLY_MODIFIED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.REMOVED;
import static org.eclipse.egit.ui.internal.staging.StagingEntry.State.UNTRACKED;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.staging.StagingView.StagingViewUpdate;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;

/**
 * ContentProvider for staged and unstaged table nodes
 */
public class StagingViewContentProvider implements
		IStructuredContentProvider {
	private StagingEntry[] content = new StagingEntry[0];
	private boolean isWorkspace;

	StagingViewContentProvider(boolean workspace) {
		this.isWorkspace = workspace;
	}

	public Object[] getElements(Object inputElement) {
		return content;
	}

	StagingEntry[] getStagingEntries() {
		return content;
	}

	public void inputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		if (!(newInput instanceof StagingViewUpdate))
			return;

		StagingViewUpdate update = (StagingViewUpdate) newInput;

		if (update.repository == null || update.indexDiff == null) {
			content = new StagingEntry[0];
			return;
		}

		Set<StagingEntry> nodes = new TreeSet<StagingEntry>(
				new Comparator<StagingEntry>() {
					public int compare(StagingEntry o1, StagingEntry o2) {
						return o1.getPath().compareTo(o2.getPath());
					}
				});

		if (update.changedResources != null
				&& !update.changedResources.isEmpty()) {
			nodes.addAll(Arrays.asList(content));
			for (String res : update.changedResources)
				for (StagingEntry entry : content)
					if (entry.getPath().equals(res))
						nodes.remove(entry);
		}

		final IndexDiffData indexDiff = update.indexDiff;
		final Repository repository = update.repository;
		if (isWorkspace) {
			for (String file : indexDiff.getMissing())
				if (indexDiff.getChanged().contains(file))
					nodes.add(new StagingEntry(repository, MISSING_AND_CHANGED,
							file));
				else
					nodes.add(new StagingEntry(repository, MISSING, file));
			for (String file : indexDiff.getModified())
				if (indexDiff.getChanged().contains(file))
					nodes.add(new StagingEntry(repository, PARTIALLY_MODIFIED,
							file));
				else
					nodes.add(new StagingEntry(repository, MODIFIED, file));
			for (String file : indexDiff.getUntracked())
				nodes.add(new StagingEntry(repository, UNTRACKED, file));
			for (String file : indexDiff.getConflicting())
				nodes.add(new StagingEntry(repository, CONFLICTING, file));
		} else {
			for (String file : indexDiff.getAdded())
				nodes.add(new StagingEntry(repository, ADDED, file));
			for (String file : indexDiff.getChanged())
				nodes.add(new StagingEntry(repository, CHANGED, file));
			for (String file : indexDiff.getRemoved())
				nodes.add(new StagingEntry(repository, REMOVED, file));
		}

		try {
		SubmoduleWalk walk = SubmoduleWalk.forIndex(repository);
		while(walk.next())
			for (StagingEntry entry : nodes)
				entry.setSubmodule(entry.getPath().equals(walk.getPath()));
		} catch(IOException e) {
			Activator.error(UIText.StagingViewContentProvider_SubmoduleError, e);
		}

		content = nodes.toArray(new StagingEntry[nodes.size()]);
	}

	public void dispose() {
		// nothing to dispose
	}
}
