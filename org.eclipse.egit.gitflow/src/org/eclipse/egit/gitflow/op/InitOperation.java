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
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;

import static org.eclipse.egit.gitflow.Activator.error;
import static org.eclipse.egit.gitflow.GitFlowDefaults.*;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.internal.CoreText;

import static org.eclipse.egit.gitflow.GitFlowRepository.*;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * git flow init
 */
@SuppressWarnings("restriction")
public final class InitOperation extends GitFlowOperation {
	private String develop;

	private String master;

	private String feature;

	private String release;

	private String hotfix;

	private String versionTag;

	/**
	 * @param jGitRepository
	 * @param develop
	 * @param master
	 * @param feature
	 * @param release
	 * @param hotfix
	 * @param versionTag
	 */
	public InitOperation(Repository jGitRepository, String develop,
			String master, String feature, String release, String hotfix,
			String versionTag) {
		super(new GitFlowRepository(jGitRepository));
		this.develop = develop;
		this.master = master;
		this.feature = feature;
		this.release = release;
		this.hotfix = hotfix;
		this.versionTag = versionTag;
	}

	/**
	 * use default prefixes and names
	 * 
	 * @param repository
	 */
	public InitOperation(Repository repository) {
		this(repository, DEVELOP, MASTER, FEATURE_PREFIX, RELEASE_PREFIX,
				HOTFIX_PREFIX);
	}

	/**
	 * @param repository
	 * @param develop
	 * @param master
	 * @param featurePrefix
	 * @param releasePrefix
	 * @param hotfixPrefix
	 */
	public InitOperation(Repository repository, String develop, String master,
			String featurePrefix, String releasePrefix, String hotfixPrefix) {
		this(repository, develop, master, featurePrefix, releasePrefix,
				hotfixPrefix, VERSION_TAG);
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			setPrefixes(feature, release, hotfix, versionTag);
			setBranches(develop, master);
			repository.getRepository().getConfig().save();
		} catch (IOException e) {
			throw new CoreException(error(e.getMessage(), e));
		}

		if (!repository.hasBranches()) {
			new CommitOperation(repository.getRepository(),
					repository.getUser(), repository.getUser(),
					CoreText.InitOperation_initialCommit).execute(monitor);
		}

		try {
			if (!repository.hasBranch(develop)) {
				CreateLocalBranchOperation branchFromHead = createBranchFromHead(
						develop, repository.findHead());
				branchFromHead.execute(monitor);
				BranchOperation checkoutOperation = new BranchOperation(
						repository.getRepository(), develop);
				checkoutOperation.execute(monitor);
			}
		} catch (GitAPIException e) {
			throw new CoreException(error(e.getMessage(), e));
		}
	}

	private void setPrefixes(String feature, String release, String hotfix,
			String versionTag) {
		repository.setPrefix(FEATURE_KEY, feature);
		repository.setPrefix(RELEASE_KEY, release);
		repository.setPrefix(HOTFIX_KEY, hotfix);
		repository.setPrefix(VERSION_TAG_KEY, versionTag);
	}

	private void setBranches(String develop, String master) {
		repository.setBranch(DEVELOP_KEY, develop);
		repository.setBranch(MASTER_KEY, master);
	}
}
