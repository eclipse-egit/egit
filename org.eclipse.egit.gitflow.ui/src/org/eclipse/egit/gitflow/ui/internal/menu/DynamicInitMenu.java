/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.menu;

import java.io.IOException;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.UIIcons;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.gitflow.ui.internal.actions.InitHandlerWrapper;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Start GitFlow release from a specified commit
 */
public class DynamicInitMenu extends ContributionItem {
	private static Image ICON = UIIcons.GITFLOW.createImage();

	@Override
	public void fill(Menu menu, int index) {
		GitFlowRepository gfRepo = getRepository();
		try {
			if (gfRepo == null || gfRepo.getConfig().isInitialized()) {
				return;
			}
		} catch (IOException e) {
			Activator.getDefault().getLog().log(Activator.error(e.getMessage()));
			return;
		}

		MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index);
		menuItem.setText(UIText.GitFlowInit_name);
		menuItem.addSelectionListener(new InitHandlerWrapper(gfRepo, getActiveShell()));
		menuItem.setImage(ICON);
	}

	private Shell getActiveShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	@Nullable
	private GitFlowRepository getRepository() {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();

		ISelection selection = window.getSelectionService().getSelection();
		IStructuredSelection structuredSelection = SelectionUtils
				.getStructuredSelection(selection);
		Repository repository = SelectionUtils
				.getRepository(structuredSelection);

		if (repository == null) {
			return null;
		}
		return new GitFlowRepository(repository);
	}
}
