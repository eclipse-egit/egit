/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495512
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.credentials.EGitCredentialsProvider;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;

/**
 * UI Wrapper for {@link PushOperation}
 */
public class PushOperationUI {
	private final Repository repository;

	private final boolean dryRun;

	private final String destinationString;

	private final RemoteConfig config;

	private PushOperationSpecification spec;

	private CredentialsProvider credentialsProvider;

	private PushOperation op;

	private final String remoteName;

	private final String branchName;

	private PushOperationResult expectedResult;

	private boolean showConfigureButton = true;

	private @NonNull PushMode pushMode = PushMode.UPSTREAM;

	/**
	 * Push to the given remote using the git configuration.
	 *
	 * @param repository
	 *            to push from
	 * @param remoteName
	 *            {@link RemoteConfig} specifying where to push to
	 * @param dryRun
	 *            whether the push should be a dry run
	 */
	public PushOperationUI(Repository repository, String remoteName,
			boolean dryRun) {
		this.repository = repository;
		this.spec = null;
		this.config = null;
		this.remoteName = remoteName;
		this.branchName = null;
		this.dryRun = dryRun;
		destinationString = MessageFormat.format("{0} - {1}", //$NON-NLS-1$
				repository.getDirectory()
				.getParentFile().getName(), remoteName);
	}

	/**
	 * Push all branches that match a configured push refspec. (Corresponds to
	 * PushDefault.MATCHING.)
	 *
	 * @param repository
	 *            to push from
	 * @param config
	 *            {@link RemoteConfig} specifying where to push to
	 * @param dryRun
	 *            whether the push should be a dry run
	 *
	 */
	public PushOperationUI(Repository repository, RemoteConfig config,
			boolean dryRun) {
		this(repository, null, config, dryRun);
	}

	/**
	 * Push a specific branch. If a tracking branch is configured, pushes to
	 * that, otherwise uses the given branch name also as upstream branch name.
	 * (This is a cross between PushDefault.UPSTREAM and PushDefault.CURRENT.)
	 *
	 * @param repository
	 *            to push from
	 * @param branchName
	 *            full name of the branch to push; may be {@code null} to push
	 *            all branches matching a configured push refspec
	 * @param config
	 *            {@link RemoteConfig} specifying where to push to
	 * @param dryRun
	 *            whether the push should be a dry run
	 */
	public PushOperationUI(Repository repository, String branchName,
			RemoteConfig config, boolean dryRun) {
		this.repository = repository;
		this.spec = null;
		this.config = config;
		this.remoteName = null;
		this.branchName = branchName;
		this.dryRun = dryRun;
		if (branchName != null) {
			destinationString = MessageFormat.format("{0} {1} - {2}", //$NON-NLS-1$
					repository.getDirectory().getParentFile().getName(),
					branchName, config.getName());
		} else {
			destinationString = MessageFormat.format("{0} - {1}", //$NON-NLS-1$
					repository.getDirectory().getParentFile().getName(),
					config.getName());
		}
	}

	/**
	 * Push exactly the branches to remotes as specified by the
	 * {@link PushOperationSpecification}.
	 *
	 * @param repository
	 *            to push from
	 * @param spec
	 *            {@link PushOperationSpecification} defining what to push where
	 * @param dryRun
	 *            whether the push should be a dry run
	 */
	public PushOperationUI(Repository repository,
			PushOperationSpecification spec, boolean dryRun) {
		this.repository = repository;
		this.spec = spec;
		this.config = null;
		this.remoteName = null;
		this.branchName = null;
		this.dryRun = dryRun;
		if (spec.getURIsNumber() == 1)
			destinationString = spec.getURIs().iterator().next()
					.toPrivateString();
		else
			destinationString = MessageFormat.format(
					UIText.PushOperationUI_MultiRepositoriesDestinationString,
					Integer.valueOf(spec.getURIsNumber()));
	}

	/**
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * Set the expected result. If this is set, the result dialog in {@link #start()} will only be
	 * shown when the result is different from the expected result.
	 *
	 * @param expectedResult
	 */
	public void setExpectedResult(PushOperationResult expectedResult) {
		this.expectedResult = expectedResult;
	}

	/**
	 * Set whether the "Configure..." button should be shown in the result
	 * dialog of {@link #start()}.
	 *
	 * @param showConfigureButton
	 */
	public void setShowConfigureButton(boolean showConfigureButton) {
		this.showConfigureButton = showConfigureButton;
	}

	/**
	 * Executes this directly, without showing a confirmation dialog
	 *
	 * @param monitor
	 * @return the result of the operation
	 * @throws CoreException
	 */

	public PushOperationResult execute(IProgressMonitor monitor)
			throws CoreException {
		createPushOperation();
		if (credentialsProvider != null)
			op.setCredentialsProvider(credentialsProvider);
		else
			op.setCredentialsProvider(new EGitCredentialsProvider());
		try {
			op.run(monitor);
			return op.getOperationResult();
		} catch (InvocationTargetException e) {
			throw new CoreException(Activator.createErrorStatus(e.getCause()
					.getMessage(), e.getCause()));
		}
	}


	private void createPushOperation() throws CoreException {
		if (remoteName != null) {
			op = new PushOperation(repository, remoteName, dryRun,
					GitSettings.getRemoteConnectionTimeout());
			return;
		}

		if (spec == null) {
			// spec == null => config was supplied in constructor
			// we don't use the configuration directly, as it may contain
			// unsaved changes and as we may need
			// to add the default push RefSpec here
			spec = new PushOperationSpecification();

			List<URIish> urisToPush = new ArrayList<>();
			for (URIish uri : config.getPushURIs())
				urisToPush.add(uri);
			if (urisToPush.isEmpty() && !config.getURIs().isEmpty())
				urisToPush.add(config.getURIs().get(0));

			List<RefSpec> pushRefSpecs = new ArrayList<>();
			if (branchName == null) {
				pushRefSpecs.addAll(config.getPushRefSpecs());
			} else {
				Config repoConfig = repository.getConfig();
				String remoteBranchName = branchName;
				BranchConfig branchConfig = new BranchConfig(repoConfig,
						Repository.shortenRefName(branchName));
				String trackingBranchName = branchConfig.getMerge();
				if (!branchConfig.isRemoteLocal() && trackingBranchName != null
						&& trackingBranchName.startsWith(Constants.R_HEADS)) {
					remoteBranchName = trackingBranchName;
				}
				pushRefSpecs
						.add(new RefSpec(branchName + ':' + remoteBranchName));
			}

			for (URIish uri : urisToPush) {
				try {
					// Fetch ref specs are passed here to make sure that the
					// returned remote ref updates include tracking branch
					// updates.
					Collection<RemoteRefUpdate> remoteRefUpdates = Transport
							.findRemoteRefUpdatesFor(repository, pushRefSpecs,
									config.getFetchRefSpecs());
					spec.addURIRefUpdates(uri, remoteRefUpdates);
				} catch (NotSupportedException e) {
					throw new CoreException(Activator.createErrorStatus(
							e.getMessage(), e));
				} catch (IOException e) {
					throw new CoreException(Activator.createErrorStatus(
							e.getMessage(), e));
				}
			}
		}
		op = new PushOperation(repository, spec, dryRun,
				GitSettings.getRemoteConnectionTimeout());
	}

	/**
	 * Starts the operation asynchronously.
	 */
	public void start() {
		final Repository repo = repository;
		if (repo == null) {
			return;
		}
		try {
			createPushOperation();
		} catch (CoreException e) {
			Activator.showErrorStatus(e.getLocalizedMessage(), e.getStatus());
			return;
		}
		if (credentialsProvider != null) {
			op.setCredentialsProvider(credentialsProvider);
		} else {
			op.setCredentialsProvider(new EGitCredentialsProvider());
		}
		Job job = new PushJob(
				MessageFormat.format(UIText.PushOperationUI_PushJobName,
						destinationString),
				repo, op, expectedResult, destinationString,
				showConfigureButton, pushMode);
		job.setUser(true);
		job.schedule();
	}

	/**
	 * @return the string denoting the remote source
	 */
	public String getDestinationString() {
		return destinationString;
	}

	/**
	 * Defines the {@link PushMode}.If not set explicitly,
	 * {@link PushMode#UPSTREAM} is assumed.
	 *
	 * @param mode
	 *            to use
	 */
	public void setPushMode(@NonNull PushMode mode) {
		pushMode = mode;
	}
}
