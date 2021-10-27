/*******************************************************************************
 * Copyright (c) 2010, 2018 SAP AG and others.
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
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.egit.ui.internal.ResourcePropertyTester;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitSelectRepositoryPage;
import org.eclipse.egit.ui.internal.fetch.FetchGerritChangeWizard;
import org.eclipse.egit.ui.internal.gerrit.FilteredSelectRepositoryPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;

/**
 * Fetch a change from Gerrit
 */
public class FetchChangeFromGerritCommand extends AbstractFetchFromHostCommand {

	@Override
	protected GitSelectRepositoryPage createSelectionPage() {
		return new FilteredSelectRepositoryPage(
				UIText.GerritSelectRepositoryPage_PageTitle,
				UIIcons.WIZBAN_FETCH_GERRIT) {

			@Override
			protected boolean includeRepository(Repository repo) {
				return ResourcePropertyTester.hasGerritConfiguration(repo);
			}
		};
	}

	@Override
	protected Wizard createFetchWizard(Repository repository, String clipText) {
		return new FetchGerritChangeWizard(repository, clipText);
	}
}
