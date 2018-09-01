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
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.Activator.error;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.gitflow.GitFlowConfig;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;

/**
 * git flow release start
 */
public final class ReleaseStartOperation extends AbstractReleaseOperation {
	private String startCommitSha1;

	private boolean isHead;

	/**
	 * start release from given commit
	 *
	 * @param repository
	 * @param startCommitSha1
	 * @param releaseName
	 */
	public ReleaseStartOperation(GitFlowRepository repository,
			String startCommitSha1, String releaseName) {
		this(repository, startCommitSha1, releaseName, isHead(repository, startCommitSha1));
	}

	/**
	 * start release from HEAD
	 *
	 * @param repository
	 * @param releaseName
	 */
	public ReleaseStartOperation(GitFlowRepository repository,
			String releaseName) {
		this(repository, findHead(repository), releaseName, true);
	}

	private ReleaseStartOperation(GitFlowRepository repository,
			String startCommitSha1, String releaseName, boolean isHead) {
		super(repository, releaseName);
		this.startCommitSha1 = startCommitSha1;
		this.isHead = isHead;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		String branchName = repository.getConfig().getReleaseBranchName(versionName);

		try {
			if (releaseExists()) {
				throw new CoreException(
						error(NLS.bind(
								CoreText.ReleaseStartOperation_releaseNameAlreadyExists,
								versionName)));
			}
		} catch (IOException e) {
			throw new CoreException(error(e.getMessage(), e));
		}

		RevCommit commit = repository.findCommit(startCommitSha1);
		if (commit == null) {
			throw new IllegalStateException(NLS.bind(CoreText.StartOperation_unableToFindCommitFor, startCommitSha1));
		}
		start(monitor, branchName, commit);
	}

	/**
	 * @return whether or not the given versionName exists
	 * @throws RevisionSyntaxException
	 * @throws AmbiguousObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	public boolean releaseExists()
			throws RevisionSyntaxException, AmbiguousObjectException,
			IncorrectObjectTypeException, IOException {
		return null != repository.getRepository().resolve(
				R_TAGS + repository.getConfig().getVersionTagPrefix() + versionName);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		if (isHead) {
			return null;
		} else {
			return super.getSchedulingRule();
		}
	}

	private static boolean isHead(final GitFlowRepository gfRepo, final String sha1) {
		try {
			RevCommit head = gfRepo.findHead();
			return sha1.equals(head.getName());
		} catch (WrongGitFlowStateException e) {
			return false;
		}
	}

	private static String findHead(GitFlowRepository repository) {
		GitFlowConfig config = repository.getConfig();
		RevCommit head = repository.findHead(config.getDevelop());
		if (head == null) {
			throw new IllegalStateException(NLS.bind(CoreText.StartOperation_unableToFindCommitFor, config.getDevelop()));
		}
		return head.getName();
	}
}
