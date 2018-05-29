/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.egit.core.op.PullOperation.PullReferenceConfig;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.SecureStoreUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.AddRemotePage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * A wizard to allow to specify a pull operation with options
 */
public class PullWizard extends Wizard {

	private final Repository repository;

	private PullWizardPage page;
	private AddRemotePage addRemotePage;

	/**
	 * @param repo
	 *            the repository
	 */
	public PullWizard(final Repository repo) {
		this.repository = repo;
		setWindowTitle(UIText.PullWizardPage_PageTitle);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		Set<String> remoteNames = repository.getConfig()
				.getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION);
		if (remoteNames.isEmpty()) {
			this.addRemotePage = new AddRemotePage(repository);
			addPage(this.addRemotePage);
		}
		this.page = new PullWizardPage(this.repository);
		addPage(this.page);
	}

	@Override
	public boolean performFinish() {
		try {
			if (this.addRemotePage != null) {
				storeCredentials(this.addRemotePage);
				URIish uri = this.addRemotePage.getSelection().getURI();
				configureNewRemote(uri);
			}
			if (this.page.overrideUpstreamConfiguration()) {
				configureUpstream();
			}
			startPull();
			return true;
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
			return false;
		} catch (URISyntaxException e) {
			Activator.logError(e.getMessage(), e);
			return false;
		}
	}

	private void storeCredentials(AddRemotePage remotePage) {
		if (remotePage.getStoreInSecureStore()) {
			URIish uri = remotePage.getSelection().getURI();
			if (uri != null) {
				SecureStoreUtils.storeCredentials(remotePage.getCredentials(),
						uri);
			}
		}
	}

	private void configureNewRemote(URIish uri)
			throws URISyntaxException, IOException {
		StoredConfig config = repository.getConfig();
		String remoteName = this.page.getRemoteConfig().getName();
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
		String fullBranch = this.repository.getFullBranch();
		if (fullBranch == null || !fullBranch.startsWith(Constants.R_HEADS)) {
			// Don't configure upstream for detached HEAD
			return;
		}
		String remoteName = this.page.getRemoteConfig().getName();
		String fullRemoteBranchName = this.page.getFullRemoteReference();

		String localBranchName = this.repository.getBranch();
		StoredConfig config = repository.getConfig();
		config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, localBranchName,
				ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
		config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, localBranchName,
				ConfigConstants.CONFIG_KEY_MERGE, fullRemoteBranchName);
		BranchRebaseMode rebaseMode = this.page.getUpstreamConfig();
		if (rebaseMode != null) {
			config.setEnum(ConfigConstants.CONFIG_BRANCH_SECTION,
					localBranchName, ConfigConstants.CONFIG_KEY_REBASE,
					rebaseMode);
		}

		config.save();
	}

	private void startPull() {
		Map<Repository, PullReferenceConfig> repos = new HashMap<>(1);
		PullReferenceConfig config = new PullReferenceConfig(
				this.page.getRemoteConfig().getName(),
				this.page.getFullRemoteReference(),
				this.page.getUpstreamConfig());
		repos.put(this.repository, config);
		PullOperationUI pullOperationUI = new PullOperationUI(repos);
		pullOperationUI.start();
	}

}
