/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.egit.ui.internal.branch.BranchOperationUI;

/**
 * Action for selecting a branch and checking it out.
 *
 * @see BranchOperationUI
 */
public class BranchAction extends RepositoryAction {
	/**
	 * Constructs this action
	 */
	public BranchAction() {
		super(ActionCommands.BRANCH_ACTION, new BranchActionHandler());
	}
}
