/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
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
package org.eclipse.egit.ui.internal.repository;

import java.util.Collection;
import java.util.Set;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.wizard.Wizard;

/**
 * Repository search wizard
 */
public class RepositorySearchWizard extends Wizard {

	private final Collection<String> dirs;

	private RepositorySearchDialog searchPage;

	private boolean allowBare;

	/**
	 * Create repository search wizard
	 *
	 * @param existingDirs
	 * @param allowBare
	 */
	public RepositorySearchWizard(Collection<String> existingDirs,
			boolean allowBare) {
		dirs = existingDirs;
		this.allowBare = allowBare;
		setWindowTitle(UIText.RepositorySearchDialog_AddGitRepositories);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		searchPage = new RepositorySearchDialog(dirs, true, allowBare);
		addPage(searchPage);
	}

	/**
	 * Get selected directories
	 *
	 * @return directories
	 */
	public Set<String> getDirectories() {
		return searchPage.getDirectories();
	}

	@Override
	public boolean performFinish() {
		return true;
	}
}
