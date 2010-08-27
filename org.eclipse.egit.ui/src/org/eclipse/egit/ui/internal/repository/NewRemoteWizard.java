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
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.components.RefSpecPage;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;

/**
 * Used for "remote" configuration of a Repository
 *
 */
public class NewRemoteWizard extends Wizard {
	final StoredConfig myConfiguration;

	private SelectRemoteNamePage selNamePage;

	private ConfigureUriPage configureFetchUriPage;

	private RefSpecPage configureFetchSpecPage;

	private ConfigureUriPage configurePushUriPage;

	private RefSpecPage configurePushSpecPage;

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == selNamePage)
			if (selNamePage.configureFetch.getSelection())
				return configureFetchUriPage;
			else if (selNamePage.configurePush.getSelection())
				return configurePushUriPage;

		if (page == configureFetchSpecPage)
			if (!selNamePage.configurePush.getSelection())
				return null;
			else
				configurePushUriPage.setURI(configureFetchUriPage.getUri());

		if (page == configureFetchUriPage) {
			try {
				getContainer().run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						String taskName = NLS.bind(
								UIText.NewRemoteWizard_CheckingUriTaskName,
								configureFetchUriPage.getUri()
										.toPrivateString());
						monitor.beginTask(taskName, IProgressMonitor.UNKNOWN);
						configureFetchSpecPage
								.setSelection(new RepositorySelection(
										configureFetchUriPage.getUri(), null));
						monitor.done();

					}
				});
			} catch (InvocationTargetException e) {
				Activator.handleError(e.getMessage(), e, true);
			} catch (InterruptedException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		}

		if (page == configurePushUriPage)
			try {
				getContainer().run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						String taskName = NLS.bind(
								UIText.NewRemoteWizard_CheckingUriTaskName,
								configurePushUriPage.getAllUris().get(0)
										.toPrivateString());
						monitor.beginTask(taskName, IProgressMonitor.UNKNOWN);
						configurePushSpecPage
								.setSelection(new RepositorySelection(
										configurePushUriPage.getAllUris()
												.get(0), null));
						monitor.done();
					}
				});
			} catch (InvocationTargetException e) {
				Activator.handleError(e.getMessage(), e, true);
			} catch (InterruptedException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		return super.getNextPage(page);
	}

	@Override
	public boolean canFinish() {
		if (selNamePage.isPageComplete()) {
			boolean complete = true;
			if (selNamePage.configureFetch.getSelection())
				complete = complete && configureFetchSpecPage.isPageComplete();
			if (selNamePage.configurePush.getSelection())
				complete = complete && configurePushSpecPage.isPageComplete();
			return complete;
		}

		return super.canFinish();
	}

	/**
	 *
	 * @param repository
	 */
	public NewRemoteWizard(Repository repository) {
		super.setNeedsProgressMonitor(true);
		myConfiguration = repository.getConfig();

		selNamePage = new SelectRemoteNamePage();
		addPage(selNamePage);

		configureFetchUriPage = new ConfigureUriPage(true, null);
		addPage(configureFetchUriPage);

		configureFetchSpecPage = new RefSpecPage(repository, false);
		addPage(configureFetchSpecPage);

		configurePushUriPage = new ConfigureUriPage(false, null);
		addPage(configurePushUriPage);

		configurePushSpecPage = new RefSpecPage(repository, true);
		addPage(configurePushSpecPage);

		setWindowTitle(UIText.ConfigureRemoteWizard_WizardTitle_New);
	}

	/**
	 * @return the configuration
	 *
	 */
	public StoredConfig getConfiguration() {
		return myConfiguration;
	}

	@Override
	public boolean performFinish() {
		RemoteConfig config;

		try {
			config = new RemoteConfig(myConfiguration, selNamePage.remoteName
					.getText());
		} catch (URISyntaxException e1) {
			// TODO better Exception handling
			return false;
		}

		if (selNamePage.configureFetch.getSelection()) {
			config.addURI(configureFetchUriPage.getUri());
			config.setFetchRefSpecs(configureFetchSpecPage.getRefSpecs());
			config.setTagOpt(configureFetchSpecPage.getTagOpt());
		}

		if (selNamePage.configurePush.getSelection()) {
			for (URIish uri : configurePushUriPage.getUris())
				config.addPushURI(uri);
			config.setPushRefSpecs(configurePushSpecPage.getRefSpecs());
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
