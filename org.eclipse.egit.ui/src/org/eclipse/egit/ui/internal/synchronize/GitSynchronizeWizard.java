/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * Synchronization wizard for Git repositories
 */
public class GitSynchronizeWizard extends Wizard {

	private IProject[] selectProjects;

	private GitSynchronizeWizardPage page;

	/**
	 * Instantiates a new wizard for synchronizing resources that are being
	 * managed by EGit.
	 */
	public GitSynchronizeWizard() {
		setWindowTitle(UIText.GitSynchronizeWizard_synchronize);
	}

	/**
	 * Set list of selected projects in wizard.
	 *
	 * @param projects
	 */
	public void selectProjects(IProject ... projects) {
		this.selectProjects = projects;
	}

	@Override
	public void addPages() {
		page = new GitSynchronizeWizardPage();
		page.selectProjects(selectProjects);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		GitSynchronizeDataSet gsdSet = new GitSynchronizeDataSet();

		Map<Repository, String> branches = page.getSelectedBranches();
		for (Entry<Repository, String> branchesEntry : branches.entrySet())
			try {
				gsdSet.add(new GitSynchronizeData(branchesEntry.getKey(),
						Constants.HEAD, branchesEntry.getValue(), false));
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
			}

		Set<IProject> selectedProjects
				 = page.getSelectedProjects();
		GitModelSynchronize.launch(gsdSet, selectedProjects
				.toArray(new IResource[selectedProjects
				.size()]));

		return true;
	}

}
