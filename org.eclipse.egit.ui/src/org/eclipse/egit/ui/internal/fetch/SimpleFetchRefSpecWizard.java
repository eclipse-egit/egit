/*******************************************************************************
 * Copyright (C) 2011, 2017 Mathias Kinzler <mathias.kinzler@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Guides the user through some questions to add a RefSpec for fetch to an
 * existing {@link RemoteConfig}
 */
public class SimpleFetchRefSpecWizard extends Wizard {

	private final FetchSourcePage sourcePage;

	private final FetchDestinationPage destinationPage;

	private RefSpec result;

	/**
	 * @param localRepository
	 * @param config
	 */
	public SimpleFetchRefSpecWizard(Repository localRepository,
			RemoteConfig config) {
		sourcePage = new FetchSourcePage(localRepository, config);
		destinationPage = new FetchDestinationPage(localRepository, config);
		setWindowTitle(UIText.SimpleFetchRefSpecWizard_WizardTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_FETCH);
	}

	@Override
	public void addPages() {
		addPage(sourcePage);
		addPage(destinationPage);
	}

	@Override
	public boolean performFinish() {
		StringBuilder sb = new StringBuilder();
		if (destinationPage.isForce()) {
			sb.append('+');
		}
		sb.append(sourcePage.getSource());
		sb.append(':');
		sb.append(destinationPage.getDestination());
		result = new RefSpec(sb.toString());
		return true;
	}

	/**
	 * @return the spec
	 */
	public RefSpec getSpec() {
		return result;
	}

}
