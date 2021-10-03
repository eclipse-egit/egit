/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP AG.
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
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;

/**
 * Abstract base for wizards for fetching a change from a remote host.
 */
public abstract class AbstractFetchFromHostWizard extends Wizard {
	private final Repository repository;

	AbstractFetchFromHostPage page;

	private String refName;

	/**
	 * @param repository
	 *            the repository
	 */
	protected AbstractFetchFromHostWizard(Repository repository) {
		Assert.isNotNull(repository);
		this.repository = repository;
		setNeedsProgressMonitor(true);
		setHelpAvailable(false);
	}

	/**
	 * @param repository
	 * @param refName initial value for the ref field
	 */
	protected AbstractFetchFromHostWizard(Repository repository,
			String refName) {
		this(repository);
		this.refName = refName;
	}

	@Override
	public void addPages() {
		page = createPage(repository, refName);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		return page.doFetch();
	}

	/**
	 * Creates the main fetch page.
	 *
	 * @param repo
	 *            to work in
	 * @param initialText
	 *            for the change ref field
	 *
	 * @return the page
	 */
	protected abstract AbstractFetchFromHostPage createPage(Repository repo,
			String initialText);
}
