/*******************************************************************************
 * Copyright (c) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.jobs;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.action.Action;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.util.FS;

/**
 * An {@link Action} to report some background job result.
 */
public abstract class RepositoryJobResultAction extends Action {

	/**
	 * Because this action may be associated with a job and may be invoked long
	 * after the repository operation had been performed we do not keep a
	 * reference to the {@link Repository} here. The repository might have been
	 * deleted in the meantime.
	 */
	private final File repositoryDir;

	private boolean repositoryGone;

	/**
	 * Creates a new {@link RepositoryJobResultAction}.
	 *
	 * @param repository
	 *            the result belongs to
	 * @param title
	 *            of the action
	 */
	public RepositoryJobResultAction(@NonNull Repository repository,
			String title) {
		super(title);
		this.repositoryDir = repository.getDirectory();
	}

	@Override
	public final void run() {
		Repository repo = null;
		if (!repositoryGone) {
			RepositoryCache repoCache = org.eclipse.egit.core.Activator
					.getDefault().getRepositoryCache();
			repo = repoCache.getRepository(repositoryDir);
			if (repo == null
					&& FileKey.isGitRepository(repositoryDir, FS.DETECTED)) {
				// No longer in the Egit cache but still on disk
				try {
					repo = repoCache.lookupRepository(repositoryDir);
				} catch (IOException e) {
					// Ignore, repo remains null
				}
			}
			repositoryGone = repo == null;
		}
		if (repositoryGone || repo == null) {
			Activator.showError(MessageFormat.format(
					UIText.RepositoryJobResultAction_RepositoryGone,
					repositoryDir), null);
			return;
		}
		showResult(repo);
	}

	/**
	 * Shows the job result to the user.
	 *
	 * @param repository
	 *            the result belongs to
	 */
	abstract protected void showResult(@NonNull Repository repository);
}
