/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;

/**
 * Checks if name is valid and branch does not exist.
 *
 * @since 4.0
 */
@Deprecated(since = "5.3.0")
public class BranchNameValidator {
	/**
	 * Characters not allowed in git flow branches.
	 */
	@Deprecated(since = "5.3.0")
	public static final String ILLEGAL_CHARS = "/ "; //$NON-NLS-1$

	/**
	 * @param repository
	 * @param featureName
	 * @return Whether featureName corresponds to existing branch.
	 * @throws CoreException
	 */
	@Deprecated(since = "5.3.0")
	public static boolean featureExists(GitFlowRepository repository,
			String featureName) throws CoreException {
		return branchExists(repository,
				repository.getConfig().getFullFeatureBranchName(featureName));
	}

	/**
	 * @param repository
	 * @param hotfixName
	 * @return Whether hotfixName corresponds to existing branch.
	 * @throws CoreException
	 */
	@Deprecated(since = "5.3.0")
	public static boolean hotfixExists(GitFlowRepository repository,
			String hotfixName) throws CoreException {
		return branchExists(repository,
				repository.getConfig().getFullHotfixBranchName(hotfixName));
	}

	/**
	 * @param repository
	 * @param releaseName
	 * @return Whether releaseName corresponds to existing branch.
	 * @throws CoreException
	 */
	@Deprecated(since = "5.3.0")
	public static boolean releaseExists(GitFlowRepository repository,
			String releaseName) throws CoreException {
		return branchExists(repository,
				repository.getConfig().getFullReleaseBranchName(releaseName));
	}

	private static boolean branchExists(GitFlowRepository repository,
			String fullBranchName) throws CoreException {
		List<Ref> branches;
		try {
			branches = Git.wrap(repository.getRepository()).branchList().call();
		} catch (GitAPIException e) {
			throw new CoreException(Activator.error(e.getMessage(), e));
		}
		for (Ref ref : branches) {
			if (fullBranchName.equals(ref.getTarget().getName())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @param name
	 *            Gitflow branch name
	 * @return Whether or not name would be a valid name for a branch.
	 */
	@Deprecated(since = "5.3.0")
	public static boolean isBranchNameValid(String name) {
		return Utils.validateNewRefName(name, Constants.R_HEADS).isOK();
	}
}
