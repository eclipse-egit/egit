/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Jing Xue <jingxue@digizenstudio.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2011, Benjamin Muskalla <benjamin.muskalla@tasktop.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.commit.CommitUI;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Scan for modified resources in the same project as the selected resources.
 */
public class CommitActionHandler extends RepositoryActionHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Repository[] repos = getRepositoriesFor(getProjectsForSelectedResources(event));
		final Shell shell = getShell(event);
		IResource[] resourcesInScope;
		try {
			IResource[] selectedResources = getSelectedResources(event);
			IWorkbenchPart part = getPart(event);
			resourcesInScope = GitScopeUtil.getRelatedChanges(part,
					selectedResources);
		} catch (InterruptedException e) {
			// ignore, we will not show the commit dialog in case the user
			// cancels the scope operation
			return null;
		}
		CommitUI commitUi = new CommitUI(shell, repos[0], resourcesInScope,
				false);
		commitUi.commit();
		return null;
	}

	@Override
	public boolean isEnabled() {
		IProject[] projects = getProjectsForSelectedResources();
		return getRepositoriesFor(projects).length == 1;
	}

}
