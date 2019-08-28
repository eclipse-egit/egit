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

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Wrapper for JGit repository.
 *
 * @since 4.0
 */
public class GitFlowRepository {

	private Repository repository;

	/**
	 * @param repository
	 */
	public GitFlowRepository(@NonNull Repository repository) {
		this.repository = repository;
	}

	/**
	 * @return Whether or not this repository has branches.
	 */
	public boolean hasBranches() {
		List<Ref> branches;
		try {
			branches = Git.wrap(repository).branchList().call();
			return !branches.isEmpty();
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param branch
	 * @return Whether or not branch exists in this repository.
	 * @throws GitAPIException
	 */
	public boolean hasBranch(String branch) throws GitAPIException {
		String fullBranchName = R_HEADS + branch;
		List<Ref> branchList = Git.wrap(repository).branchList().call();
		for (Ref ref : branchList) {
			if (fullBranchName.equals(ref.getTarget().getName())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @param branchName
	 * @return Ref for branchName.
	 * @throws IOException
	 */
	public Ref findBranch(String branchName) throws IOException {
		return repository.exactRef(R_HEADS + branchName);
	}

	/**
	 * @return current branch has feature-prefix?
	 * @throws IOException
	 */
	public boolean isFeature() throws IOException {
		String branch = repository.getBranch();
		return branch != null
				&& branch.startsWith(getConfig().getFeaturePrefix());
	}

	/**
	 * @return branch name has name "develop"?
	 * @throws IOException
	 */
	public boolean isDevelop() throws IOException {
		String branch = repository.getBranch();
		return branch != null && branch.equals(getConfig().getDevelop());
	}

	/**
	 * @return branch name has name "master"?
	 * @throws IOException
	 */
	public boolean isMaster() throws IOException {
		String branch = repository.getBranch();
		return branch != null && branch.equals(getConfig().getMaster());
	}

	/**
	 * @return current branch has release prefix?
	 * @throws IOException
	 */
	public boolean isRelease() throws IOException {
		String branch = repository.getBranch();
		return branch != null
				&& branch.startsWith(getConfig().getReleasePrefix());
	}

	/**
	 * @return current branch has hotfix prefix?
	 * @throws IOException
	 */
	public boolean isHotfix() throws IOException {
		String branch = repository.getBranch();
		return branch != null
				&& branch.startsWith(getConfig().getHotfixPrefix());
	}

	/**
	 * @return HEAD commit
	 * @throws WrongGitFlowStateException
	 */
	public RevCommit findHead() throws WrongGitFlowStateException {
		try (RevWalk walk = new RevWalk(repository)) {
			try {
				ObjectId head = repository.resolve(HEAD);
				if (head == null) {
					throw new WrongGitFlowStateException(
							CoreText.GitFlowRepository_gitFlowRepositoryMayNotBeEmpty);
				}
				return walk.parseCommit(head);
			} catch (MissingObjectException e) {
				throw new WrongGitFlowStateException(CoreText.GitFlowRepository_gitFlowRepositoryMayNotBeEmpty);
			} catch (RevisionSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @param branchName
	 * @return HEAD commit on branch branchName or {@literal null} if
	 *         {@code branchName} could not be resolved.
	 */
	public @Nullable RevCommit findHead(String branchName) {
		try (RevWalk walk = new RevWalk(repository)) {
			try {
				String revstr = R_HEADS + branchName;
				ObjectId head = repository.resolve(revstr);
				if (head == null) {
					return null;
				}
				return walk.parseCommit(head);
			} catch (RevisionSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @param sha1
	 * @return Commit for SHA1
	 */
	public RevCommit findCommit(String sha1) {
		try (RevWalk walk = new RevWalk(repository)) {
			try {
				ObjectId head = repository.resolve(sha1);
				return walk.parseCommit(head);
			} catch (RevisionSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	/**
	 * @return JGit repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @return git flow feature branches
	 */
	public List<Ref> getFeatureBranches() {
		return getPrefixBranches(R_HEADS + getConfig().getFeaturePrefix());
	}

	/**
	 * @return git flow release branches
	 */
	public List<Ref> getReleaseBranches() {
		return getPrefixBranches(R_HEADS + getConfig().getReleasePrefix());
	}

	/**
	 * @return git flow hotfix branches
	 */
	public List<Ref> getHotfixBranches() {
		return getPrefixBranches(R_HEADS + getConfig().getHotfixPrefix());
	}

	private List<Ref> getPrefixBranches(String prefix) {
		try {
			List<Ref> branches = Git.wrap(repository).branchList().call();
			List<Ref> prefixBranches = new ArrayList<>();
			for (Ref ref : branches) {
				if (ref.getName().startsWith(prefix)) {
					prefixBranches.add(ref);
				}
			}

			return prefixBranches;
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param ref
	 * @return branch name for ref
	 */
	public String getFeatureBranchName(Ref ref) {
		return ref.getName().substring(
				(R_HEADS + getConfig().getFeaturePrefix()).length());
	}

	/**
	 * @param tagName
	 * @return commit tag tagName points to
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	public RevCommit findCommitForTag(String tagName)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		try (RevWalk revWalk = new RevWalk(repository)) {
			Ref tagRef = repository.exactRef(R_TAGS + tagName);
			if (tagRef == null) {
				return null;
			}
			return revWalk.parseCommit(tagRef.getObjectId());
		}
	}

	/**
	 * @param featureName
	 * @param value
	 * @throws IOException
	 */
	public void setRemote(String featureName, String value) throws IOException {
		getConfig().setRemote(featureName, value);
	}

	/**
	 * @param featureName
	 * @param value
	 * @throws IOException
	 */
	public void setUpstreamBranchName(String featureName, String value) throws IOException {
		getConfig().setUpstreamBranchName(featureName, value);
	}

	/**
	 * @param featureName
	 * @return Upstream branch name
	 */
	public String getUpstreamBranchName(String featureName) {
		return getConfig().getUpstreamBranchName(featureName);
	}

	/**
	 * @return the configuration of this repository
	 */
	public GitFlowConfig getConfig() {
		return new GitFlowConfig(repository.getConfig());
	}

	/**
	 * Check if the given commit is an ancestor of the current HEAD on the
	 * develop branch.
	 *
	 * @param selectedCommit
	 * @return Whether or not the selected commit is on the develop branch.
	 * @throws IOException
	 * @since 4.3
	 */
	public boolean isOnDevelop(@NonNull RevCommit selectedCommit) throws IOException {
		String develop = getConfig().getDevelopFull();
		return isOnBranch(selectedCommit, develop);
	}

	private boolean isOnBranch(RevCommit commit, String fullBranch)
			throws IOException {
		Ref branchRef = repository.exactRef(fullBranch);
		if (branchRef == null) {
			return false;
		}
		try {
			List<Ref> list = Git.wrap(repository).branchList().setContains(commit.name()).call();

			return list.contains(branchRef);
		} catch (GitAPIException e) {
			// ListBranchCommand can only throw a wrapped IOException
			throw new IOException(e);
		}
	}
}
