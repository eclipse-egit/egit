/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.menu;

import static org.eclipse.egit.gitflow.ui.Activator.error;

import java.io.IOException;

import org.eclipse.egit.core.internal.IRepositoryCommit;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.gitflow.Activator;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.gitflow.ui.internal.actions.ReleaseStartFromCommitHandler;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * Start GitFlow release from a specified commit
 */
public class DynamicHistoryMenu extends ContributionItem {
	@Override
	public void fill(Menu menu, int index) {

		GitFlowRepository gfRepo = getRepository();
		if (gfRepo == null) {
			return;
		}

		RevCommit selectedCommit = getSelectedCommit();
		if (selectedCommit == null) {
			return;
		}
		String startCommitSha1 = selectedCommit.getName();
		Shell activeShell = getActiveShell();

		ReleaseStartFromCommitHandler listener = new ReleaseStartFromCommitHandler(
				gfRepo, startCommitSha1, activeShell);
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index);
		menuItem.setText(NLS.bind(
				UIText.DynamicHistoryMenu_startGitflowReleaseFrom,
				abbreviate(selectedCommit)));
		menuItem.addSelectionListener(listener);

		boolean isEnabled = false;
		try {
			isEnabled = gfRepo.isOnDevelop(selectedCommit);
		} catch (IOException e) {
			Activator.getDefault().getLog().log(error(e.getMessage(), e));
		}
		menuItem.setEnabled(isEnabled);
	}

	private String abbreviate(RevCommit selectedCommit) {
		return Utils.getShortObjectId(selectedCommit.getId());
	}

	private RevCommit getSelectedCommit() {
		ISelection selection = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart()
				.getSite().getSelectionProvider().getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}
		IStructuredSelection structSelection = (IStructuredSelection) selection;
		Object firstElement = structSelection.getFirstElement();
		if (!(firstElement instanceof RevCommit)) {
			return null;
		}
		return (RevCommit) firstElement;
	}

	private Shell getActiveShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	private GitFlowRepository getRepository() {
		IWorkbenchPart activePart = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart();
		ISelectionProvider selectionProvider = activePart.getSite()
				.getSelectionProvider();
		ISelection selection = selectionProvider.getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}
		Object element = ((IStructuredSelection) selection).getFirstElement();
		if (!(element instanceof IRepositoryCommit)) {
			return null;
		}
		Repository repository = ((IRepositoryCommit) element).getRepository();

		if (repository == null) {
			return null;
		}
		return new GitFlowRepository(repository);
	}
}
