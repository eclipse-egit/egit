/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;

/**
 * A wizard used to import existing projects from a {@link Repository}
 */
public class GitImportProjectsWizard extends Wizard {

	private final String myWorkingDir;

	private final File myGitDir;

	/**
	 * @param repository
	 *            the repository
	 * @param path
	 *            a path, either the working directory of the repository or a
	 *            sub-directory thereof
	 */
	public GitImportProjectsWizard(Repository repository, String path) {
		super();
		myWorkingDir = path;
		myGitDir = repository.getDirectory();
		setWindowTitle(UIText.GitImportProjectsWizard_ImportExistingProjects0);
	}

	@Override
	public void addPages() {

		GitProjectsImportPage page = new GitProjectsImportPage() {

			@Override
			public void setVisible(boolean visible) {
				setGitDir(myGitDir);
				setProjectsList(myWorkingDir);
				super.setVisible(visible);
			}

		};
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		GitProjectsImportPage page = (GitProjectsImportPage) getPages()[0];
		return page.createProjects();

	}

}
