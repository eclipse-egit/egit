/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.jgit.revwalk.RevObject;

/**
 * Changes the reference for the quickdiff
 */
public class SetQuickdiffBaselineAction extends AbstractRevObjectAction {

	@Override
	protected IWorkspaceRunnable createOperation(List selection) {
		return new QuickdiffBaselineOperation(getActiveRepository(), ((RevObject)selection.get(0)).getId().name());
	}

}
