/*******************************************************************************
 * Copyright (c) 2013, 2016 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.eclipse.egit.core.internal.credentials.EGitCredentialsProvider;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;

/**
 * A wizard dedicated to pushing a commit.
 */
public class PushBranchWizard extends Wizard {

	private final Repository repository;

	private final ObjectId commitToPush;

	/**
	 * In case of detached HEAD, reference is null.
	 */
	private final Ref ref;

	private PushBranchPage pushBranchPage;
	private ConfirmationPage confirmationPage;


	/**
	 * @param repository
	 *            the repository the ref belongs to
	 * @param ref
	 */
	public PushBranchWizard(final Repository repository, Ref ref) {
		this(repository, ref.getObjectId(), ref);
	}

	/**
	 * @param repository
	 *            the repository commit belongs to
	 * @param commitToPush
	 */
	public PushBranchWizard(final Repository repository, ObjectId commitToPush) {
		this(repository, commitToPush, null);
	}

	private PushBranchWizard(final Repository repository, ObjectId commitToPush, Ref ref) {
		this.repository = repository;
		this.commitToPush = commitToPush;
		this.ref = ref;
		assert (this.repository != null);
		assert (this.commitToPush != null);

		pushBranchPage = new PushBranchPage(repository, commitToPush, ref);

		confirmationPage = new ConfirmationPage(repository) {
			@Override
			public void setVisible(boolean visible) {
				setSelection(getRepositorySelection(), getRefSpecs());
				super.setVisible(visible);
			}
		};

		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_PUSH);
	}

	@Override
	public void addPages() {
		addPage(pushBranchPage);
		addPage(confirmationPage);
	}

	@Override
	public String getWindowTitle() {
		if (ref != null)
			return MessageFormat.format(UIText.PushBranchWizard_WindowTitle,
					Repository.shortenRefName(this.ref.getName()));
		else
			return UIText.PushCommitHandler_pushCommitTitle;
	}

	@Override
	public boolean canFinish() {
		return getContainer().getCurrentPage() == confirmationPage
				&& confirmationPage.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		try {
			if (pushBranchPage.getUpstreamConfig() != null) {
				configureUpstream();
			}
			startPush();
		} catch (IOException e) {
			confirmationPage.setErrorMessage(e.getMessage());
			return false;
		}

		return true;
	}

	private RepositorySelection getRepositorySelection() {
		return new RepositorySelection(null,
					pushBranchPage.getRemoteConfig());
	}

	private List<RefSpec> getRefSpecs() {
		String src = this.ref != null ? this.ref.getName() : this.commitToPush.getName();
		String dst = pushBranchPage.getFullRemoteReference();
		RefSpec refSpec = new RefSpec().setSourceDestination(src, dst)
				.setForceUpdate(pushBranchPage.isForceUpdateSelected());
		return Arrays.asList(refSpec);
	}

	private void configureUpstream() throws IOException {
		if (this.ref == null) {
			// Don't configure upstream for detached HEAD
			return;
		}
		String remoteName = getRemoteName();
		String fullRemoteBranchName = pushBranchPage.getFullRemoteReference();
		String localBranchName = Repository.shortenRefName(this.ref.getName());

		StoredConfig config = repository.getConfig();
		config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, localBranchName,
				ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
		config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, localBranchName,
				ConfigConstants.CONFIG_KEY_MERGE, fullRemoteBranchName);
		BranchRebaseMode rebaseMode = pushBranchPage.getUpstreamConfig();
		if (rebaseMode != null) {
			config.setEnum(ConfigConstants.CONFIG_BRANCH_SECTION,
					localBranchName, ConfigConstants.CONFIG_KEY_REBASE,
					rebaseMode);
		}

		config.save();
	}

	private void startPush() throws IOException {
		PushOperationResult result = confirmationPage.getConfirmedResult();
		PushOperationSpecification pushSpec = result
				.deriveSpecification(confirmationPage
						.isRequireUnchangedSelected());

		PushOperationUI pushOperationUI = new PushOperationUI(repository,
				pushSpec, false);
		pushOperationUI.setCredentialsProvider(new EGitCredentialsProvider());
		pushOperationUI.setShowConfigureButton(false);
		if (confirmationPage.isShowOnlyIfChangedSelected())
			pushOperationUI.setExpectedResult(result);
		pushOperationUI.start();
	}

	private String getRemoteName() {
		return pushBranchPage.getRemoteConfig().getName();
	}
}
