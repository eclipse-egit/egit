/*******************************************************************************
 * Copyright (C) 2010, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RefSpecPage;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Wizard to maintain RefSpecs
 */
public class RefSpecWizard extends Wizard {
	private final boolean pushMode;

	private final RemoteConfig config;

	private RefSpecPage page;

	/**
	 * @param repository
	 * @param config
	 * @param pushMode
	 */
	public RefSpecWizard(Repository repository, RemoteConfig config,
			boolean pushMode) {
		setNeedsProgressMonitor(true);
		this.pushMode = pushMode;
		this.config = config;
		page = new RefSpecPage(repository, pushMode);
		setWindowTitle(pushMode);
	}

	private void setWindowTitle(boolean pushMode) {
		final String title;
		if (pushMode)
			title = UIText.RefSpecWizard_pushTitle;
		else
			title = UIText.RefSpecWizard_fetchTitle;
		setWindowTitle(title);
	}

	@Override
	public void addPages() {
		addPage(page);
	}

	@Override
	public IWizardPage getStartingPage() {
		// only now do we set the selection (which will be progress-monitored by
		// the wizard)
		page.setSelection(new RepositorySelection(null, config));
		return super.getStartingPage();
	}

	@Override
	public boolean performFinish() {
		if (pushMode) {
			config.setPushRefSpecs(page.getRefSpecs());
		} else {
			config.setFetchRefSpecs(page.getRefSpecs());
			config.setTagOpt(page.getTagOpt());
		}
		return true;
	}
}
