/*******************************************************************************
 * Copyright (C) 2012, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.gerrit;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.ConfigConstants;
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

	}

	@Override
	public void addPages() {
		addPage(gerritConfiguration);
	}

	private void configurePage() {
		try {
			findRemoteConfig();
			if (remoteConfig != null) {
				gerritConfiguration.setSelection(getUri(), getProposedTargetBranch());
			}
		} catch (URISyntaxException e) {
			gerritConfiguration.setErrorMessage("Error in configured URI"); //$NON-NLS-1$
			Activator.logError("Configured URI could not be read", e); //$NON-NLS-1$
		}
	}

	private void findRemoteConfig() throws URISyntaxException {
		List<RemoteConfig> allRemoteConfigs = RemoteConfig.getAllRemoteConfigs(config);
		for (RemoteConfig rc : allRemoteConfigs) {
			if (rc.getName().equals(remoteName))
				remoteConfig = rc;
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
			else if (destination.startsWith("refs/for/")) //$NON-NLS-1$
				destination = destination.substring("refs/for/".length()); //$NON-NLS-1$
		}
		return destination;
	}

	@Override
	public boolean canFinish() {
		return gerritConfiguration.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		configureRemoteSection();
		configureCreateChangeId();
		try {
			config.save();
		} catch (IOException e) {
			gerritConfiguration.setErrorMessage(e.getMessage());
			return false;
		}
		return true;
	}

	private void configureRemoteSection() {
		configurePushURI();
		configurePushRefSpec();
		configureFetchNotes();
		remoteConfig.update(config);
	}

	private void configurePushURI() {
		List<URIish> pushURIs = new ArrayList<URIish>(remoteConfig.getPushURIs());
		for (URIish urIish : pushURIs) {
			remoteConfig.removePushURI(urIish);
		}
		URIish pushURI = gerritConfiguration.getURI();
		remoteConfig.addPushURI(pushURI);
	}

	private void configurePushRefSpec() {
		String gerritBranch = gerritConfiguration.getBranch();
		List<RefSpec> pushRefSpecs = new ArrayList<RefSpec>(remoteConfig.getPushRefSpecs());
		for (RefSpec refSpec : pushRefSpecs) {
			remoteConfig.removePushRefSpec(refSpec);
		}
		remoteConfig.addPushRefSpec(new RefSpec( "HEAD:refs/for/" + gerritBranch)); //$NON-NLS-1$
	}

	private void configureFetchNotes() {
		String notesRef = Constants.R_NOTES + "*"; //$NON-NLS-1$
		List<RefSpec> fetchRefSpecs = remoteConfig.getFetchRefSpecs();
		for (RefSpec refSpec : fetchRefSpecs) {
			if(refSpec.matchSource(notesRef))
				return;
		}
		remoteConfig.addFetchRefSpec(new RefSpec(notesRef + ":" + notesRef)); //$NON-NLS-1$
	}

	private void configureCreateChangeId() {
		config.setBoolean(ConfigConstants.CONFIG_GERRIT_SECTION,
				null, ConfigConstants.CONFIG_KEY_CREATECHANGEID, true);
	}

}
