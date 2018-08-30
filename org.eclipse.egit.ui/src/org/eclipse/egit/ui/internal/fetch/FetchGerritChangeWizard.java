/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;

/**
 * Wizard for fetching a Gerrit change
 */
public class FetchGerritChangeWizard extends Wizard {
	private Repository repository;

	FetchGerritChangePage page;

	private String refName;

	/**
	 *
	 */
	public FetchGerritChangeWizard() {
		setNeedsProgressMonitor(true);
		setHelpAvailable(false);
		setWindowTitle(UIText.FetchGerritChangeWizard_WizardTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_FETCH_GERRIT);
	}

	/**
	 * @param repository
	 *            the repository
	 */
	public FetchGerritChangeWizard(Repository repository) {
		this();
		Assert.isNotNull(repository);
		this.repository = repository;
	}

	/**
	 * @param repository
	 * @param refName initial value for the ref field
	 */
	public FetchGerritChangeWizard(Repository repository, String refName) {
		this();
		Assert.isNotNull(repository);
		this.repository = repository;
		this.refName = refName;
	}

	@Override
	public void addPages() {
		page = new FetchGerritChangePage(repository, refName);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		return page.doFetch();
	}

	/**
	 * @return Repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @param repository
	 */
	public void setRepository(Repository repository) {
		this.repository = repository;
	}
}
