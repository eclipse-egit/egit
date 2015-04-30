/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import static org.eclipse.egit.gitflow.Activator.error;

/**
 * Checks if name is valid and branch does not exist.
 */
public class BranchNameValidator {
	/**
	 * Characters not allowed in git flow branches.
	 */
	public static final String ILLEGAL_CHARS = "/ "; //$NON-NLS-1$

	/**
	 * @param repository
	 * @param featureName
	 * @return Whether featureName corresponds to existing branch.
	 * @throws CoreException
	 */
	public static boolean featureExists(GitFlowRepository repository,
			String featureName) throws CoreException {
		return branchExists(repository,
				repository.getFullFeatureBranchName(featureName));
	}

	/**
	 * @param repository
	 * @param hotfixName
	 * @return Whether hotfixName corresponds to existing branch.
	 * @throws CoreException
	 */
	public static boolean hotfixExists(GitFlowRepository repository,
			String hotfixName) throws CoreException {
		return branchExists(repository,
				repository.getFullHotfixBranchName(hotfixName));
	}

	/**
	 * @param repository
	 * @param releaseName
	 * @return Whether releaseName corresponds to existing branch.
	 * @throws CoreException
	 */
	public static boolean releaseExists(GitFlowRepository repository,
			String releaseName) throws CoreException {
		return branchExists(repository,
				repository.getFullReleaseBranchName(releaseName));
	}

	private static boolean branchExists(GitFlowRepository repository,
			String fullBranchName) throws CoreException {
		List<Ref> branches;
		try {
			branches = Git.wrap(repository.getRepository()).branchList().call();
		} catch (GitAPIException e) {
			throw new CoreException(error(e.getMessage(), e));
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
	 * @return Whether or not name would be a valid name for a branch.
	 */
	public static boolean isBranchNameValid(String name) {
		if (name.isEmpty()) {
			return false;
		}

		for (int i = 0; i < ILLEGAL_CHARS.length(); i++) {
			char illegalChar = ILLEGAL_CHARS.charAt(i);
			if (name.contains(String.valueOf(illegalChar))) {
				return false;
			}
		}

		return true;
	}
}
