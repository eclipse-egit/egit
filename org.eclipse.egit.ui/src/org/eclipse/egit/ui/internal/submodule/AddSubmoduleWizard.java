/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.submodule;

import org.eclipse.egit.ui.internal.SecureStoreUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

/**
 * Wizard to configure a new submodule
 */
public class AddSubmoduleWizard extends Wizard {

	private final Repository repo;

	private SubmodulePathWizardPage pathPage;

	private RepositorySelectionPage uriPage;

	/**
	 * Create wizard
	 *
	 * @param repo
	 */
	public AddSubmoduleWizard(Repository repo) {
		this.repo = repo;
		setWindowTitle(UIText.AddSubmoduleWizard_WindowTitle);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
	}

	@Override
	public void addPages() {
		pathPage = new SubmodulePathWizardPage(repo);
		addPage(pathPage);
		uriPage = new RepositorySelectionPage(true, null);
		uriPage.setPageComplete(false);
		addPage(uriPage);
	}

	/**
	 * Get path of submodule
	 *
	 * @return path
	 */
	public String getPath() {
		return pathPage.getPath();
	}

	/**
	 * Get URI of submodule
	 *
	 * @return uri
	 */
	public URIish getUri() {
		return uriPage.getSelection().getURI();
	}

	@Override
	public boolean performFinish() {
		if (uriPage.getStoreInSecureStore()
				&& !SecureStoreUtils.storeCredentials(uriPage.getCredentials(),
						uriPage.getSelection().getURI()))
			return false;

		return true;
	}
}
