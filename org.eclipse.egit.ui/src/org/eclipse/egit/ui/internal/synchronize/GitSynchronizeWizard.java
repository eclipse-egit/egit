/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;

/**
 * Synchronization wizard for Git repositories
 */
public class GitSynchronizeWizard extends Wizard {

	private GitSynchronizeWizardPage page;

	/**
	 * Instantiates a new wizard for synchronizing resources that are being
	 * managed by EGit.
	 */
	public GitSynchronizeWizard() {
		setWindowTitle(UIText.GitSynchronizeWizard_synchronize);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_SYNCHRONIZE);
	}

	@Override
	public void addPages() {
		page = new GitSynchronizeWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		GitSynchronizeDataSet gsdSet = new GitSynchronizeDataSet(page.forceFetch());

		Map<Repository, String> branches = page.getSelectedBranches();
		boolean shouldIncludeLocal = page.shouldIncludeLocal();
		for (Entry<Repository, String> branchesEntry : branches.entrySet())
			try {
				Repository repo = branchesEntry.getKey();
				GitSynchronizeData data = new GitSynchronizeData(
						repo, HEAD, branchesEntry.getValue(),
						shouldIncludeLocal);
				Set<IResource> resources = getSelectedResources(repo);
				if (resources != null && resources.size() > 0)
					data.setIncludedResources(resources);
				gsdSet.add(data);
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

	private Set<IResource> getSelectedResources(Repository repo) {
		ISelectionService selectionService = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getSelectionService();
		ISelection selection = selectionService.getSelection();
		if (selection instanceof IStructuredSelection) {
			Set<IResource> result = new HashSet<>();
			IStructuredSelection sel = (IStructuredSelection) selection;
			if (sel.size() == 0)
				return null;

			File workTree = repo.getWorkTree();
			for (Object o : sel.toArray()) {
				if (o == null) {
					continue;
				}

				IResource res = AdapterUtils.adaptToAnyResource(o);
				if (res == null) {
					continue;
				}

				int type = res.getType();
				if (type == IResource.FOLDER) {
					RepositoryMapping mapping = RepositoryMapping.getMapping(res);
					if (mapping == null) {
						continue;
					}
					Repository selRepo = mapping.getRepository();
					if (workTree.equals(selRepo.getWorkTree()))
						result.add(res);
				}
			}

			return result;
		}
		return null;
	}

}
