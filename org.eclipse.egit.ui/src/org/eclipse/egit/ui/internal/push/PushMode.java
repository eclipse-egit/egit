/*******************************************************************************
 * Copyright (C) 2015, Frank Jakob
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Push mode: either push to upstream, or push to Gerrit.
 */
public enum PushMode {
	/** Push to upstream. */
	UPSTREAM {
		@Override
		public Wizard getWizard(@NonNull Repository repository,
				RevCommit commit)
				throws IOException {
			String fullBranch = repository.getFullBranch();
			if (fullBranch != null
					&& fullBranch.startsWith(Constants.R_HEADS)) {
				Ref ref = repository.exactRef(fullBranch);
				return new PushBranchWizard(repository, ref);
			} else if (commit != null) {
				return new PushBranchWizard(repository, commit.getId());
			} else {
				ObjectId head = repository.resolve(Constants.HEAD);
				if (head != null) {
					return new PushBranchWizard(repository, head);
				}
			}
			return null;
		}
	},

	/** Push to Gerrit. */
	GERRIT {
		@Override
		public Wizard getWizard(@NonNull Repository repository,
				RevCommit commit)
				throws IOException {
			Ref ref = repository.exactRef(Constants.HEAD);
			if (ref != null) {
				return new PushToGerritWizard(repository);
			}
			return null;
		}
	};

	/**
	 * Determines a {@link Wizard} suitable for the {@link PushMode}.
	 *
	 * @param repository
	 *            to push to
	 * @param commit
	 *            to push
	 * @return a {@link Wizard}, or {@code null} if the repo has no HEAD
	 * @throws IOException
	 *             if some I/O problem prevent reading information, for instance
	 *             from a git config file
	 */
	public abstract Wizard getWizard(@NonNull Repository repository,
			RevCommit commit) throws IOException;
}
