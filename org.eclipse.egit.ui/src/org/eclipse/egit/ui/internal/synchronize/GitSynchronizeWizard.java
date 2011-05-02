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
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.wizard.Wizard;

/**
 * Synchronization wizard for Git repositories
 */
public class GitSynchronizeWizard extends Wizard {

	private GitSynchronizeWizardPage page2;
	private GitPreconfiguredSynchronizeWizardPage page1;

	/**
	 * Instantiates a new wizard for synchronizing resources that are being
	 * managed by EGit.
	 */
	public GitSynchronizeWizard() {
		setWindowTitle(UIText.GitSynchronizeWizard_synchronize);
	}

	@Override
	public void addPages() {
		page1 = new GitPreconfiguredSynchronizeWizardPage();
		page2 = new GitSynchronizeWizardPage();
		addPage(page1);
		addPage(page2);
	}

	@Override
	public boolean canFinish() {
		if (page1.requiresCustomeConfiguration())
			return page2.isPageComplete();

		return page1.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		try {
			GitSynchronizeDataSet syncData = page1.getSyncData();
			List<IProject> projects = page1.getProjects();
			syncData.addAll(page2.getSyncData());
			projects.addAll(page2.getProjects());
			GitModelSynchronize.launch(syncData,
					projects.toArray(new IProject[projects.size()]));
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return true;
	}

}
