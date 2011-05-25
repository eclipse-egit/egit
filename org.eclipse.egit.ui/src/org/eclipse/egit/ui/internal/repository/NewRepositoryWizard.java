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

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * The "Create New Repository" wizard
 */
public class NewRepositoryWizard extends Wizard implements INewWizard {
	private final CreateRepositoryPage myCreatePage;

	private Repository newRepo;

	/**
	 * @param hideBareOption
	 *            if <code>true</code>, no "bare" repository can be created
	 */
	public NewRepositoryWizard(boolean hideBareOption) {
		myCreatePage = new CreateRepositoryPage(hideBareOption);
	}

	@Override
	public void addPages() {
		setWindowTitle(UIText.NewRepositoryWizard_WizardTitle);
		setHelpAvailable(false);
		addPage(myCreatePage);
	}

	@Override
	public boolean performFinish() {
		RepositoryCache cache = Activator.getDefault().getRepositoryCache();
		try {
			File repoFile = new File(myCreatePage.getDirectory());
			if (!myCreatePage.getBare())
				repoFile = new File(repoFile, Constants.DOT_GIT);

			Repository repoToCreate = cache.lookupRepository(repoFile);
			repoToCreate.create(myCreatePage.getBare());
			Activator.getDefault().getRepositoryUtil()
					.addConfiguredRepository(repoFile);
			this.newRepo = repoToCreate;
		} catch (IOException e) {
			org.eclipse.egit.ui.Activator.handleError(e.getMessage(), e, false);
		}
		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to initialize
	}

	/**
	 * @return the newly created Repository in case of successful completion,
	 *         otherwise <code>null</code
	 */
	public Repository getCreatedRepository() {
		return newRepo;
	}
}
