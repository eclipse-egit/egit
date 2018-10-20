/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Commit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelWorkingTree;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * Ensure proper ordering of nodes in Git Change Set.
 *
 * Working Tree node will be always first in Change Set, then comes node with
 * Staged changes, after that will be shown list of commits ordered by commit
 * date.
 *
 * Elements that represents files will be always put after container elements
 * (like commit or folder). In other cases elements will be sorted in
 * alphabetical order.
 *
 */
// TODO extend ViewerComparator as soon as minimum platform version is 4.7
// (Oxygen)
@SuppressWarnings("deprecation") // used as navigator commonSorter extension
public class GitChangeSetSorter extends ViewerSorter {

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof GitModelBlob && !(e2 instanceof GitModelBlob))
			return 1;

		if (e2 instanceof GitModelBlob && !(e1 instanceof GitModelBlob))
			return -1;

		if (e1 instanceof GitModelWorkingTree)
			return -1;

		if (e2 instanceof GitModelWorkingTree)
			return 1;

		if (e1 instanceof GitModelCache)
			return -2;

		if (e2 instanceof GitModelCache)
			return 2;

		if ((e1 instanceof GitModelTree && e2 instanceof GitModelTree) ||
				(e1 instanceof GitModelBlob && e2 instanceof GitModelBlob))
			return super.compare(viewer, e1, e2);

		if (e1 instanceof GitModelTree && e2 instanceof GitModelCommit)
			return 1;

		if (e1 instanceof GitModelCommit && e2 instanceof GitModelCommit) {
			Commit rc1 = ((GitModelCommit) e1).getCachedCommitObj();
			Commit rc2 = ((GitModelCommit) e2).getCachedCommitObj();

			return rc2.getCommitDate().compareTo(rc1.getCommitDate());
		}

		return super.compare(viewer, e1, e2);
	}

}
