/*******************************************************************************
 * Copyright (c) 2010, 2017 SAP AG and others
 *
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
package org.eclipse.egit.ui.internal.repository;

import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.internal.SecureStoreUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.transport.URIish;

/**
 * Wizard to select a URI
 */
public class SelectUriWizard extends Wizard {
	private URIish uri;
	private RepositorySelectionPage page;


	/**
	 * @param sourceSelection
	 */
	public SelectUriWizard(boolean sourceSelection) {
		this(sourceSelection, null);
	}

	/**
	 * @param sourceSelection
	 * @param presetUri
	 */
	public SelectUriWizard(boolean sourceSelection, String presetUri) {
		page = new RepositorySelectionPage(sourceSelection, presetUri);
		addPage(page);
		setWindowTitle(UIText.SelectUriWizard_Title);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
	}

	/**
	 * @return the URI
	 */
	public URIish getUri() {
		return uri;
	}

	@Override
	public boolean performFinish() {
		uri = page.getSelection().getURI();
		if (page.getStoreInSecureStore()) {
			if (!SecureStoreUtils.storeCredentials(page.getCredentials(),
					uri)) {
				return false;
			}
		}

		return uri != null;
	}

	/**
	 * @return credentials
	 */
	public UserPasswordCredentials getCredentials() {
		return page.getCredentials();
	}
}
