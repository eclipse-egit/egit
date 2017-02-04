/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.osgi.util.NLS;

/**
 * git flow release finish
 */
public final class ReleaseFinishOperation extends AbstractReleaseOperation {
	/**
	 * finish given release
	 *
	 * @param repository
	 * @param releaseName
	 */
	public ReleaseFinishOperation(GitFlowRepository repository,
			String releaseName) {
		super(repository, releaseName);
	}

	/**
	 * finish current release
	 *
	 * @param repository
	 * @throws WrongGitFlowStateException
	 * @throws CoreException
	 * @throws IOException
	 */
	public ReleaseFinishOperation(GitFlowRepository repository)
			throws WrongGitFlowStateException, CoreException, IOException {
		this(repository, getReleaseName(repository));
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		String releaseBranchName = repository.getConfig().getReleaseBranchName(versionName);
		String master = repository.getConfig().getMaster();
		SubMonitor progress = SubMonitor.convert(monitor, 3);
		mergeResult = mergeTo(progress.newChild(1), releaseBranchName, master,
				false /* TODO */, false);
		if (!mergeResult.getMergeStatus().isSuccessful()) {
			// problems during merge to master => this repository is not in a healthy state
			return;
		}

		// this may result in conflicts, but that's ok
		safeCreateTag(progress.newChild(1),
				repository.getConfig().getVersionTagPrefix() + versionName,
				NLS.bind(CoreText.ReleaseFinishOperation_releaseOf, versionName));

		finish(progress.newChild(1), releaseBranchName,
				false /* TODO: squash should also be supported for releases */,
				false /* TODO: keep should also be supported for releases */,
				false);
	}
}
