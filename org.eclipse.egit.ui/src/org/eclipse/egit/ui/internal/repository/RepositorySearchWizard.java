/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

	/**
	 * Create repository search wizard
	 *
	 * @param existingDirs
	 */
	public RepositorySearchWizard(Collection<String> existingDirs) {
		dirs = existingDirs;
		setWindowTitle(UIText.RepositorySearchDialog_AddGitRepositories);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		searchPage = new RepositorySearchDialog(dirs, true);
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

	public boolean performFinish() {
		return true;
	}
}
