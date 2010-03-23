/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.components.RefSpecPage;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;

/**
 * Used for "remote" configuration of a Repository
 */
class ConfigureRemoteWizard extends Wizard {

	final RepositoryConfig myConfiguration;

	final boolean pushMode;

	final String myRemoteName;

	/**
	 * @param repository
	 */
	public ConfigureRemoteWizard(Repository repository) {
		this(repository, null, false);
	}

	/**
	 *
	 * @param repository
	 * @param remoteName
	 * @param push
	 */
	public ConfigureRemoteWizard(Repository repository, String remoteName,
			boolean push) {
		myConfiguration = repository.getConfig();
		myRemoteName = remoteName;
		pushMode = push;
		if (myRemoteName == null) {
			// create mode: add remote name page and repository selection page
			addPage(new SelectRemoteNamePage());
			addPage(new RepositorySelectionPage(null));
			setWindowTitle(UIText.ConfigureRemoteWizard_WizardTitle_New);
		} else {
			// edit mode: no remote name page and pre-selected repository
			// selection page
			RepositorySelectionPage sp = new RepositorySelectionPage(
					myConfiguration.getString(RepositoriesView.REMOTE,
							myRemoteName, RepositoriesView.URL));

			addPage(sp);
			// and also the corresponding configuration page
			RefSpecPage rsp = new RefSpecPage(repository, pushMode, sp,
					myRemoteName);
			addPage(rsp);
			setWindowTitle(NLS.bind(
					UIText.ConfigureRemoteWizard_WizardTitle_Change,
					myRemoteName));
		}

	}

	/**
	 * @return the configuration
	 *
	 */
	public RepositoryConfig getConfiguration() {
		return myConfiguration;
	}

	@Override
	public boolean performFinish() {

		String actRemoteName = myRemoteName;
		if (myRemoteName == null) {
			SelectRemoteNamePage page = (SelectRemoteNamePage) getPage(SelectRemoteNamePage.class
					.getName());
			actRemoteName = page.remoteName.getText();
		}

		RepositorySelectionPage sp = (RepositorySelectionPage) getPage(RepositorySelectionPage.class
				.getName());

		String uriString = sp.getSelection().getURI().toString();

		myConfiguration.setString(RepositoriesView.REMOTE, actRemoteName,
				RepositoriesView.URL, uriString);

		if (myRemoteName != null) {

			RefSpecPage specPage = (RefSpecPage) getPage(RefSpecPage.class
					.getName());

			if (specPage.getRefSpecs().isEmpty()) {
				specPage.setVisible(true);
				specPage.setVisible(false);
			}

			RemoteConfig config;
			try {
				config = new RemoteConfig(myConfiguration, actRemoteName);
			} catch (URISyntaxException e1) {
				// TODO better Exception handling
				return false;
			}

			if (pushMode)
				config.setPushRefSpecs(specPage.getRefSpecs());
			else {
				config.setFetchRefSpecs(specPage.getRefSpecs());
				config.setTagOpt(specPage.getTagOpt());
			}
			config.update(myConfiguration);
		}

		try {
			myConfiguration.save();
			return true;
		} catch (IOException e) {
			// TODO better Exception handling
			return false;
		}
	}

}
