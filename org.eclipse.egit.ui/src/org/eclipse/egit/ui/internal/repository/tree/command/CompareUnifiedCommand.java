/*******************************************************************************
 * Copyright (C) 2020, Alexander Nittka <alex@nittka.de>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.commit.command.UnifiedDiffHandler;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Compares the commits referenced by two refs as unified diff.
 */
public class CompareUnifiedCommand extends CompareCommand {

	@Override
	protected void compare(IWorkbenchPage page, Repository repo,
			String compareCommit, String baseCommit) throws ExecutionException {
		try {
			RevCommit base = repo.parseCommit(ObjectId.fromString(baseCommit));
			RevCommit tip = repo
					.parseCommit(ObjectId.fromString(compareCommit));
			UnifiedDiffHandler.show(new RepositoryCommit(repo, tip),
					new RepositoryCommit(repo, base));
		} catch (Exception e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
	}
}
