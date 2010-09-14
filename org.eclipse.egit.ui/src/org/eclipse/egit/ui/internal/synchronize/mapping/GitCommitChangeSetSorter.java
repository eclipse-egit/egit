/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelWorkingTree;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Sorter that order commits in Git Change Set by commit time;
 */
public class GitCommitChangeSetSorter extends ViewerSorter {

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof GitModelWorkingTree)
			return -1;

		if (e2 instanceof GitModelWorkingTree)
			return 1;

		if (e1 instanceof GitModelCache)
			return -2;

		if (e2 instanceof GitModelCache)
			return 2;

		if (e1 instanceof GitModelCommit && e2 instanceof GitModelCommit) {
			RevCommit rc1 = ((GitModelCommit) e1).getRemoteCommit();
			RevCommit rc2 = ((GitModelCommit) e2).getRemoteCommit();

			return rc2.getCommitTime() - rc1.getCommitTime();
		}

		return super.compare(viewer, e1, e2);
	}

}
