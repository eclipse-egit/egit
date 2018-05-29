/*******************************************************************************
 * Copyright (C) 2011, Philipp Thun <philipp.thun@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Helper class to create decoratable resources
 *
 * @see IDecoratableResource
 */
public class DecoratableResourceHelper {

	/**
	 * Maps repository to the branch state. The entries are removed each time
	 * {@link IndexDiffData} changes
	 *
	 * @see GitLightweightDecorator#indexDiffChanged(Repository,
	 *      org.eclipse.egit.core.internal.indexdiff.IndexDiffData)
	 */
	private static Map<Repository, String> branchState = Collections
			.synchronizedMap(new WeakHashMap<Repository, String>());

	static String getRepositoryName(Repository repository) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		RepositoryState state = repository.getRepositoryState();
		if (state != RepositoryState.SAFE)
			return repoName + '|' + state.getDescription();
		else
			return repoName;
	}

	static String getShortBranch(Repository repository) throws IOException {
		return Activator.getDefault().getRepositoryUtil()
				.getShortBranch(repository);
	}

	static RevCommit getHeadCommit(Repository repository) {
		return Activator.getDefault().getRepositoryUtil()
				.parseHeadCommit(repository);
	}

	static String getBranchStatus(Repository repo) throws IOException {
		String cachedStatus = branchState.get(repo);
		if (cachedStatus != null)
			return cachedStatus;

		String branchName = repo.getBranch();
		if (branchName == null)
			return null;

		BranchTrackingStatus status = BranchTrackingStatus.of(repo, branchName);
		if (status == null)
			return null;

		if (status.getAheadCount() == 0 && status.getBehindCount() == 0)
			return null;

		String formattedStatus = GitLabels.formatBranchTrackingStatus(status);
		branchState.put(repo, formattedStatus);
		return formattedStatus;
	}

	static void clearState(Repository repo) {
		branchState.remove(repo);
	}
}
