/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

/**
 * Wizard for adding a new remote.
 */
public class AddRemoteWizard extends Wizard {

	private AddRemotePage page;

	private URIish uri;

	private String remoteName;

	/**
	 * @param repository
	 */
	public AddRemoteWizard(Repository repository) {
		setWindowTitle(UIText.AddRemoteWizard_Title);
		page = new AddRemotePage(repository);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		uri = page.getSelection().getURI();
		remoteName = page.getRemoteName();
		return uri != null;
	}

	/**
	 * @return repository selection of URI page
	 */
	public RepositorySelection getRepositorySelection() {
		return page.getSelection();
	}

	/**
	 * @return the wizard page
	 */
	public AddRemotePage getAddRemotePage() {
		return page;
	}

	/**
	 * @return the entered URI
	 */
	public URIish getUri() {
		return uri;
	}

	/**
	 * @return the entered remote name
	 */
	public String getRemoteName() {
		return remoteName;
	}
}
