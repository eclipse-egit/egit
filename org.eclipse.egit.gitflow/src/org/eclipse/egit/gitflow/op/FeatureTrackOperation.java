/*******************************************************************************
 * Copyright (C) 2015, 2016 Max Hohenegger <eclipse@hohenegger.eu> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.Activator.error;
import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.FetchResult;

/**
 * git flow feature track
 */
public final class FeatureTrackOperation extends AbstractFeatureOperation {
	private static final String REMOTE_ORIGIN_FEATURE_PREFIX = R_REMOTES
			+ DEFAULT_REMOTE_NAME + SEP;

	private Ref remoteFeature;

	private FetchResult operationResult;

	private int timeout;

	/**
	 * Track given ref, referencing a feature branch.
	 *
	 * @param repository
	 * @param ref
	 * @param timeout
	 *            timeout in seconds for remote operations
	 * @since 4.2
	 */
	public FeatureTrackOperation(GitFlowRepository repository, Ref ref,
			int timeout) {
		this(repository, ref, ref.getName().substring(
				(REMOTE_ORIGIN_FEATURE_PREFIX + repository.getConfig()
						.getFeaturePrefix()).length()));
		this.timeout = timeout;
	}

	/**
	 * Track given feature branch locally as newLocalBranch.
	 *
	 * @param repository
	 * @param ref
	 * @param newLocalBranch
	 */
	public FeatureTrackOperation(GitFlowRepository repository, Ref ref,
			String newLocalBranch) {
		super(repository, newLocalBranch);
		this.remoteFeature = ref;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 3);
		try {
			String newLocalBranch = repository
					.getConfig().getFeatureBranchName(featureName);
			operationResult = fetch(progress.newChild(1), timeout);

			if (repository.hasBranch(newLocalBranch)) {
				String errorMessage = String.format(
						CoreText.FeatureTrackOperation_localBranchExists,
						newLocalBranch);
				throw new CoreException(error(errorMessage));
			}
			CreateLocalBranchOperation createLocalBranchOperation = new CreateLocalBranchOperation(
					repository.getRepository(), newLocalBranch, remoteFeature,
					BranchRebaseMode.NONE);
			createLocalBranchOperation.execute(progress.newChild(1));

			BranchOperation branchOperation = new BranchOperation(
					repository.getRepository(), newLocalBranch);
			branchOperation.execute(progress.newChild(1));
			CheckoutResult result = branchOperation.getResult();
			if (!Status.OK.equals(result.getStatus())) {
				String errorMessage = String.format(
						CoreText.FeatureTrackOperation_checkoutReturned,
						newLocalBranch, result.getStatus().name());
				throw new CoreException(error(errorMessage));
			}

			try {
				repository.setRemote(newLocalBranch, DEFAULT_REMOTE_NAME);
				repository.setUpstreamBranchName(newLocalBranch,
						repository.getConfig().getFullFeatureBranchName(featureName));
			} catch (IOException e) {
				throw new CoreException(error(
						CoreText.FeatureTrackOperation_unableToStoreGitConfig,
						e));
			}
		} catch (URISyntaxException e) {
			throw new CoreException(error(e.getMessage(), e));
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			throw new CoreException(error(targetException.getMessage(),
					targetException));
		} catch (GitAPIException e) {
			throw new CoreException(error(e.getMessage(), e));
		}

	}

	/**
	 * @return result set after operation was executed
	 */
	public FetchResult getOperationResult() {
		return operationResult;
	}
}
