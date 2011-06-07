/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Jing Xue <jingxue@digizenstudio.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
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
import org.eclipse.jgit.lib.Repository;

/**
 * Scan for modified resources in the same project as the selected resources.
 */
public class CommitActionHandler extends RepositoryActionHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		Repository[] repos = getRepositoriesFor(getProjectsForSelectedResources(event));
		IResource[] selectedResources = getSelectedResources(event);
		new CommitUI(getShell(event), repos[0], selectedResources, false).commit();
		return null;
	}

	@Override
	public boolean isEnabled() {
		IProject[] projects = getProjectsForSelectedResources();
		return getRepositoriesFor(projects).length == 1 && !selectionContainsLinkedResources();
	}

}
