/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.Activator.error;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.osgi.util.NLS;

/**
 * git flow hotfix finish
 */
public final class HotfixFinishOperation extends AbstractHotfixOperation {
	private MergeResult mergeResult;

	/**
	 * finish given hotfix branch
	 *
	 * @param repository
	 * @param hotfixName
	 */
	public HotfixFinishOperation(GitFlowRepository repository, String hotfixName) {
		super(repository, hotfixName);
	}

	/**
	 * finish current hotfix branch
	 *
	 * @param repository
	 * @throws WrongGitFlowStateException
	 * @throws CoreException
	 * @throws IOException
	 */
	public HotfixFinishOperation(GitFlowRepository repository)
			throws WrongGitFlowStateException, CoreException, IOException {
		this(repository, getHotfixName(repository));
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		String hotfixBranchName = repository.getConfig().getHotfixBranchName(versionName);
		mergeResult = mergeTo(monitor, hotfixBranchName,
				repository.getConfig().getMaster());
		if (!mergeResult.getMergeStatus().isSuccessful()) {
			throw new CoreException(
					error(CoreText.HotfixFinishOperation_mergeFromHotfixToMasterFailed));
		}

		mergeResult = finish(monitor, hotfixBranchName);
		if (!mergeResult.getMergeStatus().isSuccessful()) {
			return;
		}

		safeCreateTag(monitor, versionName,
				NLS.bind(CoreText.HotfixFinishOperation_hotfix, versionName));
	}

	/**
	 * @return result set after operation was executed
	 */
	public MergeResult getOperationResult() {
		return mergeResult;
	}
}
