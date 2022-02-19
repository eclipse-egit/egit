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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.credentials.EGitCredentialsProvider;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushConfig;
import org.eclipse.jgit.transport.PushConfig.PushDefault;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

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
	 * Push to the given remote using the git configuration, pushing either
	 * whatever is configured in the push refspecs of the {@link RemoteConfig}
	 * or {@link PushDefault#CURRENT} if there are no push refspecs.
	 *
	 * @param repository
	 *            to push from
	 * @param remoteName
	 *            name of the {@link RemoteConfig} specifying where to push to
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
	 * Push all branches that match a configured push refspec.
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

			try {
				spec = PushOperationSpecification.create(repository, config,
						pushRefSpecs);
			} catch (NotSupportedException e) {
				throw new CoreException(
						Activator.createErrorStatus(e.getMessage(), e));
			} catch (IOException e) {
				throw new CoreException(
						Activator.createErrorStatus(e.getMessage(), e));
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

	/**
	 * Performs a "Push to Upstream". This is the equivalent of "git push",
	 * using whatever is configured in the git config. If multiple branches
	 * would be pushed, a warning dialog is shown, where the user can cancel the
	 * operation. If no push refspecs are configured and the push specification
	 * of the current branch is ambiguous, or if we're in detached head mode,
	 * the "Push Branch" dialog is shown to give the user a chance to specify
	 * where to push to.
	 *
	 * @param parent
	 *            {@link Shell} to use as parent for dialogs
	 * @param repository
	 *            to perform the operation on
	 * @throws IOException
	 *             if the repository or its configuration cannot be read
	 */
	public static void pushToUpstream(Shell parent,
			@NonNull Repository repository) throws IOException {
		PushOperationUI op = doPushToUpstream(parent, repository);
		if (op != null) {
			op.start();
		}
	}

	private static PushOperationUI doPushToUpstream(Shell parent,
			@NonNull Repository repository) throws IOException {
		String fullBranch = repository.getFullBranch();
		if (ObjectId.isId(fullBranch)) {
			pushBranchDialog(parent, repository);
			return null;
		}
		String shortBranch = Repository.shortenRefName(fullBranch);
		Config config = repository.getConfig();
		RemoteConfig remoteCfg = PushOperation.getRemote(shortBranch, config);
		if (remoteCfg == null) {
			nothingToPush(parent);
			return null;
		}
		List<RefSpec> refSpecs = remoteCfg.getPushRefSpecs();
		if (!refSpecs.isEmpty()) {
			RefSpec match = refSpecs.stream().filter(RefSpec::isMatching)
					.findAny().orElse(null);
			if (match != null) {
				if (repository.getRefDatabase()
							.getRefsByPrefix(Constants.R_HEADS).size() > 1) {
					if (!warnMatching(parent,
							RepositoryUtil.INSTANCE
									.getRepositoryName(repository),
							remoteCfg.getName(),
							MessageFormat.format(
									UIText.PushOperationUI_PushMatchingPushRefSpec,
									remoteCfg.getName(), match))) {
						return null;
					}
				}
			} else {
				Collection<RemoteRefUpdate> updates = Transport
						.findRemoteRefUpdatesFor(repository, refSpecs,
								remoteCfg.getFetchRefSpecs());
				if (updates.isEmpty()) {
					nothingToPush(parent);
					return null;
				} else if (updates.size() > 1) {
					List<String> allLocalNames = updates.stream()
							.map(RemoteRefUpdate::getSrcRef)
							.collect(Collectors.toList());
					if (!warnMultiple(parent, allLocalNames)) {
						return null;
					}
				}
			}
			return new PushOperationUI(repository, remoteCfg.getName(), false);
		} else {
			PushDefault pushDefault = config.get(PushConfig::new)
					.getPushDefault();
			switch (pushDefault) {
			case CURRENT:
				return new PushOperationUI(repository, remoteCfg.getName(),
						false);
			case MATCHING:
				int numberOfBranches = repository.getRefDatabase()
						.getRefsByPrefix(Constants.R_HEADS).size();
				if (numberOfBranches == 0) {
					nothingToPush(parent);
					return null;
				}
				if (numberOfBranches > 1) {
					if (!warnMatching(parent,
							RepositoryUtil.INSTANCE
									.getRepositoryName(repository),
							remoteCfg.getName(), "push.default=matching")) { //$NON-NLS-1$
						return null;
					}
				}
				PushOperationSpecification spec = PushOperationSpecification
						.create(repository, remoteCfg,
								Collections.singleton(new RefSpec(":"))); //$NON-NLS-1$
				return new PushOperationUI(repository, spec, false);
			case NOTHING:
				nothingToPush(parent);
				return null;
			case SIMPLE:
			case UPSTREAM:
				BranchConfig branchCfg = new BranchConfig(config, shortBranch);
				String upstreamBranch = branchCfg.getMerge();
				if (upstreamBranch == null) {
					// Nothing configured
					pushBranchDialog(parent, repository);
					return null;
				}
				String fetchRemote = branchCfg.getRemote();
				if (fetchRemote == null) {
					fetchRemote = Constants.DEFAULT_REMOTE_NAME;
				}
				boolean isTriangular = !fetchRemote.equals(remoteCfg.getName());
				if (isTriangular) {
					// UPSTREAM not allowed in C git: use dialog. SIMPLE falls
					// back to CURRENT.
					if (PushDefault.UPSTREAM.equals(pushDefault)) {
						pushBranchDialog(parent, repository);
						return null;
					}
					return new PushOperationUI(repository, remoteCfg.getName(),
							false);
				}
				if (PushDefault.SIMPLE.equals(pushDefault)
						&& !upstreamBranch.equals(fullBranch)) {
					pushBranchDialog(parent, repository);
					return null;
				}
				return new PushOperationUI(repository, fullBranch, remoteCfg,
						false);
			default:
				throw new IllegalStateException(
						"Unknown push.default: " + pushDefault); //$NON-NLS-1$
			}
		}
	}

	private static void nothingToPush(Shell shell) {
		MessageDialog.openInformation(shell,
				UIText.SimplePushActionHandler_NothingToPushDialogTitle,
				UIText.SimplePushActionHandler_NothingToPushDialogMessage);
	}

	private static void pushBranchDialog(Shell shell,
			@NonNull Repository repo) throws IOException {
		Wizard wizard = PushMode.UPSTREAM.getWizard(repo, null);
		if (wizard != null) {
			PushWizardDialog dialog = new PushWizardDialog(shell, wizard);
			dialog.open();
		}
	}

	private static boolean warnMatching(Shell shell, String repository,
			String remote, String cause) {
		MessageDialog dialog = new MessageDialog(shell,
				UIText.PushOperationUI_PushMatchingTitle, null,
				MessageFormat.format(UIText.PushOperationUI_PushMatchingMessage,
						repository, remote, cause),
				MessageDialog.QUESTION, IDialogConstants.OK_ID,
				UIText.PushOperationUI_PushMultipleOkLabel,
				IDialogConstants.CANCEL_LABEL);
		return dialog.open() == Window.OK;
	}

	private static boolean warnMultiple(Shell shell, List<String> refNames) {
		PushMultipleDialog dialog = new PushMultipleDialog(shell, refNames);
		return dialog.open() == Window.OK;
	}

	private static class PushMultipleDialog extends MessageDialog {

		private final List<String> names;

		PushMultipleDialog(Shell parent, List<String> names) {
			super(parent, UIText.PushOperationUI_PushMultipleTitle, null,
					UIText.PushOperationUI_PushMultipleMessage,
					MessageDialog.INFORMATION, IDialogConstants.OK_ID,
					UIText.PushOperationUI_PushMultipleOkLabel,
					IDialogConstants.CANCEL_LABEL);
			this.names = names;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			ResourceManager resources = new LocalResourceManager(
					JFaceResources.getResources());
			parent.addDisposeListener(event -> resources.dispose());
			TableViewer table = new TableViewer(parent,
					SWT.READ_ONLY | SWT.V_SCROLL);
			table.setContentProvider(ArrayContentProvider.getInstance());
			table.setLabelProvider(
					LabelProvider.createImageProvider(element -> {
						if (element.toString().startsWith(Constants.R_HEADS)) {
							return UIIcons.getImage(resources, UIIcons.BRANCH);
						}
						return UIIcons.getImage(resources, UIIcons.COMMIT);
					}));
			table.setInput(names);

			GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
			layoutData.heightHint = Math.min(20, names.size() + 1)
					* table.getTable().getItemHeight();
			table.getControl().setLayoutData(layoutData);

			return table.getControl();
		}
	}
}
