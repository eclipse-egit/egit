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
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;

/**
 * Used for "remote" configuration of a Repository
 *
 * If this is in "create"-mode, there will be the following pages:
 * <ol>
 * <li>Selection of a remote name</li>
 * <li>Fetch URL</li>
 * <li>Fetch Specification</li>
 * <li>Push URL</li>
 * <li>Push Specification</li>
 * </ol>
 * <p>
 * In "edit"-mode, there will be the following pages:
 *
 *
 * <ol>
 * <li>Fetch or Push URL</li>
 * <li>Fetch or Push Specification</li>
 * </ol>
 *
 */
class ConfigureRemoteWizard extends Wizard {

	final RepositoryConfig myConfiguration;

	final boolean createMode;

	final boolean pushMode;

	final String myRemoteName;

	/**
	 * @param repository
	 */
	public ConfigureRemoteWizard(Repository repository) {
		this(repository, null, false);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {


		if (!createMode) {
			return super.getNextPage(page);
		}
		if (page instanceof SelectRemoteNamePage) {
			SelectRemoteNamePage srp = (SelectRemoteNamePage) page;
			if (srp.configureFetch.getSelection()) {
				return getPages()[1];
			}
			if (srp.configurePush.getSelection()) {
				return getPages()[3];
			}
		}
		if (page == getPages()[1] || page == getPages()[3]) {
			RefSpecPage next = (RefSpecPage) getPages()[2];
			next
					.setConfigName(((SelectRemoteNamePage) getPages()[0]).remoteName
							.getText());
			next = (RefSpecPage) getPages()[4];
			next
					.setConfigName(((SelectRemoteNamePage) getPages()[0]).remoteName
							.getText());

		}
		if (page == getPages()[2]) {
			SelectRemoteNamePage srp = (SelectRemoteNamePage) getPages()[0];
			if (srp.configurePush.getSelection()) {
				return getPages()[3];
			} else {
				return null;
			}
		}

		return super.getNextPage(page);
	}

	@Override
	public boolean canFinish() {
		if (createMode) {
			IWizardPage[] pages = getPages();
			if (pages[0].isPageComplete()) {
				boolean done = true;
				SelectRemoteNamePage srp = (SelectRemoteNamePage) pages[0];
				if (srp.configureFetch.getSelection())
					done = done & pages[1].isPageComplete()
							& pages[2].isPageComplete();
				if (srp.configurePush.getSelection())
					done = done & pages[3].isPageComplete()
							& pages[4].isPageComplete();
				return done;
			}
			return false;
		}
		return super.canFinish();
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
		createMode = remoteName == null;
		if (createMode) {
			// selection of a remote name
			addPage(new SelectRemoteNamePage());

			// repository selection for fetch
			RepositorySelectionPage sp = new RepositorySelectionPage(false,
					null, myConfiguration.getString(RepositoriesView.REMOTE,
							null, RepositoriesView.URL));
			addPage(sp);

			// ref spec for fetch
			RefSpecPage rsp = new RefSpecPage(repository, false, sp);
			addPage(rsp);

			// repository selection for push
			sp = new RepositorySelectionPage(true, null, myConfiguration
					.getString(RepositoriesView.REMOTE, null,
							RepositoriesView.PUSHURL));
			addPage(sp);

			// ref spec for push
			rsp = new RefSpecPage(repository, true, sp);
			addPage(rsp);

			setWindowTitle(UIText.ConfigureRemoteWizard_WizardTitle_New);

		} else {
			// edit mode: no remote name page and pre-selected repository
			// selection page
			RepositorySelectionPage sp;
			if (pushMode) {
				sp = new RepositorySelectionPage(pushMode, null,
						myConfiguration.getString(RepositoriesView.REMOTE,
								myRemoteName, RepositoriesView.PUSHURL));
			} else {
				sp = new RepositorySelectionPage(pushMode, null,
						myConfiguration.getString(RepositoriesView.REMOTE,
								myRemoteName, RepositoriesView.URL));
			}

			addPage(sp);
			// and also the corresponding configuration page
			RefSpecPage rsp = new RefSpecPage(repository, pushMode, sp);
			rsp.setConfigName(myRemoteName);
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

		RemoteConfig config;
		if (createMode) {

			SelectRemoteNamePage srp = (SelectRemoteNamePage) getPage(SelectRemoteNamePage.class
					.getName());

			String actRemoteName = srp.remoteName.getText();

			try {
				config = new RemoteConfig(myConfiguration, actRemoteName);
			} catch (URISyntaxException e1) {
				// TODO better Exception handling
				return false;
			}

			if (srp.configureFetch.getSelection()) {
				RepositorySelectionPage sp = (RepositorySelectionPage) getPages()[1];
				config.addURI(sp.getSelection().getURI());
				RefSpecPage specPage = (RefSpecPage) getPages()[2];
				config.setFetchRefSpecs(specPage.getRefSpecs());
				config.setTagOpt(specPage.getTagOpt());
				config.update(myConfiguration);
				sp.saveUriInPrefs(sp.getSelection().getURI().toString());

			}
			if (srp.configurePush.getSelection()) {
				RepositorySelectionPage sp = (RepositorySelectionPage) getPages()[3];
				config.addPushURI(sp.getSelection().getURI());
				RefSpecPage specPage = (RefSpecPage) getPages()[4];
				config.setPushRefSpecs(specPage.getRefSpecs());
				config.update(myConfiguration);
				sp.saveUriInPrefs(sp.getSelection().getURI().toString());
			}

		} else {

			RepositorySelectionPage sp = (RepositorySelectionPage) getPage(RepositorySelectionPage.class
					.getName());

			RefSpecPage specPage = (RefSpecPage) getPage(RefSpecPage.class
					.getName());

			try {
				config = new RemoteConfig(myConfiguration, myRemoteName);
			} catch (URISyntaxException e1) {
				// TODO better Exception handling
				return false;
			}


			if (pushMode){
				config.addPushURI(sp.getSelection().getURI());
				config.setPushRefSpecs(specPage.getRefSpecs());
			}
			else {
				config.addURI(sp.getSelection().getURI());
				config.setFetchRefSpecs(specPage.getRefSpecs());
				config.setTagOpt(specPage.getTagOpt());
			}

			sp.saveUriInPrefs(sp.getSelection().getURI().toString());
		}

		config.update(myConfiguration);

		try {
			myConfiguration.save();
			return true;
		} catch (IOException e) {
			// TODO better Exception handling
			return false;
		}
	}

}
