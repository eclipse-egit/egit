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

import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.UIText;
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
				Set<IContainer> containers = getSelectedContainers(repo);
				if (containers != null && containers.size() > 0)
					data.setIncludedPaths(containers);
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

	private Set<IContainer> getSelectedContainers(Repository repo) {
		ISelectionService selectionService = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getSelectionService();
		ISelection selection = selectionService.getSelection();
		if (selection instanceof IStructuredSelection) {
			Set<IContainer> result = new HashSet<IContainer>();
			IStructuredSelection sel = (IStructuredSelection) selection;
			if (sel.size() == 0)
				return null;

			File workTree = repo.getWorkTree();
			for (Object o : sel.toArray()) {
				if (!(o instanceof IAdaptable))
					continue;

				IResource res = (IResource) ((IAdaptable) o)
						.getAdapter(IResource.class);
				if (res == null)
					continue;

				int type = res.getType();
				if (type == IResource.FOLDER) {
					Repository selRepo = RepositoryMapping.getMapping(res)
							.getRepository();
					if (workTree.equals(selRepo.getWorkTree()))
						result.add((IContainer) res);
				}
			}

			return result;
		}
		return null;
	}

}
