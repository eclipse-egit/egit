/*******************************************************************************
 * Copyright (C) 2012, 2016 Stefan Lay <stefan.lay@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 493935
 *******************************************************************************/
package org.eclipse.egit.ui.internal.gerrit;

import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Configure a remote configuration for Gerrit Code Review
 */
public class ConfigureGerritWizard extends Wizard {

	/**
	 * a page for some gerrit related parameters
	 */
	private GerritConfigurationPage gerritConfiguration;
	private StoredConfig config;
	private final String remoteName;
	private RemoteConfig remoteConfig;

	private Repository repository;

	/**
	 * @param repository the repository
	 * @param remoteName the name of the remote in the configuration
	 *
	 */
	public ConfigureGerritWizard(Repository repository, String remoteName) {
		super();
		setWindowTitle(UIText.ConfigureGerritWizard_title);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		setNeedsProgressMonitor(true);
		gerritConfiguration = new GerritConfigurationPage(repository, remoteName) {
			@Override
			public void setVisible(boolean visible) {
				if (visible)
					configurePage();
				super.setVisible(visible);
			}

		};
		this.config = repository.getConfig();
		this.remoteName = remoteName;
		this.repository = repository;
	}

	@Override
	public void addPages() {
		addPage(gerritConfiguration);
	}

	private void configurePage() {
		try {
			remoteConfig = GerritUtil.findRemoteConfig(config, remoteName);
			if (remoteConfig != null) {
				gerritConfiguration.setSelection(getUri(), getProposedTargetBranch());
			}
		} catch (URISyntaxException e) {
			gerritConfiguration.setErrorMessage("Error in configured URI"); //$NON-NLS-1$
			Activator.logError("Configured URI could not be read", e); //$NON-NLS-1$
		}
	}

	private URIish getUri() {
		URIish urIish = null;
		if (remoteConfig.getPushURIs().size() > 0)
			urIish = remoteConfig.getPushURIs().get(0);
		else
			urIish = remoteConfig.getURIs().get(0);
		return urIish;
	}

	private String getProposedTargetBranch() {
		List<RefSpec> pushRefSpecs = remoteConfig.getPushRefSpecs();
		String destination = null;
		if (pushRefSpecs.size() > 0) {
			destination = pushRefSpecs.get(0).getDestination();
			if (destination.startsWith(Constants.R_HEADS))
				destination = destination.substring(Constants.R_HEADS.length());
			else if (destination.startsWith(GerritUtil.REFS_FOR))
				destination = destination.substring(GerritUtil.REFS_FOR
						.length());
		}
		return destination;
	}

	@Override
	public boolean canFinish() {
		return gerritConfiguration.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		try {
			configureRemoteSection();
			GerritUtil.setCreateChangeId(config);
			config.save();
		} catch (Exception e) {
			gerritConfiguration.setErrorMessage(e.getMessage());
			return false;
		}
		GerritDialogSettings.updateRemoteConfig(repository, remoteConfig);
		return true;
	}

	private void configureRemoteSection() {
		GerritUtil.configurePushURI(remoteConfig, gerritConfiguration.getURI());
		GerritUtil.configurePushRefSpec(remoteConfig,
				gerritConfiguration.getBranch());
		GerritUtil.configureFetchNotes(remoteConfig);
		remoteConfig.update(config);
	}
}
