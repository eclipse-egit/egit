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
import static org.eclipse.egit.gitflow.GitFlowConfig.DEVELOP_KEY;
import static org.eclipse.egit.gitflow.GitFlowConfig.FEATURE_KEY;
import static org.eclipse.egit.gitflow.GitFlowConfig.HOTFIX_KEY;
import static org.eclipse.egit.gitflow.GitFlowConfig.MASTER_KEY;
import static org.eclipse.egit.gitflow.GitFlowConfig.RELEASE_KEY;
import static org.eclipse.egit.gitflow.GitFlowConfig.VERSION_TAG_KEY;
import static org.eclipse.egit.gitflow.GitFlowDefaults.VERSION_TAG;
import static org.eclipse.jgit.lib.Constants.R_HEADS;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.gitflow.GitFlowConfig;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.InitParameters;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;

/**
 * git flow init
 */
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
	 * @deprecated Use {@code InitOperation#InitOperation(Repository, InitParameters)} instead.
	 */
	@Deprecated
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
	 * @param jGitRepository
	 * @param parameters
	 * @since 4.1
	 */
	public InitOperation(@NonNull Repository jGitRepository,
			@NonNull InitParameters parameters) {
		super(new GitFlowRepository(jGitRepository));
		this.develop = parameters.getDevelop();
		this.master = parameters.getMaster();
		this.feature = parameters.getFeature();
		this.release = parameters.getRelease();
		this.hotfix = parameters.getHotfix();
		this.versionTag = parameters.getVersionTag();
	}

	/**
	 * use default prefixes and names
	 *
	 * @param repository
	 */
	public InitOperation(Repository repository) {
		this(repository, new InitParameters());
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

		SubMonitor progress = SubMonitor.convert(monitor, 3);
		if (!repository.hasBranches()) {
			new CommitOperation(repository.getRepository(),
					repository.getConfig().getUser(), repository.getConfig().getUser(),
					CoreText.InitOperation_initialCommit)
							.execute(progress.newChild(1));
		}

		try {
			if (!isMasterBranchAvailable()) {
				throw new CoreException(error(NLS.bind(CoreText.InitOperation_localMasterDoesNotExist, master)));
			}

			RevCommit head = repository.findHead();
			if (!repository.hasBranch(develop)) {
				CreateLocalBranchOperation branchFromHead = createBranchFromHead(
						develop, head);
				branchFromHead.execute(progress.newChild(1));
				BranchOperation checkoutOperation = new BranchOperation(
						repository.getRepository(), develop);
				checkoutOperation.execute(progress.newChild(1));
			}
		} catch (WrongGitFlowStateException e) {
			throw new CoreException(error(e));
		} catch (GitAPIException e) {
			throw new CoreException(error(e.getMessage(), e));
		}
	}

	private void setPrefixes(String feature, String release, String hotfix,
			String versionTag) {
		GitFlowConfig config = repository.getConfig();
		config.setPrefix(FEATURE_KEY, feature);
		config.setPrefix(RELEASE_KEY, release);
		config.setPrefix(HOTFIX_KEY, hotfix);
		config.setPrefix(VERSION_TAG_KEY, versionTag);
	}

	private void setBranches(String develop, String master) {
		GitFlowConfig config = repository.getConfig();
		config.setBranch(DEVELOP_KEY, develop);
		config.setBranch(MASTER_KEY, master);
	}

	private boolean isMasterBranchAvailable() throws CoreException {
		try {
			return repository.getRepository().exactRef(R_HEADS + master) != null;
		} catch (IOException e) {
			throw new CoreException(error(e.getMessage(), e));
		}
	}
}
