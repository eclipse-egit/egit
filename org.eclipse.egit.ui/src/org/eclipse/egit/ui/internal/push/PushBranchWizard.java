/*******************************************************************************
 * Copyright (c) 2013, 2016 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.ui.internal.SecureStoreUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

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

	private AddRemotePage addRemotePage;
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

		Set<String> remoteNames = repository.getConfig().getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION);
		if (remoteNames.isEmpty())
			addRemotePage = new AddRemotePage(repository);

		pushBranchPage = new PushBranchPage(repository, commitToPush, ref) {
			@Override
			public void setVisible(boolean visible) {
				if (visible && addRemotePage != null) {
					setSelectedRemote(addRemotePage.getRemoteName(),
							addRemotePage.getSelection().getURI());
				}
				super.setVisible(visible);
			}
		};
		// Don't show button if we're configuring a remote in the first step
		pushBranchPage.setShowNewRemoteButton(addRemotePage == null);

		confirmationPage = new ConfirmationPage(repository) {
			@Override
			public void setVisible(boolean visible) {
				setSelection(getRepositorySelection(), getRefSpecs());
				AddRemotePage remotePage = getAddRemotePage();
				if (remotePage != null)
					setCredentials(remotePage.getCredentials());
				super.setVisible(visible);
			}
		};

		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_PUSH);
	}

	@Override
	public void addPages() {
		if (addRemotePage != null)
			addPage(addRemotePage);
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
			AddRemotePage remotePage = getAddRemotePage();
			if (remotePage != null) {
				storeCredentials(remotePage);
				URIish uri = remotePage.getSelection().getURI();
				configureNewRemote(uri);
			}
			if (pushBranchPage.getUpstreamConfig() != null) {
				configureUpstream();
			}
			startPush();
		} catch (IOException e) {
			confirmationPage.setErrorMessage(e.getMessage());
			return false;
		} catch (URISyntaxException e) {
			confirmationPage.setErrorMessage(e.getMessage());
			return false;
		}

		return true;
	}

	private AddRemotePage getAddRemotePage() {
		if (addRemotePage != null)
			return addRemotePage;
		else
			return pushBranchPage.getAddRemotePage();
	}

	private RepositorySelection getRepositorySelection() {
		AddRemotePage remotePage = getAddRemotePage();
		if (remotePage != null)
			return remotePage.getSelection();
		else
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

	private void storeCredentials(AddRemotePage remotePage) {
		if (remotePage.getStoreInSecureStore()) {
			URIish uri = remotePage.getSelection().getURI();
			if (uri != null)
				SecureStoreUtils.storeCredentials(
						remotePage.getCredentials(), uri);
		}
	}

	private void configureNewRemote(URIish uri) throws URISyntaxException,
			IOException {
		StoredConfig config = repository.getConfig();
		String remoteName = getRemoteName();
		RemoteConfig remoteConfig = new RemoteConfig(config, remoteName);
		remoteConfig.addURI(uri);
		RefSpec defaultFetchSpec = new RefSpec().setForceUpdate(true)
				.setSourceDestination(Constants.R_HEADS + "*", //$NON-NLS-1$
						Constants.R_REMOTES + remoteName + "/*"); //$NON-NLS-1$
		remoteConfig.addFetchRefSpec(defaultFetchSpec);
		remoteConfig.update(config);
		config.save();
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
