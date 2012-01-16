/*******************************************************************************
 * Copyright (C) 2011, Philipp Thun <philipp.thun@sap.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

/**
 * Helper class to create decoratable resources
 *
 * @see IDecoratableResource
 */
public class DecoratableResourceHelper {

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
		Ref head = repository.getRef(Constants.HEAD);
		if (head != null && !head.isSymbolic()) {
			String refString = Activator
					.getDefault()
					.getRepositoryUtil()
					.mapCommitToRef(repository, repository.getFullBranch(),
							false);
			if (refString != null) {
				return repository.getFullBranch().substring(0, 7)
						+ "... (" + refString + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			} else
				return repository.getFullBranch().substring(0, 7) + "..."; //$NON-NLS-1$
		}

		if (head == null || head.getObjectId() == null)
			return UIText.DecoratableResourceHelper_noHead;

		return repository.getBranch();
	}

	static String getBranchStatus(Repository repo) throws IOException {
		String branchName = repo.getBranch();
		if (branchName == null)
			return null;

		BranchTrackingStatus status = BranchTrackingStatus.of(repo, branchName);
		if (status == null)
			return null;

		if (status.getAheadCount() == 0 && status.getBehindCount() == 0)
			return null;

		String formattedStatus = GitLabelProvider.formatBranchTrackingStatus(status);
		return formattedStatus;
	}
}
