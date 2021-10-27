/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP AG.
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

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Repository;

/**
 * Wizard for fetching a Gerrit change.
 */
public class FetchGerritChangeWizard extends AbstractFetchFromHostWizard {

	/**
	 * @param repository
	 *            the repository
	 */
	public FetchGerritChangeWizard(Repository repository) {
		super(repository);
		setWindowTitle(UIText.FetchGerritChangeWizard_WizardTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_FETCH_GERRIT);
	}

	/**
	 * @param repository
	 * @param refName initial value for the ref field
	 */
	public FetchGerritChangeWizard(Repository repository, String refName) {
		super(repository, refName);
		setWindowTitle(UIText.FetchGerritChangeWizard_WizardTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_FETCH_GERRIT);
	}

	@Override
	protected AbstractFetchFromHostPage createPage(Repository repo,
			String initialText) {
		return new FetchGerritChangePage(repo, initialText);
	}
}
